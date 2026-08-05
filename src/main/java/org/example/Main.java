package org.example;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        ServerConfig config = new ServerConfig(
                8080,
                4,
                Path.of("public")
        );

        HttpServer server = new HttpServer(config);

        server.get(
                "/",
                (request, response) ->
                        response.text("Home page")
        );

        server.get(
                "/users",
                (request, response) ->
                        response.json(
                                "[{\"id\":1,\"name\":\"Alice\"}]"
                        )
        );

        server.post(
                "/users",
                (request, response) ->
                        response
                                .status(HttpStatus.CREATED)
                                .json(
                                        "{\"message\":\"User created\"}"
                                )
        );

        server.start();
    }
}