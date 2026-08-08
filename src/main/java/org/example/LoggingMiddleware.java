package org.example;

public class LoggingMiddleware
        implements Middleware {

    @Override
    public void handle(HttpRequest request, HttpResponse response, MiddlewareChain chain) throws Exception {
        long start = System.nanoTime();

        chain.next(request, response);

        long durationMillis = (System.nanoTime() - start) / 1_000_000;

        System.out.println(request.getMethod() +
                        " " + request.getPath() +
                        " " + response.getStatus().getCode() +
                        " " + durationMillis + "ms");
    }
}