package org.betacom.databaseConfig;

import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;

@ExtendWith(VertxExtension.class)
public class MongoDBConfigTest {

    //Test generaly MongoDBClient creation
    @Test
    public void testCreateMongoClient(Vertx vertx, VertxTestContext testContext){
        CompletableFuture<MongoClient> mongoClientFuture = MongoDBConfig.createMongoClient(vertx);
        mongoClientFuture.thenAccept(mongoClient -> {
            if(mongoClient != null) {
                testContext.completeNow();
            } else {
                testContext.failNow(new Throwable("MongoClient is null"));
            }
        }).exceptionally(throwable -> {
            testContext.failNow(throwable);
            return null;
        });
    }

    //Test MongoDBClient creation with address
    @Test
    public void testCreateMongoClientAddress(Vertx vertx, VertxTestContext testContext){
        CompletableFuture<MongoClient> mongoClientFuture = MongoDBConfig.createMongoClient(vertx);
        mongoClientFuture.thenAccept(mongoClient -> {
            if(mongoClient != null) {
                mongoClient.getCollections().toCompletionStage().whenComplete((databases, throwable) -> {
                    if (throwable == null) {
                        testContext.completeNow();
                    }else{
                        testContext.failNow(throwable);
                    }
                });
            } else {
                testContext.failNow(new Throwable("MongoClient is null"));
            }
        }).exceptionally(throwable -> {
            testContext.failNow(throwable);
            return null;
        });
    }

    //Test database and collections
    @Test
    public void testDatabaseNameAndCollections(Vertx vertx, VertxTestContext testContext) {
        CompletableFuture<MongoClient> mongoClientFuture = MongoDBConfig.createMongoClient(vertx);
        mongoClientFuture.thenAccept(mongoClient -> {
            if(mongoClient != null) {
                testContext.verify(() -> {
                    mongoClient.getCollections().toCompletionStage().whenComplete((collections, throwable) -> {
                        if (throwable == null) {
                            System.out.println("Collections retrieved successfully");
                            if (collections.contains("items") && collections.contains("users")) {
                                System.out.println("Both 'items' and 'users' collections are present");
                                testContext.completeNow();
                            } else {
                                System.out.println("Missing one or both collections: 'items' and/or 'users'");
                                testContext.failNow(new Throwable("Missing one or both collections: 'items' and/or 'users'"));
                            }
                        } else {
                            throwable.printStackTrace();
                            testContext.failNow(new Throwable("Failed to connect to the database or retrieve collections"));
                        }
                    });
                });
            } else {
                testContext.failNow(new Throwable("MongoClient is null"));
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            testContext.failNow(throwable);
            return null;
        });
    }
}
