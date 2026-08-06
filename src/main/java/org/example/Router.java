package org.example;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Router {

    private final Map<HttpMethod, Map<String, RouteHandler>> exactRoutes;
    private final List<Route> dynamicRoutes;

    public Router(){
        this.exactRoutes = new HashMap<>();
        this.dynamicRoutes = new ArrayList<>();
    }

    public void add(
            HttpMethod method,
            String path,
            RouteHandler handler
    ) {
        if (isDynamicPath(path)) {
            addDynamicRoute(method, path, handler);
            return;
        }

        Map<String, RouteHandler> methodRoutes =
                exactRoutes.computeIfAbsent(
                        method,
                        ignored -> new HashMap<>()
                );

        if (methodRoutes.containsKey(path)) {
            throw new IllegalArgumentException(
                    "Route already registered: " +
                            method +
                            " " +
                            path
            );
        }

        methodRoutes.put(path, handler);
    }

    public RouteMatch match(
            HttpMethod method,
            String path
    ) {
        RouteMatch exactMatch = findExactMatch(
                method,
                path
        );

        if (exactMatch != null) {
            return exactMatch;
        }

        return findDynamicMatch(method, path);
    }

    private RouteMatch findExactMatch(
            HttpMethod method,
            String path
    ) {
        Map<String, RouteHandler> methodRoutes =
                exactRoutes.get(method);

        if (methodRoutes == null) {
            return null;
        }

        RouteHandler handler = methodRoutes.get(path);

        if (handler == null) {
            return null;
        }

        Route route = new Route(
                method,
                path,
                handler
        );

        return new RouteMatch(
                route,
                Map.of()
        );
    }
    private Map<String, String> matchPath(
            String template,
            String actualPath
    ) {
        String[] templateSegments = splitPath(template);
        String[] actualSegments = splitPath(actualPath);

        if (templateSegments.length != actualSegments.length) {
            return null;
        }

        Map<String, String> parameters = new HashMap<>();

        for (int index = 0;
             index < templateSegments.length;
             index++) {

            String templateSegment = templateSegments[index];
            String actualSegment = actualSegments[index];

            if (templateSegment.startsWith(":")) {
                String parameterName =
                        templateSegment.substring(1);

                parameters.put(
                        parameterName,
                        actualSegment
                );

                continue;
            }

            if (!templateSegment.equals(actualSegment)) {
                return null;
            }
        }

        return parameters;
    }

    private String[] splitPath(String path) {
        if (path.equals("/")) {
            return new String[0];
        }

        return path.substring(1).split("/");
    }

    private RouteMatch findDynamicMatch(
            HttpMethod method,
            String path
    ) {
        for (Route route : dynamicRoutes) {
            if (route.getMethod() != method) {
                continue;
            }

            Map<String, String> parameters =
                    matchPath(
                            route.getPathTemplate(),
                            path
                    );

            if (parameters != null) {
                return new RouteMatch(
                        route,
                        parameters
                );
            }
        }

        return null;
    }

    public boolean hasPath(String path) {
        for (Map<String, RouteHandler> methodRoutes : exactRoutes.values()) {
            if (methodRoutes.containsKey(path)) {
                return true;
            }
        }

        for (Route route : dynamicRoutes) {
            if(matchPath(route.getPathTemplate(), path) != null) {
                return true;
            }
        }

        return false;
    }

    private boolean isDynamicPath(String path) {
        String[] segments = path.split("/");

        for (String segment : segments) {
            if (segment.startsWith(":")) {
                return true;
            }
        }

        return false;
    }

    private void addDynamicRoute(
            HttpMethod method,
            String path,
            RouteHandler handler
    ) {
        for (Route route : dynamicRoutes) {
            if (
                    route.getMethod() == method &&
                            route.getPathTemplate().equals(path)
            ) {
                throw new IllegalArgumentException(
                        "Route already registered: " +
                                method +
                                " " +
                                path
                );
            }
        }

        dynamicRoutes.add(
                new Route(method, path, handler)
        );
    }

}
