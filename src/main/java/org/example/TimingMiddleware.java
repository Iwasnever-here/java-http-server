package org.example;

public class TimingMiddleware implements Middleware{
    @Override
    public void handle(HttpRequest request, HttpResponse response, MiddlewareChain chain) throws Exception {
        long start = System.nanoTime();
        chain.next(request, response);

        long durationNanos = System.nanoTime() - start;
        long durationMillis = durationNanos / 1_000_000;

        response.header("X-Response-Time", durationMillis + "ms");

    }
}
