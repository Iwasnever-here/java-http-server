package org.example;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {

        ServerConfig config = new ServerConfig(8080, 4, Path.of("public"));
        HttpServer server = new HttpServer(config);
        server.start();
    }
}