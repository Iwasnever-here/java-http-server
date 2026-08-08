package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieTest {

    private RequestParser parser;

    @BeforeEach
    void setUp() {
        parser = new RequestParser();
    }

    @Test
    void parsesSingleCookie() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n" +
                        "Cookie: sessionId=abc123\r\n" +
                        "\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertEquals(
                "abc123",
                request.getCookie("sessionId")
        );
    }

    @Test
    void parsesMultipleCookies() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n" +
                        "Cookie: sessionId=abc123; theme=dark\r\n" +
                        "\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertEquals(
                "abc123",
                request.getCookie("sessionId")
        );

        assertEquals(
                "dark",
                request.getCookie("theme")
        );
    }

    @Test
    void trimsWhitespaceAroundCookies() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n" +
                        "Cookie: sessionId=abc123;   theme=dark  \r\n" +
                        "\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertEquals(
                "abc123",
                request.getCookie("sessionId")
        );

        assertEquals(
                "dark",
                request.getCookie("theme")
        );
    }

    @Test
    void cookieValueCanContainEqualsSign() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n" +
                        "Cookie: token=abc=123\r\n" +
                        "\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertEquals(
                "abc=123",
                request.getCookie("token")
        );
    }

    @Test
    void returnsNullForMissingCookie() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertNull(
                request.getCookie("sessionId")
        );
    }

    @Test
    void ignoresMalformedCookieWithoutValue() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n" +
                        "Cookie: brokenCookie; theme=dark\r\n" +
                        "\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertNull(
                request.getCookie("brokenCookie")
        );

        assertEquals(
                "dark",
                request.getCookie("theme")
        );
    }

    @Test
    void exposesParsedCookiesMap() {
        InputStream inputStream = createInputStream(
                "GET / HTTP/1.1\r\n" +
                        "Cookie: sessionId=abc123; theme=dark\r\n" +
                        "\r\n"
        );

        HttpRequest request =
                parser.parse(inputStream);

        assertEquals(
                2,
                request.getCookies().size()
        );
    }

    @Test
    void responseCookieUsesDefaultOptions() {
        HttpResponse response =
                new HttpResponse()
                        .cookie(
                                "sessionId",
                                "abc123"
                        );

        String setCookie =
                response
                        .getHeaders()
                        .get("Set-Cookie");

        assertEquals(
                "sessionId=abc123; Path=/; HttpOnly",
                setCookie
        );
    }

    @Test
    void responseCookieSupportsCustomPath() {
        HttpResponse response =
                new HttpResponse()
                        .cookie(
                                "theme",
                                "dark",
                                "/account",
                                null,
                                false
                        );

        String setCookie =
                response
                        .getHeaders()
                        .get("Set-Cookie");

        assertTrue(
                setCookie.contains(
                        "theme=dark"
                )
        );

        assertTrue(
                setCookie.contains(
                        "Path=/account"
                )
        );
    }

    @Test
    void responseCookieSupportsMaxAge() {
        HttpResponse response =
                new HttpResponse()
                        .cookie(
                                "sessionId",
                                "abc123",
                                "/",
                                3600L,
                                true
                        );

        String setCookie =
                response
                        .getHeaders()
                        .get("Set-Cookie");

        assertTrue(
                setCookie.contains(
                        "Max-Age=3600"
                )
        );
    }

    @Test
    void responseCookieCanBeHttpOnly() {
        HttpResponse response =
                new HttpResponse()
                        .cookie(
                                "sessionId",
                                "abc123",
                                "/",
                                null,
                                true
                        );

        String setCookie =
                response
                        .getHeaders()
                        .get("Set-Cookie");

        assertTrue(
                setCookie.contains(
                        "HttpOnly"
                )
        );
    }

    @Test
    void responseCookieCanDisableHttpOnly() {
        HttpResponse response =
                new HttpResponse()
                        .cookie(
                                "theme",
                                "dark",
                                "/",
                                null,
                                false
                        );

        String setCookie =
                response
                        .getHeaders()
                        .get("Set-Cookie");

        assertTrue(
                !setCookie.contains("HttpOnly")
        );
    }

    @Test
    void zeroMaxAgeCanDeleteCookie() {
        HttpResponse response =
                new HttpResponse()
                        .cookie(
                                "sessionId",
                                "",
                                "/",
                                0L,
                                true
                        );

        String setCookie =
                response
                        .getHeaders()
                        .get("Set-Cookie");

        assertTrue(
                setCookie.contains(
                        "sessionId="
                )
        );

        assertTrue(
                setCookie.contains(
                        "Max-Age=0"
                )
        );
    }

    private InputStream createInputStream(
            String rawRequest
    ) {
        return new ByteArrayInputStream(
                rawRequest.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}