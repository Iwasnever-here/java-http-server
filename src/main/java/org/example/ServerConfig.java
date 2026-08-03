package org.example;
import java.nio.file.Path;
import java.util.Objects;


public class ServerConfig {
    private final int port;
    private final int threadPoolSize;
    private final Path staticDirectory;

    public ServerConfig(int port, int threadPoolSize, Path staticDirectory){

        if (port < 0 || port > 65535){
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (threadPoolSize <= 0){
            throw new IllegalArgumentException("thread pool size must be > 0");
        }
        this.port= port;
        this.threadPoolSize = threadPoolSize;
        this.staticDirectory = Objects.requireNonNull(staticDirectory, "static directory must not be null");

    }

    public int getPort(){
        return port;
    }

    public int getThreadPoolSize(){
        return threadPoolSize;
    }

    public Path getStaticDirectory(){
        return staticDirectory;
    }
}
