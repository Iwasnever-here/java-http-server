package org.example;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {

    private HttpStatus status;
    private final Map<String, String> headers;
    private byte[] body;

    public HttpResponse(){
        this.status = HttpStatus.OK;
        this.headers = new LinkedHashMap<>();
        this.body = new byte[0];

        headers.put("Connection", "close");
    }


    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getHeaders(){
        return headers;
    }

    public byte[] getBody(){
        return body;
    }

    public HttpResponse status(HttpStatus status){
        this.status = status;
        return this;
    }

    public HttpResponse header(String name, String value){
        headers.put(name, value);
        return this;
    }

    public HttpResponse body(byte[] value) {
        this.body = value;
        return this;
    }

    public HttpResponse cookie(String name, String value){
        return cookie(name, value, "/", null, true);
    }

    public HttpResponse body(String value){
        this.body = value.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public HttpResponse text (String value){
        header(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );
        return body(value);
    }

    public HttpResponse json(String value) {
        header(
                "Content-Type",
                "application/json; charset=UTF-8"
        );
        return body(value);
    }

    public HttpResponse cookie(String name, String value, String path, Long maxAge, boolean httpOnly){
        StringBuilder cookie = new StringBuilder();

        cookie.append(name).append("=").append(value);

        if (path != null){
            cookie.append("; Path=").append(path);
        }

        if (maxAge != null){
            cookie.append("; Max-Age=").append(maxAge);
        }
        if (httpOnly){
            cookie.append("; HttpOnly");
        }
        header("Set-Cookie", cookie.toString());

        return this;
    }
}
