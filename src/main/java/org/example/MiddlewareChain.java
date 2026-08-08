package org.example;

import java.util.List;

public class MiddlewareChain {

    private final List<Middleware> middleware;
    private final RouteHandler finalHandler;

    private int index;

    public MiddlewareChain(List<Middleware> middleware, RouteHandler finalHandler) {
        this.middleware = middleware;
        this.finalHandler = finalHandler;
        this.index = 0;
    }

    public void next(HttpRequest request, HttpResponse response) throws Exception {
        if (index < middleware.size()) {
            Middleware current = middleware.get(index++);

            current.handle(request, response, this);

            return;
        }

        finalHandler.handle(request, response);
    }
}