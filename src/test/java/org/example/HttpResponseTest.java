package org.example;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpResponseTest {

    @Test
    void usesOkStatusByDefault() {
        HttpResponse response = new HttpResponse();

        assertEquals(
                HttpStatus.OK,
                response.getStatus()
        );
    }

    @Test
    void usesEmptyBodyByDefault() {
        HttpResponse response = new HttpResponse();

        assertEquals(
                0,
                response.getBody().length
        );
    }

    @Test
    void setsCustomStatus() {
        HttpResponse response = new HttpResponse()
                .status(HttpStatus.CREATED);

        assertEquals(
                HttpStatus.CREATED,
                response.getStatus()
        );
    }

    @Test
    void addsCustomHeader() {
        HttpResponse response = new HttpResponse()
                .header("X-Test", "hello");

        assertEquals(
                "hello",
                response.getHeaders().get("X-Test")
        );
    }

    @Test
    void createsTextResponse() {
        HttpResponse response = new HttpResponse()
                .text("Hello");

        assertEquals(
                "text/plain; charset=UTF-8",
                response.getHeaders().get("Content-Type")
        );

        assertEquals(
                "Hello",
                new String(
                        response.getBody(),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void createsJsonResponse() {
        HttpResponse response = new HttpResponse()
                .json("{\"message\":\"Hello\"}");

        assertEquals(
                "application/json; charset=UTF-8",
                response.getHeaders().get("Content-Type")
        );
    }

    @Test
    void closesConnectionByDefault() {
        HttpResponse response = new HttpResponse();

        assertEquals(
                "close",
                response.getHeaders().get("Connection")
        );
    }

}