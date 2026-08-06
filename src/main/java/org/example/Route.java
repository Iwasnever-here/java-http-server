package org.example;

public class Route {

    private final HttpMethod method;
    private final String pathTemplate;
    private final RouteHandler handler;

    public Route(HttpMethod method, String pathTemplate, RouteHandler handler){
        this.method = method;
        this.pathTemplate = pathTemplate;
        this.handler = handler;
    }

    public HttpMethod getMethod(){
        return method;
    }

    public String getPathTemplate(){
        return pathTemplate;
    }

    public RouteHandler getHandler(){
        return handler;
    }
}
