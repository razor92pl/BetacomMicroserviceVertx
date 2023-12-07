package org.betacom.databaseConfig;

import io.vertx.config.ConfigRetriever;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.util.concurrent.CompletableFuture;
public class MongoDBConfig {
    public static CompletableFuture<MongoClient> createMongoClient(Vertx vertx) {
        CompletableFuture<MongoClient> future = new CompletableFuture<>();

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "src/main/resources/application-config.properties"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore);

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);

        configRetriever.getConfig(ar -> {
            if (ar.succeeded()) {
                JsonObject config = ar.result();
                MongoClient mongoClient = MongoClient.createShared(vertx, config);
                future.complete(mongoClient);
            } else {
                ar.cause().printStackTrace();
                future.completeExceptionally(ar.cause());
            }
        });
        return future;
    }
}