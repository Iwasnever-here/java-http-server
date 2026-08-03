package org.example;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerConfigTest {

    @Test
    void storesPort() {
        ServerConfig config = new ServerConfig(
                8080,
                4,
                Path.of("public")
        );

        assertEquals(8080, config.getPort());
    }

    @Test
    void storesThreadPoolSize() {
        ServerConfig config = new ServerConfig(
                8080,
                4,
                Path.of("public")
        );

        assertEquals(4, config.getThreadPoolSize());
    }

    @Test
    void storesStaticDirectory() {
        Path directory = Path.of("public");

        ServerConfig config = new ServerConfig(
                8080,
                4,
                directory
        );

        assertEquals(directory, config.getStaticDirectory());
    }

    @Test
    void allowsPortZero() {
        ServerConfig config = new ServerConfig(
                0,
                4,
                Path.of("public")
        );

        assertEquals(0, config.getPort());
    }

    @Test
    void allowsMaximumPort() {
        ServerConfig config = new ServerConfig(
                65535,
                4,
                Path.of("public")
        );

        assertEquals(65535, config.getPort());
    }

    @Test
    void rejectsNegativePort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServerConfig(
                        -1,
                        4,
                        Path.of("public")
                )
        );
    }

    @Test
    void rejectsPortAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServerConfig(
                        65536,
                        4,
                        Path.of("public")
                )
        );
    }

    @Test
    void rejectsZeroThreadPoolSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServerConfig(
                        8080,
                        0,
                        Path.of("public")
                )
        );
    }

    @Test
    void rejectsNegativeThreadPoolSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServerConfig(
                        8080,
                        -1,
                        Path.of("public")
                )
        );
    }

    @Test
    void rejectsNullStaticDirectory() {
        assertThrows(
                NullPointerException.class,
                () -> new ServerConfig(
                        8080,
                        4,
                        null
                )
        );
    }
}