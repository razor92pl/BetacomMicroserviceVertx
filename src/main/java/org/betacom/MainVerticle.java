package org.betacom;

import io.vertx.core.Vertx;
import org.betacom.service.RestServiceVerticle;

public class MainVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new RestServiceVerticle(), result -> {
            if (result.succeeded()) {
                System.out.println("Application deployed successfully");
            } else {
                System.err.println("Application deployment failed");
                result.cause().printStackTrace();
            }
        });
    }
}
