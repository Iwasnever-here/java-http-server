package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;




public class RequestParser {
    public HttpRequest parse(InputStream inputStream){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String requestLine = readRequestLine(reader);

            String[] parts = requestLine.split("\\s+");

            if (parts.length != 3){
                throw new BadRequestException("Malformed request line");
            }

            HttpMethod method  = parseMethod(parts[0]);
            String target = parts[1];

            String[] targetParts = target.split("\\?", 2);

            String path = targetParts[0];


            if (!path.startsWith("/")) {
                throw new BadRequestException(
                        "request path must start with /"
                );
            }

            Map<String, String> queryParameters = new HashMap<>();
            Map<String, String> headers = parseHeaders(reader);
            Map<String, String> cookies = parseCookies(headers);
            byte[] body = readBody(reader, headers);

            if (targetParts.length == 2) {
                String queryString = targetParts[1];

                if (!queryString.isBlank()) {
                    String[] pairs = queryString.split("&");

                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);

                        String key = URLDecoder.decode(
                                keyValue[0],
                                StandardCharsets.UTF_8
                        );

                        String value = keyValue.length == 2
                                ? URLDecoder.decode(
                                keyValue[1],
                                StandardCharsets.UTF_8
                        )
                                : "";

                        queryParameters.put(key, value);
                    }
                }
            }


            String version = parts[2];

            validateHttpVersion(version);

            return new HttpRequest(
                    method,
                    path,
                    version,
                    headers,
                    queryParameters,
                    Map.of(),
                    cookies,
                    body
            );

        } catch (IOException exception){
            throw new BadRequestException("failed to read HTTP request");
        }
    }

    private String readRequestLine(BufferedReader reader) throws IOException{
        String requestLine = reader.readLine();

        if (requestLine == null || requestLine.isBlank()){
            throw new BadRequestException("missing request line");
        }

        return requestLine;
    }

    private HttpMethod parseMethod(String value){
        try {
            return HttpMethod.valueOf(value);

        }catch (IllegalArgumentException exception){
            throw new BadRequestException("Unsupported method: "+ value);
        }

    }

    private void validateHttpVersion(String version){
        if (!version.equals("HTTP/1.1")){
            throw new BadRequestException("unsupported version: "+ version);
        }
    }

    private Map<String, String> parseHeaders(BufferedReader reader) throws IOException{
        Map<String, String> headers = new HashMap<>();

        String line;

        while ((line = reader.readLine()) != null && !line.isEmpty()){
            int colonIndex = line.indexOf(':');

            if (colonIndex <= 0){
                throw new BadRequestException("Invalid header: "+ line);
            }

            String name  = line.substring(0, colonIndex).trim().toLowerCase();

            String value = line.substring(colonIndex + 1).trim();

            headers.put(name, value);
        }
        return headers;
    }
    private byte[] readBody(
            BufferedReader reader,
            Map<String, String> headers
    ) throws IOException {
        String contentLengthHeader = headers.get("content-length");

        if (contentLengthHeader == null) {
            return new byte[0];
        }

        int contentLength;

        try {
            contentLength = Integer.parseInt(contentLengthHeader);
        } catch (NumberFormatException exception) {
            throw new BadRequestException(
                    "Invalid Content-Length: " + contentLengthHeader
            );
        }

        if (contentLength < 0) {
            throw new BadRequestException(
                    "Content-Length cannot be negative"
            );
        }

        char[] bodyChars = new char[contentLength];

        int totalRead = 0;

        while (totalRead < contentLength) {
            int read = reader.read(
                    bodyChars,
                    totalRead,
                    contentLength - totalRead
            );

            if (read == -1) {
                throw new BadRequestException(
                        "Body shorter than Content-Length"
                );
            }

            totalRead += read;
        }

        return new String(bodyChars)
                .getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, String> parseCookies(
            Map<String, String> headers
    ) {
        Map<String, String> cookies = new HashMap<>();

        String cookieHeader =
                headers.get("cookie");

        if (
                cookieHeader == null ||
                        cookieHeader.isBlank()
        ) {
            return cookies;
        }

        String[] cookiePairs =
                cookieHeader.split(";");

        for (String cookiePair : cookiePairs) {
            String[] parts =
                    cookiePair.trim().split("=", 2);

            if (parts.length != 2) {
                continue;
            }

            String name = parts[0].trim();
            String value = parts[1].trim();

            cookies.put(name, value);
        }

        return cookies;
    }
}
