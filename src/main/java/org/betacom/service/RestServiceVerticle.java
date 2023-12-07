package org.betacom.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.betacom.databaseConfig.MongoDBConfig;
import org.betacom.model.Item;
import org.betacom.model.User;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RestServiceVerticle extends AbstractVerticle {

    private JWTAuth provider;
    private MongoClient mongoClient;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        Router router = Router.router(vertx);

        //Handler for JSON in request
        router.route().handler(BodyHandler.create());

        //JWTAutentication
        provider = JWTAuth.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("keyboard cat")));


        //CreateMongoClient
        CompletableFuture<MongoClient> mongoClientFuture = MongoDBConfig.createMongoClient(vertx);
        mongoClientFuture.thenAccept(client -> {
            mongoClient = client;

            // UnAuthorized Endpoints
            router.post("/login").handler(this::handleLogin);
            router.post("/register").handler(this::handleRegister);

            // Authorized Endpoints
            router.get("/items").handler(this::handleGetItems);
            router.post("/items").handler(this::handleCreateItem);

            // HTTPServer start
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(3000, http -> {
                        if (http.succeeded()) {
                            promise.complete();
                            System.out.println("HTTP server started on port 3000");
                        } else {
                            promise.fail(http.cause());
                        }
                    });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            promise.fail(throwable);
            return null;
        });

    }

    private void handleLogin(RoutingContext routingContext) {
        JsonObject requestJson = routingContext.getBodyAsJson();
        String login = requestJson.getString("login");
        String password = requestJson.getString("password");

        JsonObject query = new JsonObject().put("login", login);

        mongoClient.findOne("users", query, null, userResult -> {
            if (userResult.succeeded()) {
                JsonObject user = userResult.result();
                if (user != null && BCrypt.checkpw(password, user.getString("password"))) {
                    UUID id = UUID.fromString(user.getString("id"));

                    //GENERATE TOKEN WITH 20 min expiration
                    String token = provider.generateToken(new JsonObject().put("login", login).put("id", id.toString()), new JWTOptions().setExpiresInMinutes(20));
                    routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("token", token).encode());
                } else {
                    routingContext.response()
                            .setStatusCode(401)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("error", "Invalid login or password").encode());
                }
            } else {
                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("error", "Internal Server Error").encode());
            }
        });

    }

    private void handleRegister(RoutingContext routingContext) {
        JsonObject requestJson = routingContext.getBodyAsJson();

        String login = requestJson.getString("login");
        String password = requestJson.getString("password");

        //check if user exists query
        JsonObject query = new JsonObject().put("login", login);

        //check if user exists, if not, create new user
        mongoClient.findOne("users", query, null, res -> {
            if(res.succeeded()){
                JsonObject existingUser = res.result();
                if(existingUser == null){
                    User newUser = new User(login, password);
                    mongoClient.save("users", JsonObject.mapFrom(newUser), saveRes -> {
                        if (saveRes.succeeded()) {
                            routingContext.response()
                                    .setStatusCode(204)
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject().put("description:", "Registering successfull.").encode());
                        } else {
                            routingContext.response()
                                    .setStatusCode(500)
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject().put("error", "Invalid registration").encode());
                        }
                    });
                }
                else{
                    routingContext.response()
                            .setStatusCode(400)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("error", "User already exists").encode());
                }
            }
            else{
                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("error", "Internal Server Error").encode());
            }

        });

    }
    private void handleCreateItem(RoutingContext routingContext){
        String authorizationHeader = routingContext.request().getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            String token = authorizationHeader.substring(7);
            provider.authenticate(new JsonObject().put("token", token), authResult -> {
                if(authResult.succeeded()) {
                    //authorization success
                    JsonObject principal= authResult.result().principal();
                    String userId = principal.getString("id");
                    JsonObject requestJson = routingContext.getBodyAsJson();
                    Item newItem = new Item(UUID.fromString(userId), requestJson.getString("name"));
                    mongoClient.save("items", JsonObject.mapFrom(newItem), saveRes -> {
                        if (saveRes.succeeded()) {
                            routingContext.response()
                                    .setStatusCode(204)
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject().put("description:", "Item created successfull.").encode());
                        } else {
                            routingContext.response()
                                    .setStatusCode(500)
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject().put("error", "Invalid item Added").encode());
                        }
                    });
                }
                else {
                    routingContext.response()
                            .setStatusCode(401)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("description", "You have not provided an authentication token, the one provided has expired, was revoked or is not authentic").encode());

                }
            });
        }
        else{
            routingContext.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Internal Server Error").encode());
        }
    }

    private void handleGetItems(RoutingContext routingContext) {
        String authorizationHeader = routingContext.request().getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            provider.authenticate(new JsonObject().put("token", token), authResult -> {
                if(authResult.succeeded()) {
                    //authorization success
                    JsonObject principal= authResult.result().principal();
                    String userId = principal.getString("id");
                    mongoClient.find("items", new JsonObject().put("owner", userId), itemsResult -> {
                        if (itemsResult.succeeded()) {
                            List<JsonObject> itemsList = itemsResult.result();
                            JsonArray items = new JsonArray();
                            for (JsonObject item : itemsList) {
                                JsonObject newItem = new JsonObject()
                                        .put("id", item.getString("id"))
                                        .put("title", item.getString("name"));
                                items.add(newItem);
                            }
                            routingContext.response()
                                    .setStatusCode(200)
                                    .putHeader("content-type", "application/json")
                                    .end(items.encode());
                        } else {
                            routingContext.response()
                                    .setStatusCode(500)
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject().put("error", "Internal Server Error").encode());
                        }
                    });

                }
                else {
                    routingContext.response()
                            .setStatusCode(401)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("description", "You have not provided an authentication token, the one provided has expired, was revoked or is not authentic").encode());

                }
            });
        }
        else{
            routingContext.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Internal Server Error").encode());
        }

    }
}





