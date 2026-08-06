package org.example;

import java.util.Map;

public final class MimeTypes {

    private static final String DEFAULT_TYPE =
            "application/octet-stream";

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=UTF-8"),
            Map.entry("css", "text/css; charset=UTF-8"),
            Map.entry("js", "application/javascript; charset=UTF-8"),
            Map.entry("json", "application/json; charset=UTF-8"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("txt", "text/plain; charset=UTF-8")
    );

    private MimeTypes() {
    }

    public static String fromPath(String path) {
        int dotIndex = path.lastIndexOf('.');

        if (
                dotIndex == -1 ||
                        dotIndex == path.length() - 1
        ) {
            return DEFAULT_TYPE;
        }

        String extension = path
                .substring(dotIndex + 1)
                .toLowerCase();

        return TYPES.getOrDefault(
                extension,
                DEFAULT_TYPE
        );
    }
}