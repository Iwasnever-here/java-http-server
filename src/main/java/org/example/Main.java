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
        SessionManager sessionManager = new SessionManager();

        server.use(new ErrorHandlingMiddleware());

        server.use(new SessionMiddleware(sessionManager));

        server.use(new LoggingMiddleware());

        server.use(new TimingMiddleware());

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


        server.get(
                "/boom",
                (request, response) -> {
                    throw new RuntimeException(
                            "Something broke"
                    );
                }
        );

        server.post(
                "/login",
                (request, response) -> {
                    Session session =
                            request.getSession();

                    session.set(
                            "username",
                            "Alice"
                    );

                    response.text(
                            "Logged in"
                    );
                }
        );
        server.get(
                "/profile",
                (request, response) -> {
                    Session session =
                            request.getSession();

                    String username =
                            session.getString(
                                    "username"
                            );

                    if (username == null) {
                        response
                                .status(
                                        HttpStatus.UNAUTHORIZED
                                )
                                .text(
                                        "Not logged in"
                                );

                        return;
                    }

                    response.text(
                            "Hello " + username
                    );
                }
        );
        server.post(
                "/logout",
                (request, response) -> {
                    String sessionId =
                            request.getCookie(
                                    "sessionId"
                            );

                    sessionManager.delete(
                            sessionId
                    );

                    response.cookie(
                            "sessionId",
                            "",
                            "/",
                            0L,
                            true
                    );

                    response.text(
                            "Logged out"
                    );
                }
        );

        server.start();
    }
}