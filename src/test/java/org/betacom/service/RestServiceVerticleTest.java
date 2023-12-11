package org.betacom.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.betacom.databaseConfig.MongoDBConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class RestServiceVerticleTest {

    private static MongoClient mongoClient;
    private static WebClient webClient;
    private static String login = "test";
    private static String password = "test";
    private static String token;

    @BeforeAll
    static void setUp(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new RestServiceVerticle(), result -> {
            if (result.succeeded()) {
                MongoDBConfig.createMongoClient(vertx)
                        .thenAccept(client -> {
                            mongoClient = client;
                            webClient = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(3000));
                            testContext.completeNow();
                        });

            } else {
                testContext.failNow(result.cause());
            }
        });
    }

    @AfterAll
    static void tearDown(Vertx vertx, VertxTestContext testContext) {
        deleteTestUser(testContext);
        vertx.close();
        mongoClient.close();
        testContext.completeNow();
    }

    @Test
    void registerUserTest(Vertx vertx, VertxTestContext testContext) {
        JsonObject user = new JsonObject()
                .put("login", login)
                .put("password", password);

        webClient.post("/register")
                .sendJsonObject(user, testContext.succeeding(response -> {
                    assertEquals(204, response.statusCode());
                    //assertTrue(response.bodyAsString().contains("Registering successfull."));
                    testContext.completeNow();
                }));

    }

    @Test
    void registerSameUserTest(Vertx vertx, VertxTestContext testContext) {
        JsonObject user = new JsonObject()
                .put("login", login)
                .put("password", password);

        webClient.post("/register")
                .sendJsonObject(user, testContext.succeeding(response -> {
                    assertEquals(400, response.statusCode());
                    //assertTrue(response.bodyAsString().contains("Registering successfull."));
                    testContext.completeNow();
                }));

    }

    @Test
    void unsuccessfulLoginTest(Vertx vertx, VertxTestContext testContext) {
        JsonObject invalidUser = new JsonObject()
                .put("login", login + login)
                .put("password", password);

        webClient.post("/login")
                .sendJsonObject(invalidUser, testContext.succeeding(response -> {
                    assertEquals(401, response.statusCode());
                    assertNotNull(response.bodyAsJsonObject().getString("error"));
                    testContext.completeNow();
                }));
    }

    @Test
    void successfulLoginTest(Vertx vertx, VertxTestContext testContext) {
        JsonObject user = new JsonObject()
                .put("login", login)
                .put("password", password);

        webClient.post("/login")
                .sendJsonObject(user, testContext.succeeding(response -> {
                    assertEquals(200, response.statusCode());
                    //assertNotNull(response.bodyAsJsonObject().getString("token"));
                    token = response.bodyAsJsonObject().getString("token");
                    testContext.completeNow();
                }));
    }

    @Test
    void getItemsWithValidTokenTest(Vertx vertx, VertxTestContext testContext) {
        webClient.get("/items")
                .putHeader("Authorization", "Bearer " + token)
                .send(testContext.succeeding(response -> {
                    assertEquals(200, response.statusCode());
                    testContext.completeNow();
                }));
    }

    @Test
    void getItemsWithInvalidTokenTest(Vertx vertx, VertxTestContext testContext) {
        webClient.get("/items")
                .putHeader("Authorization", "Bearer " + token + token)
                .send(testContext.succeeding(response -> {
                    assertEquals(401, response.statusCode());
                    testContext.completeNow();
                }));
    }

    @Test
    void addItemWithInvalidTokenTest(Vertx vertx, VertxTestContext testContext) {
        JsonObject item = new JsonObject()
                .put("name", "testName");

        webClient.post("/items")
                .putHeader("Authorization", "Bearer " + token + token)
                .sendJsonObject(item, testContext.succeeding(response -> {
                    assertEquals(401, response.statusCode());
                    testContext.completeNow();
                }));
    }

    private static void deleteTestUser(VertxTestContext testContext) {
        JsonObject query = new JsonObject().put("login", login);
        mongoClient.removeDocument("users", query, deleteResult -> {
            if (deleteResult.succeeded()) {
                testContext.completeNow();
            } else {
                deleteResult.cause().printStackTrace();
                testContext.failNow(deleteResult.cause());
            }
        });
    }

}

