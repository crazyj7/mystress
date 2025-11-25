package com.stresstest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class UDPClient implements Runnable {
    private final Config.ClientConfig config;
    private final Config.TestConfig testConfig;
    private final AtomicLong packetCount;
    private final AtomicLong totalBytesSent;
    private final Random random = new Random();

    public UDPClient(Config.ClientConfig config, Config.TestConfig testConfig,
                     AtomicLong packetCount, AtomicLong totalBytesSent) {
        this.config = config;
        this.testConfig = testConfig;
        this.packetCount = packetCount;
        this.totalBytesSent = totalBytesSent;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        
        // Retry socket creation on failure
        while (socket == null) {
            try {
                socket = new DatagramSocket();
            } catch (IOException e) {
                // Socket creation failed - retry after 1 second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        
        try {
            byte[] data = new byte[testConfig.dataSize];
            random.nextBytes(data);
            
            // Resolve server address with retry on failure
            InetAddress serverAddress = null;
            while (serverAddress == null) {
                try {
                    serverAddress = InetAddress.getByName(config.serverHost);
                } catch (IOException e) {
                    // Failed to resolve - retry after 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        socket.close();
                        return;
                    }
                }
            }

            for (int i = 0; i < testConfig.iterations; i++) {
                int port = config.udpPortRange.start + 
                          random.nextInt(config.udpPortRange.end - config.udpPortRange.start + 1);
                
                try {
                    DatagramPacket packet = new DatagramPacket(
                        data, data.length, serverAddress, port
                    );
                    socket.send(packet);
                    packetCount.incrementAndGet();
                    totalBytesSent.addAndGet(data.length);
                    
                    // Try to receive echo (with timeout)
                    socket.setSoTimeout(100);
                    byte[] buffer = new byte[testConfig.dataSize];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    socket.receive(response);
                } catch (IOException e) {
                    // Timeout or error - continue
                }
                
                if (testConfig.delayBetweenIterations > 0 && i < testConfig.iterations - 1) {
                    try {
                        Thread.sleep(testConfig.delayBetweenIterations);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}

