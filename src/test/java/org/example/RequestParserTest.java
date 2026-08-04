package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestParserTest {

    private RequestParser parser;

    @BeforeEach
    void setUp() {
        parser = new RequestParser();
    }

    @Test
    void parsesValidGetRequestLine() {
        InputStream inputStream = createInputStream(
                "GET /users HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals(HttpMethod.GET, request.getMethod());
        assertEquals("/users", request.getPath());
        assertEquals("HTTP/1.1", request.getHttpVersion());
    }

    @Test
    void parsesValidPostRequestLine() {
        InputStream inputStream = createInputStream(
                "POST /users HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals(HttpMethod.POST, request.getMethod());
        assertEquals("/users", request.getPath());
        assertEquals("HTTP/1.1", request.getHttpVersion());
    }

    @Test
    void rejectsMalformedRequestLine() {
        InputStream inputStream = createInputStream(
                "GET /users\r\n\r\n"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> parser.parse(inputStream)
        );

        assertEquals(
                "Malformed request line",
                exception.getMessage()
        );
    }

    @Test
    void rejectsUnsupportedMethod() {
        InputStream inputStream = createInputStream(
                "TRACE /users HTTP/1.1\r\n\r\n"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> parser.parse(inputStream)
        );

        assertEquals(
                "Unsupported method: TRACE",
                exception.getMessage()
        );
    }

    @Test
    void rejectsPathWithoutLeadingSlash() {
        InputStream inputStream = createInputStream(
                "GET users HTTP/1.1\r\n\r\n"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> parser.parse(inputStream)
        );

        assertEquals(
                "request path must start with /",
                exception.getMessage()
        );
    }

    @Test
    void rejectsUnsupportedHttpVersion() {
        InputStream inputStream = createInputStream(
                "GET /users HTTP/2\r\n\r\n"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> parser.parse(inputStream)
        );

        assertEquals(
                "unsupported version: HTTP/2",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMissingRequestLine() {
        InputStream inputStream = createInputStream("");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> parser.parse(inputStream)
        );

        assertEquals(
                "missing request line",
                exception.getMessage()
        );
    }

    @Test
    void rejectsBlankRequestLine() {
        InputStream inputStream = createInputStream(
                "\r\n"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> parser.parse(inputStream)
        );

        assertEquals(
                "missing request line",
                exception.getMessage()
        );
    }
    @Test
    void parsesSingleQueryParameter() {
        InputStream inputStream = createInputStream(
                "GET /users?id=10 HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals(HttpMethod.GET, request.getMethod());
        assertEquals("/users", request.getPath());
        assertEquals("10", request.getQueryParameter("id"));
    }
    @Test
    void parsesMultipleQueryParameters() {
        InputStream inputStream = createInputStream(
                "GET /search?q=java&page=2 HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals("/search", request.getPath());
        assertEquals("java", request.getQueryParameter("q"));
        assertEquals("2", request.getQueryParameter("page"));
    }
    @Test
    void parsesQueryParameterWithoutValue() {
        InputStream inputStream = createInputStream(
                "GET /users?sort HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals("", request.getQueryParameter("sort"));
    }
    @Test
    void decodesUrlEncodedQueryParameter() {
        InputStream inputStream = createInputStream(
                "GET /search?q=hello%20world HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals(
                "hello world",
                request.getQueryParameter("q")
        );
    }
    @Test
    void parsesQueryValueContainingEqualsSign() {
        InputStream inputStream = createInputStream(
                "GET /search?token=abc=123 HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals("/search", request.getPath());
        assertEquals(
                "abc=123",
                request.getQueryParameter("token")
        );
    }
    @Test
    void handlesEmptyQueryString() {
        InputStream inputStream = createInputStream(
                "GET /users? HTTP/1.1\r\n\r\n"
        );

        HttpRequest request = parser.parse(inputStream);

        assertEquals("/users", request.getPath());
        assertEquals(0, request.getQueryParameters().size());
    }

    private InputStream createInputStream(String rawRequest) {
        return new ByteArrayInputStream(
                rawRequest.getBytes(StandardCharsets.UTF_8)
        );
    }
}