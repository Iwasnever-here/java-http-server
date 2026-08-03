package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class HttpServer {

    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Server listening on port " + port);

        // keep the server running, accepting one client at a time
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());


            InputStream input = clientSocket.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            BufferedReader buffered = new BufferedReader(reader);
            String line = buffered.readLine();


            // HTTP headers end with blank line, so when reaches the blank line stop
            while (line != null && !line.isEmpty()){
                System.out.println(line);

                line = buffered.readLine();
            }

            sendHelloResponse(clientSocket);
            clientSocket.close();
        }

    }

    private void sendHelloResponse(Socket socket) throws IOException{
        String body = "hello world";

        // convert the response body to UTF-8 bytes so content length matches
        // the actual number of bytes sent over the network
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);


        // HTTP requires CRLF ("\r\n") at end of each header line
        String responseHeaders =
                "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "Content-Length: " + bodyBytes.length
                        + "\r\n" + "Connection: close\r\n" + "\r\n";

        OutputStream output = socket.getOutputStream();
        output.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
        output.write(bodyBytes);
        output.flush();



    }
}