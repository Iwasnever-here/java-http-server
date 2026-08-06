package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class HttpServer {

    private final int port;
    private final RequestParser requestParser;
    private final ResponseWriter responseWriter;
    private final Router router;

    private final StaticFileHandler staticFileHandler;

    private boolean running;
    private ServerSocket serverSocket;

    public HttpServer(ServerConfig config) {
        this.port = config.getPort();
        this.requestParser = new RequestParser();
        this.responseWriter = new ResponseWriter();
        this.router = new Router();
        this.staticFileHandler = new StaticFileHandler(config.getStaticDirectory());
    }

    public boolean isRunning() {
        return running;
    }

    public void get(String path, RouteHandler handler){
        router.add(HttpMethod.GET, path, handler);
    }
    public void post(String path, RouteHandler handler){
        router.add(HttpMethod.POST, path, handler);
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
            try {
                HttpRequest request = requestParser.parse(
                        clientSocket.getInputStream()
                );

                logRequest(request);

                HttpResponse response = createResponseFor(request);
                responseWriter.write(response, clientSocket.getOutputStream());

            } catch (BadRequestException exception) {
                System.err.println(
                        "Bad request from " +
                                clientAddress +
                                ": " +
                                exception.getMessage()
                );

                HttpResponse response = new HttpResponse()
                        .status(HttpStatus.BAD_REQUEST)
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
                    "Connection closed: " + clientAddress
            );
        }
    }

    private HttpResponse createResponseFor(
            HttpRequest request
    ) {
        RouteMatch routeMatch = router.match(
                request.getMethod(),
                request.getPath()
        );

        if (routeMatch != null) {
            return executeRoute(request, routeMatch);
        }

        if (
                request.getMethod() == HttpMethod.GET &&
                        !router.hasPath(request.getPath())
        ) {
            HttpResponse response = new HttpResponse();

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
                        .text("Internal Server Error");
            }
        }

        if (router.hasPath(request.getPath())) {
            return new HttpResponse()
                    .status(
                            HttpStatus.METHOD_NOT_ALLOWED
                    )
                    .text("Method Not Allowed");
        }

        return new HttpResponse()
                .status(HttpStatus.NOT_FOUND)
                .text("Not Found");
    }

    private HttpResponse executeRoute(
            HttpRequest request,
            RouteMatch routeMatch
    ) {
        request.setPathParameters(
                routeMatch.getPathParameters()
        );

        HttpResponse response = new HttpResponse();

        try {
            routeMatch
                    .getRoute()
                    .getHandler()
                    .handle(request, response);

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
                    .text("Internal Server Error");
        }
    }

    private void logRequest(HttpRequest request) {
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

        System.out.println(
                "Headers: " + request.getHeaders()
        );

        if (request.getBody().length > 0) {
            System.out.println(
                    "Body: " + request.getBodyAsString()
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


}