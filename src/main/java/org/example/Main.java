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
                "/users/new",
                (request, response) ->
                        response.text("Create new user")
        );

        server.get(
                "/users/:id",
                (request, response) -> {
                    String id = request.getPathParameter("id");
                    response.text("User " + id);
                }
        );

        server.get(
                "/slow",
                (request, response) -> {
                    Thread.sleep(2000);
                    response.text("Finished");
                }
        );
        server.get(
                "/quick",
                (request, response) ->
                        response.text("Quick")
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