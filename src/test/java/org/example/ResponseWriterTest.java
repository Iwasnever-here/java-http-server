package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseWriterTest {

    private ResponseWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ResponseWriter();
    }

    @Test
    void writesTextResponse() throws Exception {
        HttpResponse response = new HttpResponse()
                .text("Hello");

        String rawResponse = writeResponse(response);

        assertTrue(
                rawResponse.startsWith(
                        "HTTP/1.1 200 OK\r\n"
                )
        );

        assertTrue(
                rawResponse.contains(
                        "Content-Type: " +
                                "text/plain; charset=UTF-8\r\n"
                )
        );

        assertTrue(
                rawResponse.contains(
                        "Content-Length: 5\r\n"
                )
        );

        assertTrue(
                rawResponse.endsWith(
                        "\r\n\r\nHello"
                )
        );
    }

    @Test
    void writesJsonResponse() throws Exception {
        String json = "{\"message\":\"Hello\"}";

        HttpResponse response = new HttpResponse()
                .json(json);

        String rawResponse = writeResponse(response);

        assertTrue(
                rawResponse.contains(
                        "Content-Type: " +
                                "application/json; charset=UTF-8\r\n"
                )
        );

        assertTrue(rawResponse.endsWith(json));
    }

    @Test
    void writesCustomStatus() throws Exception {
        HttpResponse response = new HttpResponse()
                .status(HttpStatus.CREATED)
                .text("Created");

        String rawResponse = writeResponse(response);

        assertTrue(
                rawResponse.startsWith(
                        "HTTP/1.1 201 Created\r\n"
                )
        );
    }

    @Test
    void writesCustomHeader() throws Exception {
        HttpResponse response = new HttpResponse()
                .header("X-Test", "value")
                .text("Hello");

        String rawResponse = writeResponse(response);

        assertTrue(
                rawResponse.contains(
                        "X-Test: value\r\n"
                )
        );
    }

    @Test
    void calculatesUtf8ContentLength() throws Exception {
        String body = "Hello £";

        HttpResponse response = new HttpResponse()
                .text(body);

        String rawResponse = writeResponse(response);

        int expectedLength = body
                .getBytes(StandardCharsets.UTF_8)
                .length;

        assertTrue(
                rawResponse.contains(
                        "Content-Length: " +
                                expectedLength +
                                "\r\n"
                )
        );
    }

    @Test
    void writesEmptyBody() throws Exception {
        HttpResponse response = new HttpResponse();

        String rawResponse = writeResponse(response);

        assertTrue(
                rawResponse.contains(
                        "Content-Length: 0\r\n"
                )
        );

        assertTrue(
                rawResponse.endsWith("\r\n\r\n")
        );
    }

    private String writeResponse(
            HttpResponse response
    ) throws Exception {
        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        writer.write(response, output);

        return output.toString(
                StandardCharsets.UTF_8
        );
    }
}