package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        HttpServer server = new HttpServer(8080);
        server.start();
    }
}