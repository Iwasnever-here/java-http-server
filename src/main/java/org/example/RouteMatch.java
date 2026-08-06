package org.example;
import java.util.Map;
public class RouteMatch {

    private final Route route;
    private Map<String, String> pathParameters;

    public RouteMatch(Route route, Map<String, String> pathParameters){
        this.route = route;
        this.pathParameters = pathParameters;
    }

    public Route getRoute(){
        return route;
    }

    public Map<String, String> getPathParameters(){
        return pathParameters;
    }



}


