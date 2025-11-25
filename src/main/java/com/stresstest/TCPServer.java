package com.stresstest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class TCPServer {
    private final Config.ServerConfig config;
    private final ExecutorService executor;
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
    private volatile boolean running = false;

    public TCPServer(Config.ServerConfig config) {
        this.config = config;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        if (!config.tcpEnabled) {
            System.out.println("TCP Server is disabled in config");
            return;
        }

        running = true;
        
        for (int port = config.tcpPortRange.start; port <= config.tcpPortRange.end; port++) {
            final int currentPort = port;
            executor.submit(() -> {
                try (ServerSocket serverSocket = new ServerSocket(currentPort)) {
                    System.out.println("TCP Server listening on port " + currentPort);
                    
                    while (running) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            connectionCount.incrementAndGet();
                            
                            executor.submit(() -> handleClient(clientSocket));
                        } catch (IOException e) {
                            if (running) {
                                System.err.println("Error accepting connection on port " + currentPort + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error starting TCP server on port " + currentPort + ": " + e.getMessage());
                }
            });
        }
        
        System.out.println("TCP Server started on ports " + config.tcpPortRange.start + "-" + config.tcpPortRange.end);
    }

    private void handleClient(Socket socket) {
        try {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                totalBytesReceived.addAndGet(bytesRead);
                // Echo back to client
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            // Connection closed or error - this is expected
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }

    public long getConnectionCount() {
        return connectionCount.get();
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
    }
}

