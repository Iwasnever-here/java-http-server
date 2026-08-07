package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServerLifecycleTest {

    private HttpServer server;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.stop();
        }

        if (serverThread != null) {
            serverThread.join(3000);
        }
    }

    @Test
    void stopBeforeStartDoesNotThrow() {
        server = createServer(2);

        assertDoesNotThrow(server::stop);
        assertFalse(server.isRunning());
    }

    @Test
    void serverStartsAndStopsCleanly() throws Exception {
        server = createServer(2);

        startServer();

        assertTrue(server.isRunning());
        assertTrue(server.getPort() > 0);

        server.stop();

        serverThread.join(3000);

        assertFalse(server.isRunning());
        assertFalse(serverThread.isAlive());
    }

    @Test
    void rejectsRouteRegistrationAfterStart()
            throws Exception {
        server = createServer(2);

        startServer();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> server.get(
                        "/late",
                        (request, response) ->
                                response.text("Too late")
                )
        );

        assertEquals(
                "Routes cannot be modified while server is running",
                exception.getMessage()
        );
    }

    @Test
    void countsHandledRequests() throws Exception {
        server = createServer(4);

        server.get(
                "/test",
                (request, response) ->
                        response.text("OK")
        );

        startServer();

        sendRequest("/test");
        sendRequest("/test");
        sendRequest("/test");

        waitForRequestCount(3);

        assertEquals(
                3,
                server.getRequestCount()
        );
    }

    @Test
    void countsConcurrentRequestsSafely()
            throws Exception {
        int requestTotal = 20;

        server = createServer(4);

        server.get(
                "/test",
                (request, response) ->
                        response.text("OK")
        );

        startServer();

        ExecutorService clients =
                Executors.newFixedThreadPool(10);

        try {
            List<Future<String>> futures =
                    new ArrayList<>();

            for (int i = 0; i < requestTotal; i++) {
                futures.add(
                        clients.submit(
                                () -> sendRequest("/test")
                        )
                );
            }

            for (Future<String> future : futures) {
                String response =
                        future.get(
                                3,
                                TimeUnit.SECONDS
                        );

                assertTrue(
                        response.contains("200 OK")
                );
            }

        } finally {
            clients.shutdownNow();
        }

        waitForRequestCount(requestTotal);

        assertEquals(
                requestTotal,
                server.getRequestCount()
        );
    }

    @Test
    void runningRequestCanFinishDuringShutdown()
            throws Exception {
        CountDownLatch routeStarted =
                new CountDownLatch(1);

        CountDownLatch allowRouteToFinish =
                new CountDownLatch(1);

        server = createServer(2);

        server.get(
                "/slow",
                (request, response) -> {
                    routeStarted.countDown();

                    allowRouteToFinish.await(
                            3,
                            TimeUnit.SECONDS
                    );

                    response.text("Finished");
                }
        );

        startServer();

        ExecutorService clientExecutor =
                Executors.newSingleThreadExecutor();

        try {
            Future<String> slowResponse =
                    clientExecutor.submit(
                            () -> sendRequest("/slow")
                    );

            assertTrue(
                    routeStarted.await(
                            1,
                            TimeUnit.SECONDS
                    )
            );

            Thread shutdownThread =
                    new Thread(server::stop);

            shutdownThread.start();

            allowRouteToFinish.countDown();

            String response =
                    slowResponse.get(
                            3,
                            TimeUnit.SECONDS
                    );

            assertTrue(
                    response.contains("Finished")
            );

            shutdownThread.join(3000);

            assertFalse(server.isRunning());

        } finally {
            clientExecutor.shutdownNow();
        }
    }

    private HttpServer createServer(
            int threadPoolSize
    ) {
        ServerConfig config = new ServerConfig(
                0,
                threadPoolSize,
                Path.of("public")
        );

        return new HttpServer(config);
    }

    private void startServer()
            throws InterruptedException {
        serverThread =
                new Thread(server::start);

        serverThread.start();

        for (int i = 0; i < 100; i++) {
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
            String path
    ) throws Exception {
        try (
                Socket socket = new Socket(
                        "localhost",
                        server.getPort()
                )
        ) {
            socket.getOutputStream().write(
                    (
                            "GET " +
                                    path +
                                    " HTTP/1.1\r\n" +
                                    "Host: localhost\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes(StandardCharsets.UTF_8)
            );

            socket.getOutputStream().flush();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                response
                        .append(line)
                        .append("\n");
            }

            return response.toString();
        }
    }

    private void waitForRequestCount(
            long expected
    ) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (
                    server.getRequestCount() >= expected
            ) {
                return;
            }

            Thread.sleep(10);
        }

        throw new AssertionError(
                "Expected request count " +
                        expected +
                        " but got " +
                        server.getRequestCount()
        );
    }
}