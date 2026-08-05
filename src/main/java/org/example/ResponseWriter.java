package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseWriter {

    public void write(
            HttpResponse response,
            OutputStream outputStream
    ) throws IOException {
        byte[] body = response.getBody();

        Map<String, String> headers =
                new LinkedHashMap<>(response.getHeaders());

        headers.put(
                "Content-Length",
                String.valueOf(body.length)
        );

        StringBuilder responseHead = new StringBuilder();

        responseHead
                .append("HTTP/1.1 ")
                .append(response.getStatus().getCode())
                .append(" ")
                .append(response.getStatus().getReasonPhrase())
                .append("\r\n");

        for (Map.Entry<String, String> header
                : headers.entrySet()) {
            responseHead
                    .append(header.getKey())
                    .append(": ")
                    .append(header.getValue())
                    .append("\r\n");
        }

        responseHead.append("\r\n");

        byte[] headerBytes = responseHead
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        outputStream.write(headerBytes);
        outputStream.write(body);
        outputStream.flush();
    }
}