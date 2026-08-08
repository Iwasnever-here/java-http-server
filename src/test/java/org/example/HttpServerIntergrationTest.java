package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServerIntegrationTest {

    @TempDir
    Path tempDirectory;

    private HttpServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(
                tempDirectory.resolve("index.html"),
                "<h1>Test Home</h1>",
                StandardCharsets.UTF_8
        );

        Files.writeString(
                tempDirectory.resolve("styles.css"),
                "body { margin: 0; }",
                StandardCharsets.UTF_8
        );

        ServerConfig config = new ServerConfig(
                0,
                4,
                tempDirectory
        );

        server = new HttpServer(config);

        registerRoutes();

        startServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }

        if (serverThread != null) {
            serverThread.join(3000);
        }
    }

    @Test
    void servesStaticIndexForRoot() throws Exception {
        String response = sendRequest(
                "GET / HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Content-Type: text/html"
                )
        );

        assertTrue(
                response.contains(
                        "<h1>Test Home</h1>"
                )
        );
    }

    @Test
    void servesStaticCssFile() throws Exception {
        String response = sendRequest(
                "GET /styles.css HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Content-Type: text/css"
                )
        );

        assertTrue(
                response.contains(
                        "body { margin: 0; }"
                )
        );
    }

    @Test
    void handlesExactGetRoute() throws Exception {
        String response = sendRequest(
                "GET /hello HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                response.contains("Hello")
        );
    }

    @Test
    void handlesDynamicRoute() throws Exception {
        String response = sendRequest(
                "GET /users/42 HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "User 42"
                )
        );
    }

    @Test
    void handlesPostRequestWithBody() throws Exception {
        String body = "Alice";

        String response = sendRequest(
                "POST /users HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: " +
                        body.getBytes(
                                StandardCharsets.UTF_8
                        ).length +
                        "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        body
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 201 Created\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Created Alice"
                )
        );
    }

    @Test
    void returns404ForMissingResource() throws Exception {
        String response = sendRequest(
                "GET /missing HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 404 Not Found\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Not Found"
                )
        );
    }

    @Test
    void returns405WhenPathExistsForDifferentMethod()
            throws Exception {
        String response = sendRequest(
                "POST /hello HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 405 Method Not Allowed\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Method Not Allowed"
                )
        );
    }

    @Test
    void returns400ForMalformedRequest() throws Exception {
        String response = sendRequest(
                "GET users HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 400 Bad Request\r\n"
                )
        );
    }

    @Test
    void remainsRunningAfterMalformedRequest()
            throws Exception {
        String badResponse = sendRequest(
                "GET users HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "\r\n"
        );

        assertTrue(
                badResponse.startsWith(
                        "HTTP/1.1 400 Bad Request\r\n"
                )
        );

        assertTrue(server.isRunning());

        String goodResponse = sendRequest(
                "GET /hello HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                goodResponse.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                goodResponse.contains("Hello")
        );
    }

    @Test
    void writesCustomResponseHeaders() throws Exception {
        String response = sendRequest(
                "GET /header HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "X-Test: integration\r\n"
                )
        );
    }

    @Test
    void correctlyWritesUnicodeResponseBody()
            throws Exception {
        String response = sendRequest(
                "GET /unicode HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Hello £ 世界"
                )
        );

        int expectedLength =
                "Hello £ 世界"
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
                        .length;

        assertTrue(
                response.contains(
                        "Content-Length: " +
                                expectedLength +
                                "\r\n"
                )
        );
    }

    @Test
    void middlewareAddsResponseTimeHeader()
            throws Exception {
        String response = sendRequest(
                "GET /hello HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.contains(
                        "X-Response-Time:"
                )
        );
    }

    @Test
    void routeExceptionReturns500()
            throws Exception {
        String response = sendRequest(
                "GET /boom HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                response.startsWith(
                        "HTTP/1.1 500 Internal Server Error\r\n"
                )
        );

        assertTrue(
                response.contains(
                        "Internal Server Error"
                )
        );

        assertTrue(server.isRunning());
    }

    @Test
    void sessionPersistsAcrossRequests()
            throws Exception {
        String loginResponse = sendRequest(
                "POST /login HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                loginResponse.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        String sessionCookie =
                extractSessionCookie(
                        loginResponse
                );

        assertFalse(sessionCookie.isBlank());

        String profileResponse = sendRequest(
                "GET /profile HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Cookie: " +
                        sessionCookie +
                        "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        );

        assertTrue(
                profileResponse.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                profileResponse.contains(
                        "Hello Alice"
                )
        );
    }

    private void registerRoutes() {
        SessionManager sessionManager =
                new SessionManager();

        server.use(
                new ErrorHandlingMiddleware()
        );

        server.use(
                new SessionMiddleware(
                        sessionManager
                )
        );

        server.use(
                new TimingMiddleware()
        );

        server.get(
                "/hello",
                (request, response) ->
                        response.text("Hello")
        );

        server.get(
                "/users/:id",
                (request, response) ->
                        response.text(
                                "User " +
                                        request.getPathParameter(
                                                "id"
                                        )
                        )
        );

        server.post(
                "/users",
                (request, response) ->
                        response
                                .status(
                                        HttpStatus.CREATED
                                )
                                .text(
                                        "Created " +
                                                request
                                                        .getBodyAsString()
                                )
        );

        server.get(
                "/header",
                (request, response) ->
                        response
                                .header(
                                        "X-Test",
                                        "integration"
                                )
                                .text("OK")
        );

        server.get(
                "/unicode",
                (request, response) ->
                        response.text(
                                "Hello £ 世界"
                        )
        );

        server.get(
                "/boom",
                (request, response) -> {
                    throw new RuntimeException(
                            "Boom"
                    );
                }
        );

        server.post(
                "/login",
                (request, response) -> {
                    request
                            .getSession()
                            .set(
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
                    String username =
                            request
                                    .getSession()
                                    .getString(
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
    }

    private void startServer()
            throws InterruptedException {
        serverThread =
                new Thread(server::start);

        serverThread.start();

        for (int attempt = 0;
             attempt < 100;
             attempt++) {

            if (server.isRunning()) {
                return;
            }

            Thread.sleep(10);
        }

        throw new IllegalStateException(
                "Server failed to start"
        );
    }

    private String sendRequest(
            String rawRequest
    ) throws Exception {
        try (
                Socket socket =
                        new Socket(
                                "localhost",
                                server.getPort()
                        )
        ) {
            socket
                    .getOutputStream()
                    .write(
                            rawRequest.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            socket
                    .getOutputStream()
                    .flush();

            ByteArrayOutputStream response =
                    new ByteArrayOutputStream();

            InputStream input =
                    socket.getInputStream();

            byte[] buffer =
                    new byte[1024];

            int bytesRead;

            while (
                    (bytesRead =
                            input.read(buffer)) != -1
            ) {
                response.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return response.toString(
                    StandardCharsets.UTF_8
            );
        }
    }

    private String extractSessionCookie(
            String response
    ) {
        String[] lines =
                response.split("\r\n");

        for (String line : lines) {
            if (
                    line.startsWith(
                            "Set-Cookie:"
                    )
            ) {
                String cookie =
                        line.substring(
                                "Set-Cookie:".length()
                        ).trim();

                int semicolon =
                        cookie.indexOf(';');

                if (semicolon >= 0) {
                    cookie =
                            cookie.substring(
                                    0,
                                    semicolon
                            );
                }

                if (
                        cookie.startsWith(
                                "sessionId="
                        )
                ) {
                    return cookie;
                }
            }
        }

        throw new AssertionError(
                "Session cookie not found"
        );
    }
}