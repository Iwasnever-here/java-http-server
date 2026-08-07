package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServerConcurrencyTest {

    private HttpServer server;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.stop();
        }

        if (serverThread != null) {
            serverThread.join(2000);
        }
    }

    @Test
    void handlesTwoRequestsConcurrently() throws Exception {
        ServerConfig config = new ServerConfig(
                0,
                2,
                Path.of("public")
        );

        server = new HttpServer(config);

        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch allowSlowToFinish = new CountDownLatch(1);

        server.get(
                "/slow",
                (request, response) -> {
                    slowStarted.countDown();

                    allowSlowToFinish.await(
                            2,
                            TimeUnit.SECONDS
                    );

                    response.text("Slow finished");
                }
        );

        server.get(
                "/quick",
                (request, response) ->
                        response.text("Quick")
        );

        serverThread = new Thread(server::start);
        serverThread.start();

        waitForServerToStart();

        Thread slowClient = new Thread(() ->
                sendRequest("/slow")
        );

        slowClient.start();

        assertTrue(
                slowStarted.await(
                        1,
                        TimeUnit.SECONDS
                )
        );

        long start = System.nanoTime();

        String quickResponse = sendRequest("/quick");

        long elapsedMillis =
                TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - start
                );

        allowSlowToFinish.countDown();
        slowClient.join();

        assertTrue(
                quickResponse.contains("Quick")
        );

        assertTrue(
                elapsedMillis < 1000,
                "Quick request was blocked by slow request"
        );
    }

    private String sendRequest(String path) {
        try (
                Socket socket = new Socket(
                        "localhost",
                        server.getPort()
                );

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        )
        ) {
            socket.getOutputStream().write(
                    (
                            "GET " + path + " HTTP/1.1\r\n" +
                                    "Host: localhost\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes()
            );

            socket.getOutputStream().flush();

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            return response.toString();

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void waitForServerToStart()
            throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (server.isRunning()) {
                return;
            }

            Thread.sleep(20);
        }

        throw new IllegalStateException(
                "Server failed to start"
        );
    }
}