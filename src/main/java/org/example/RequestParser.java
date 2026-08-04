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

            if (!target.startsWith("/")){
                throw new BadRequestException("request path must start with /");
            }
            return new HttpRequest(method, path, version, Map.of(), queryParameters, Map.of(), new byte[0]);

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
}
