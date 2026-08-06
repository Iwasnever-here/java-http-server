package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHandler {

    private final Path publicDirectory;

    public StaticFileHandler(Path publicDirectory) {
        this.publicDirectory = publicDirectory
                .toAbsolutePath()
                .normalize();
    }

    public void handle(
            HttpRequest request,
            HttpResponse response
    ) throws IOException {
        String requestedPath = request.getPath();

        if (requestedPath.equals("/")) {
            requestedPath = "/index.html";
        }

        Path resolvedPath = resolveSecurely(requestedPath);

        if (resolvedPath == null) {
            response
                    .status(HttpStatus.FORBIDDEN)
                    .text("Forbidden");
            return;
        }

        if (!Files.exists(resolvedPath)) {
            response
                    .status(HttpStatus.NOT_FOUND)
                    .text("Not Found");
            return;
        }

        if (Files.isDirectory(resolvedPath)) {
            resolvedPath = resolvedPath.resolve("index.html");

            if (!Files.exists(resolvedPath)) {
                response
                        .status(HttpStatus.NOT_FOUND)
                        .text("Not Found");
                return;
            }
        }

        if (!Files.isReadable(resolvedPath)) {
            response
                    .status(HttpStatus.FORBIDDEN)
                    .text("Forbidden");
            return;
        }

        byte[] fileBytes = Files.readAllBytes(resolvedPath);

        response
                .header(
                        "Content-Type",
                        MimeTypes.fromPath(
                                resolvedPath.toString()
                        )
                )
                .body(fileBytes);
    }

    private Path resolveSecurely(String requestedPath) {
        String relativePath = requestedPath.startsWith("/")
                ? requestedPath.substring(1)
                : requestedPath;

        Path resolvedPath = publicDirectory
                .resolve(relativePath)
                .normalize();

        if (!resolvedPath.startsWith(publicDirectory)) {
            return null;
        }

        return resolvedPath;
    }
}