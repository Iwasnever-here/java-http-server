package org.example;
import java.util.HashMap;
import java.util.Map;

public class Router {
    private final Map<HttpMethod, Map<String, RouteHandler>> routes;

    public Router(){
        this.routes = new HashMap<>();
    }

    public void add(HttpMethod method, String path, RouteHandler handler){
        Map<String, RouteHandler> methodRoutes =
                routes.computeIfAbsent(
                        method,
                        ignored -> new HashMap<>()
                );

        if (methodRoutes.containsKey(path)){
            throw new IllegalArgumentException("Route already registerd: "+ method + " "+ path);

        }
        methodRoutes.put(path, handler);
    }

    public RouteHandler find(HttpMethod method, String path){
        Map<String, RouteHandler> methodRoutes = routes.get(method);

        if (methodRoutes == null) {
            return null;
        }
        return methodRoutes.get(path);
    }

    public boolean hasPath(String path){
        for (Map<String, RouteHandler> methodRoutes : routes.values()) {
            if (methodRoutes.containsKey(path)){
                return true;
            }
        }
        return false;
    }
}
