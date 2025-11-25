package com.stresstest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class UDPServer {
    private final Config.ServerConfig config;
    private final ExecutorService executor;
    private final AtomicLong packetCount = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
    private volatile boolean running = false;

    public UDPServer(Config.ServerConfig config) {
        this.config = config;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        if (!config.udpEnabled) {
            System.out.println("UDP Server is disabled in config");
            return;
        }

        running = true;
        
        for (int port = config.udpPortRange.start; port <= config.udpPortRange.end; port++) {
            final int currentPort = port;
            executor.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket(currentPort)) {
                    System.out.println("UDP Server listening on port " + currentPort);
                    byte[] buffer = new byte[8192];
                    
                    while (running) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socket.receive(packet);
                            
                            packetCount.incrementAndGet();
                            totalBytesReceived.addAndGet(packet.getLength());
                            
                            // Echo back to client
                            DatagramPacket response = new DatagramPacket(
                                packet.getData(), 
                                packet.getLength(),
                                packet.getAddress(),
                                packet.getPort()
                            );
                            socket.send(response);
                        } catch (IOException e) {
                            if (running) {
                                System.err.println("Error receiving UDP packet on port " + currentPort + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.err.println("Error starting UDP server on port " + currentPort + ": " + e.getMessage());
                }
            });
        }
        
        System.out.println("UDP Server started on ports " + config.udpPortRange.start + "-" + config.udpPortRange.end);
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }

    public long getPacketCount() {
        return packetCount.get();
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
    }
}

