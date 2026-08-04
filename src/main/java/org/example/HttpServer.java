package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class HttpServer {

    private final int port;
    private final RequestParser requestParser;

    private boolean running;
    private ServerSocket serverSocket;

    public HttpServer(ServerConfig config) {
        this.port = config.getPort();
        this.requestParser = new RequestParser();
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) {
            throw new IllegalStateException(
                    "Server is already running"
            );
        }

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println(
                    "Server started on http://localhost:" + port
            );

            while (running) {
                Socket clientSocket = serverSocket.accept();
                handleConnection(clientSocket);
            }

        } catch (SocketException exception) {
            if (running) {
                System.err.println(
                        "Server socket error: " +
                                exception.getMessage()
                );
            }

        } catch (IOException exception) {
            System.err.println(
                    "Server I/O error on port " +
                            port +
                            ": " +
                            exception.getMessage()
            );

        } finally {
            stop();
        }
    }

    private void handleConnection(Socket clientSocket) {
        String clientAddress = clientSocket
                .getInetAddress()
                .getHostAddress();

        System.out.println(
                "Accepted connection from: " + clientAddress
        );

        try (clientSocket) {
            HttpRequest request = requestParser.parse(
                    clientSocket.getInputStream()
            );

            System.out.println(
                    "Method: " + request.getMethod()
            );
            System.out.println(
                    "Path: " + request.getPath()
            );
            System.out.println(
                    "Query: " + request.getQueryParameters()
            );
            System.out.println(
                    "Version: " + request.getHttpVersion()
            );

            sendHelloResponse(clientSocket);

        } catch (BadRequestException exception) {
            System.err.println(
                    "Bad request from " +
                            clientAddress +
                            ": " +
                            exception.getMessage()
            );

        } catch (IOException exception) {
            System.err.println(
                    "Connection failed for " +
                            clientAddress +
                            ": " +
                            exception.getMessage()
            );

        } finally {
            System.out.println(
                    "Connection closed: " + clientAddress
            );
        }
    }

    public void stop() {
        running = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        if (
                serverSocket == null ||
                        serverSocket.isClosed()
        ) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException exception) {
            System.err.println(
                    "Failed to close server socket: " +
                            exception.getMessage()
            );
        }
    }

    private void sendHelloResponse(
            Socket socket
    ) throws IOException {
        String body = "Hello World";

        byte[] bodyBytes = body.getBytes(
                StandardCharsets.UTF_8
        );

        String responseHeaders =
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        OutputStream output = socket.getOutputStream();

        output.write(
                responseHeaders.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        output.write(bodyBytes);
        output.flush();
    }
}