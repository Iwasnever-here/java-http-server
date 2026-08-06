package org.example;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequest {

    private final HttpMethod method;
    private final String path;
    private final String httpVersion;
    private final Map<String, String> headers;
    private final Map<String, String> queryParameters;
    private Map<String, String> pathParameters;
    private final byte[] body;

    public HttpRequest(
            HttpMethod method,
            String path,
            String httpVersion,
            Map<String, String> headers,
            Map<String, String> queryParameters,
            Map<String, String> pathParameters,
            byte[] body
    ) {
        this.method = method;
        this.path = path;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.queryParameters = queryParameters;
        this.pathParameters = pathParameters;
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getQueryParameter(String name) {
        return queryParameters.get(name);
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public byte[] getBody() {
        return body;
    }

    public String getHeader(String name){
        return headers.get(name.toLowerCase());
    }

    public String getBodyAsString(){
        return new String(body, StandardCharsets.UTF_8);
    }

    public String getPathParameter(String name) {
        return pathParameters.get(name);
    }

    public void setPathParameters(
            Map<String, String> pathParameters
    ) {
        this.pathParameters = pathParameters;
    }
}