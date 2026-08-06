package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MimeTypesTest {

    @Test
    void detectsHtml() {
        assertEquals(
                "text/html; charset=UTF-8",
                MimeTypes.fromPath("index.html")
        );
    }

    @Test
    void detectsCss() {
        assertEquals(
                "text/css; charset=UTF-8",
                MimeTypes.fromPath("styles.css")
        );
    }

    @Test
    void detectsJavaScript() {
        assertEquals(
                "application/javascript; charset=UTF-8",
                MimeTypes.fromPath("app.js")
        );
    }

    @Test
    void detectsPng() {
        assertEquals(
                "image/png",
                MimeTypes.fromPath("logo.png")
        );
    }

    @Test
    void detectsJpeg() {
        assertEquals(
                "image/jpeg",
                MimeTypes.fromPath("photo.jpg")
        );
    }

    @Test
    void handlesUppercaseExtension() {
        assertEquals(
                "image/png",
                MimeTypes.fromPath("IMAGE.PNG")
        );
    }

    @Test
    void usesDefaultForUnknownExtension() {
        assertEquals(
                "application/octet-stream",
                MimeTypes.fromPath("file.xyz")
        );
    }

    @Test
    void usesDefaultWhenExtensionIsMissing() {
        assertEquals(
                "application/octet-stream",
                MimeTypes.fromPath("README")
        );
    }
}