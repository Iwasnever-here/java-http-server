package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


public class HttpServer {

    private final int port;
    private boolean running;
    private ServerSocket serverSocket;

    public HttpServer(ServerConfig config) {
        port = config.getPort();
    }

    public boolean isRunning(){
        return running;
    }

    private void handleConnection(Socket clientSocket) {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();

        try (clientSocket) {
            System.out.println("Accepted connection from: " + clientAddress);

            // HTTP header ends with empty line, read until the empty line
            InputStream input = clientSocket.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            BufferedReader buffered = new BufferedReader(reader);

            String line = buffered.readLine();

            while (line != null && !line.isEmpty()) {
                System.out.println(line);
                line = buffered.readLine();
            }

            sendHelloResponse(clientSocket);

        } catch (IOException exception) {
            System.err.println("Connection failed for: " + clientAddress + ": " + exception.getMessage());

        } finally {
            System.out.println("Connection closed: " + clientAddress);
        }
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("Server started on http://localhost:" + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                handleConnection(clientSocket);
            }

        } catch (SocketException exception) {
            if (running) {
                System.err.println("Server socket error: " + exception.getMessage());
            }

        } catch (IOException exception) {
            System.err.println("Server I/O error on port " + port + ": " + exception.getMessage());

        } finally {
            stop();
        }
    }

    public void closeServerSocket(){
        if (serverSocket != null && !serverSocket.isClosed()) {
            return;
        }try{
            serverSocket.close();
        }catch(IOException exception){
            System.err.println("failed to close server socket: " + exception.getMessage());
        }
    }
    public void stop() {
        running = false;
        closeServerSocket();
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