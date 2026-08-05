package org.example;


@FunctionalInterface
public interface RouteHandler {

    void handle(HttpRequest request, HttpResponse response) throws Exception;
}
