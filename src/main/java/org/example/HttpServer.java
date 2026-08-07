package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HttpServer {

    private final int port;
    private final int threadPoolSize;

    private final RequestParser requestParser;
    private final ResponseWriter responseWriter;
    private final Router router;
    private final StaticFileHandler staticFileHandler;

    private final AtomicLong requestCount =
            new AtomicLong();

    private volatile boolean running;

    private ServerSocket serverSocket;
    private ExecutorService executor;

    public HttpServer(ServerConfig config) {
        this.port = config.getPort();
        this.threadPoolSize = config.getThreadPoolSize();

        this.requestParser = new RequestParser();
        this.responseWriter = new ResponseWriter();
        this.router = new Router();

        this.staticFileHandler =
                new StaticFileHandler(
                        config.getStaticDirectory()
                );
    }

    public boolean isRunning() {
        return running;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public int getPort() {
        if (
                serverSocket != null &&
                        serverSocket.isBound()
        ) {
            return serverSocket.getLocalPort();
        }

        return port;
    }

    public void get(
            String path,
            RouteHandler handler
    ) {
        ensureNotRunning();

        router.add(
                HttpMethod.GET,
                path,
                handler
        );
    }

    public void post(
            String path,
            RouteHandler handler
    ) {
        ensureNotRunning();

        router.add(
                HttpMethod.POST,
                path,
                handler
        );
    }

    public void start() {
        if (running) {
            throw new IllegalStateException(
                    "Server is already running"
            );
        }

        try {
            serverSocket = new ServerSocket(port);

            executor =
                    Executors.newFixedThreadPool(
                            threadPoolSize
                    );

            running = true;

            System.out.println(
                    "Server started on http://localhost:" +
                            getPort()
            );

            while (running) {
                Socket clientSocket =
                        serverSocket.accept();

                submitConnection(clientSocket);
            }

        } catch (SocketException exception) {
            if (running) {
                System.err.println(
                        "Server socket error: " +
                                exception.getMessage()
                );
            }

        } catch (IOException exception) {
            if (running) {
                System.err.println(
                        "Server I/O error on port " +
                                port +
                                ": " +
                                exception.getMessage()
                );
            }

        } finally {
            stop();
        }
    }

    private void submitConnection(
            Socket clientSocket
    ) throws IOException {
        try {
            executor.submit(
                    () -> handleConnection(
                            clientSocket
                    )
            );

        } catch (RejectedExecutionException exception) {
            clientSocket.close();

            if (running) {
                System.err.println(
                        "Connection rejected by worker pool"
                );
            }
        }
    }

    private void handleConnection(
            Socket clientSocket
    ) {
        String clientAddress =
                clientSocket
                        .getInetAddress()
                        .getHostAddress();

        long currentRequest =
                requestCount.incrementAndGet();

        System.out.println(
                "Request #" + currentRequest +
                        " accepted from: " +
                        clientAddress
        );

        System.out.println(
                "Handling on thread: " +
                        Thread.currentThread().getName()
        );

        try (clientSocket) {
            try {
                HttpRequest request =
                        requestParser.parse(
                                clientSocket.getInputStream()
                        );

                logRequest(request);

                HttpResponse response =
                        createResponseFor(request);

                responseWriter.write(
                        response,
                        clientSocket.getOutputStream()
                );

            } catch (BadRequestException exception) {
                System.err.println(
                        "Bad request from " +
                                clientAddress +
                                ": " +
                                exception.getMessage()
                );

                HttpResponse response =
                        new HttpResponse()
                                .status(
                                        HttpStatus.BAD_REQUEST
                                )
                                .text(
                                        "Bad Request: " +
                                                exception.getMessage()
                                );

                responseWriter.write(
                        response,
                        clientSocket.getOutputStream()
                );
            }

        } catch (IOException exception) {
            System.err.println(
                    "Connection failed for " +
                            clientAddress +
                            ": " +
                            exception.getMessage()
            );

        } finally {
            System.out.println(
                    "Connection closed: " +
                            clientAddress
            );
        }
    }

    private HttpResponse createResponseFor(
            HttpRequest request
    ) {
        RouteMatch routeMatch =
                router.match(
                        request.getMethod(),
                        request.getPath()
                );

        if (routeMatch != null) {
            return executeRoute(
                    request,
                    routeMatch
            );
        }

        if (
                request.getMethod() ==
                        HttpMethod.GET &&
                        !router.hasPath(
                                request.getPath()
                        )
        ) {
            return handleStaticFile(request);
        }

        if (
                router.hasPath(
                        request.getPath()
                )
        ) {
            return new HttpResponse()
                    .status(
                            HttpStatus.METHOD_NOT_ALLOWED
                    )
                    .text(
                            "Method Not Allowed"
                    );
        }

        return new HttpResponse()
                .status(
                        HttpStatus.NOT_FOUND
                )
                .text("Not Found");
    }

    private HttpResponse handleStaticFile(
            HttpRequest request
    ) {
        HttpResponse response =
                new HttpResponse();

        try {
            staticFileHandler.handle(
                    request,
                    response
            );

            return response;

        } catch (IOException exception) {
            System.err.println(
                    "Static file error: " +
                            exception.getMessage()
            );

            return new HttpResponse()
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .text(
                            "Internal Server Error"
                    );
        }
    }

    private HttpResponse executeRoute(
            HttpRequest request,
            RouteMatch routeMatch
    ) {
        request.setPathParameters(
                routeMatch.getPathParameters()
        );

        HttpResponse response =
                new HttpResponse();

        try {
            routeMatch
                    .getRoute()
                    .getHandler()
                    .handle(
                            request,
                            response
                    );

            return response;

        } catch (Exception exception) {
            System.err.println(
                    "Route handler failed: " +
                            exception.getMessage()
            );

            return new HttpResponse()
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .text(
                            "Internal Server Error"
                    );
        }
    }

    private void logRequest(
            HttpRequest request
    ) {
        System.out.println(
                "Method: " +
                        request.getMethod()
        );

        System.out.println(
                "Path: " +
                        request.getPath()
        );

        System.out.println(
                "Query: " +
                        request.getQueryParameters()
        );

        System.out.println(
                "Version: " +
                        request.getHttpVersion()
        );

        System.out.println(
                "Headers: " +
                        request.getHeaders()
        );

        if (
                request.getBody().length > 0
        ) {
            System.out.println(
                    "Body: " +
                            request.getBodyAsString()
            );
        }
    }

    private void ensureNotRunning() {
        if (running) {
            throw new IllegalStateException(
                    "Routes cannot be modified " +
                            "while server is running"
            );
        }
    }

    public void stop() {
        running = false;

        closeServerSocket();
        shutdownExecutor();
    }

    private void shutdownExecutor() {
        if (
                executor == null ||
                        executor.isShutdown()
        ) {
            return;
        }

        executor.shutdown();

        try {
            if (
                    !executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    )
            ) {
                System.err.println(
                        "Worker pool did not stop in time"
                );

                executor.shutdownNow();

                if (
                        !executor.awaitTermination(
                                2,
                                TimeUnit.SECONDS
                        )
                ) {
                    System.err.println(
                            "Worker pool failed to terminate"
                    );
                }
            }

        } catch (InterruptedException exception) {
            executor.shutdownNow();

            Thread.currentThread().interrupt();
        }
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
}