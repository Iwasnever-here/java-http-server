package org.example;

@FunctionalInterface
public interface Middleware {

    void handle(
            HttpRequest request,
            HttpResponse response,
            MiddlewareChain chain
    ) throws Exception;
}