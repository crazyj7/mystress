package com.stresstest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StressTest {
    private final Config config;
    private TCPServer tcpServer;
    private UDPServer udpServer;
    private FileStressTest fileStressTest;
    private ExecutorService clientExecutor;
    private final AtomicLong tcpConnectionCount = new AtomicLong(0);
    private final AtomicLong tcpBytesSent = new AtomicLong(0);
    private final AtomicLong udpPacketCount = new AtomicLong(0);
    private final AtomicLong udpBytesSent = new AtomicLong(0);

    public StressTest(Config config) {
        this.config = config;
    }

    public void start() {
        System.out.println("Starting Stress Test Simulator...");
        System.out.println("Config loaded:");
        if (config.networkTest != null) {
            System.out.println("  Network Test: " + (config.networkTest.enabled ? "Enabled" : "Disabled"));
        }
        if (config.networkTest != null && config.networkTest.enabled) {
            System.out.println("  TCP: " + (config.server.tcpEnabled ? "Enabled" : "Disabled"));
            System.out.println("  UDP: " + (config.server.udpEnabled ? "Enabled" : "Disabled"));
            System.out.println("  TCP Client Threads: " + config.client.tcpThreadCount);
            System.out.println("  UDP Client Threads: " + config.client.udpThreadCount);
            System.out.println("  Server Host: " + config.client.serverHost);
            System.out.println("  Iterations per thread: " + config.networkTest.iterations);
        }
        if (config.fileTest != null) {
            System.out.println("  File Test: " + (config.fileTest.enabled ? "Enabled" : "Disabled"));
        }
        System.out.println();

        // Start network test servers and clients only if network test is enabled
        if (config.networkTest != null && config.networkTest.enabled) {
            // Start servers
            if (config.server.tcpEnabled) {
                tcpServer = new TCPServer(config.server);
                tcpServer.start();
            }

            if (config.server.udpEnabled) {
                udpServer = new UDPServer(config.server);
                udpServer.start();
            }

            // Wait a bit for servers to start
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Start clients
            clientExecutor = Executors.newCachedThreadPool();

            if (config.client.tcpEnabled) {
                System.out.println("Starting " + config.client.tcpThreadCount + " TCP client threads...");
                for (int i = 0; i < config.client.tcpThreadCount; i++) {
                    clientExecutor.submit(new TCPClient(config.client, config.networkTest, 
                        tcpConnectionCount, tcpBytesSent));
                }
            }

            if (config.client.udpEnabled) {
                System.out.println("Starting " + config.client.udpThreadCount + " UDP client threads...");
                for (int i = 0; i < config.client.udpThreadCount; i++) {
                    clientExecutor.submit(new UDPClient(config.client, config.networkTest, 
                        udpPacketCount, udpBytesSent));
                }
            }
        } else {
            System.out.println("Network test is disabled. Skipping network test servers and clients.");
        }

        // Start file stress test
        if (config.fileTest != null && config.fileTest.enabled) {
            fileStressTest = new FileStressTest(config.fileTest);
            fileStressTest.start();
        }

        if (config.networkTest != null && config.networkTest.enabled) {
            System.out.println("All network test clients started.");
        }
        System.out.println("Stress test running...");
        System.out.println("Press Ctrl+C to stop.");
    }

    public void stop() {
        System.out.println("\nStopping stress test...");
        
        if (clientExecutor != null) {
            clientExecutor.shutdown();
            try {
                if (!clientExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    clientExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (tcpServer != null) {
            tcpServer.stop();
        }

        if (udpServer != null) {
            udpServer.stop();
        }

        if (fileStressTest != null) {
            fileStressTest.stop();
        }

        printStatistics();
    }

    private void printStatistics() {
        System.out.println("\n=== Stress Test Statistics ===");
        
        if (config.networkTest != null && config.networkTest.enabled) {
            if (config.server.tcpEnabled && tcpServer != null) {
                System.out.println("TCP Server:");
                System.out.println("  Connections handled: " + tcpServer.getConnectionCount());
                System.out.println("  Total bytes received: " + tcpServer.getTotalBytesReceived());
            }
            
            if (config.client.tcpEnabled) {
                System.out.println("TCP Client:");
                System.out.println("  Connections made: " + tcpConnectionCount.get());
                System.out.println("  Total bytes sent: " + tcpBytesSent.get());
            }
            
            if (config.server.udpEnabled && udpServer != null) {
                System.out.println("UDP Server:");
                System.out.println("  Packets received: " + udpServer.getPacketCount());
                System.out.println("  Total bytes received: " + udpServer.getTotalBytesReceived());
            }
            
            if (config.client.udpEnabled) {
                System.out.println("UDP Client:");
                System.out.println("  Packets sent: " + udpPacketCount.get());
                System.out.println("  Total bytes sent: " + udpBytesSent.get());
            }
        }
        
        if (config.fileTest != null && config.fileTest.enabled && fileStressTest != null) {
            if (config.networkTest != null && config.networkTest.enabled) {
                System.out.println();
            }
            fileStressTest.printStatistics();
        }
    }
}

