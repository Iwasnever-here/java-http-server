package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticFileHandlerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void servesIndexFileForRootPath() throws Exception {
        Files.writeString(
                tempDirectory.resolve("index.html"),
                "<h1>Hello</h1>"
        );

        StaticFileHandler handler =
                new StaticFileHandler(tempDirectory);

        HttpRequest request = createRequest("/");

        HttpResponse response = new HttpResponse();

        handler.handle(request, response);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
                "text/html; charset=UTF-8",
                response.getHeaders().get("Content-Type")
        );
        assertEquals(
                "<h1>Hello</h1>",
                new String(
                        response.getBody(),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void servesCssFile() throws Exception {
        Files.writeString(
                tempDirectory.resolve("styles.css"),
                "body { margin: 0; }"
        );

        StaticFileHandler handler =
                new StaticFileHandler(tempDirectory);

        HttpResponse response = new HttpResponse();

        handler.handle(
                createRequest("/styles.css"),
                response
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
                "text/css; charset=UTF-8",
                response.getHeaders().get("Content-Type")
        );
    }

    @Test
    void returnsNotFoundForMissingFile() throws Exception {
        StaticFileHandler handler =
                new StaticFileHandler(tempDirectory);

        HttpResponse response = new HttpResponse();

        handler.handle(
                createRequest("/missing.html"),
                response
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatus()
        );
    }

    @Test
    void blocksDirectoryTraversal() throws Exception {
        StaticFileHandler handler =
                new StaticFileHandler(tempDirectory);

        HttpResponse response = new HttpResponse();

        handler.handle(
                createRequest("/../../secret.txt"),
                response
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatus()
        );
    }

    @Test
    void loadsIndexFromDirectory() throws Exception {
        Path docsDirectory =
                Files.createDirectory(
                        tempDirectory.resolve("docs")
                );

        Files.writeString(
                docsDirectory.resolve("index.html"),
                "<h1>Docs</h1>"
        );

        StaticFileHandler handler =
                new StaticFileHandler(tempDirectory);

        HttpResponse response = new HttpResponse();

        handler.handle(
                createRequest("/docs"),
                response
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
                "<h1>Docs</h1>",
                new String(
                        response.getBody(),
                        StandardCharsets.UTF_8
                )
        );
    }

    private HttpRequest createRequest(String path) {
        return new HttpRequest(
                HttpMethod.GET,
                path,
                "HTTP/1.1",
                Map.of(),
                Map.of(),
                Map.of(),
                new byte[0]
        );
    }
}