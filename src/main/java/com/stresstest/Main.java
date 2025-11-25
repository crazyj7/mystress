package com.stresstest;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String configPath = "config.json";
        String peerIP = null;
        
        // Parse command line arguments
        // Format: java -jar mystress.jar [config.json] [peerIP]
        // or: java -jar mystress.jar [peerIP]
        if (args.length > 0) {
            // Check if first argument is an IP address or config file
            if (args[0].contains(".") || args[0].contains(":") || args[0].equals("localhost")) {
                // Looks like an IP address
                peerIP = args[0];
            } else {
                // Assume it's a config file path
                configPath = args[0];
                if (args.length > 1) {
                    peerIP = args[1];
                }
            }
        }

        try {
            Config config = Config.load(configPath);
            
            // Override serverHost if peerIP is provided
            if (peerIP != null && !peerIP.isEmpty()) {
                config.client.serverHost = peerIP;
                System.out.println("Peer IP override: " + peerIP);
            }
            
            StressTest stressTest = new StressTest(config);
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stressTest.stop();
            }));
            
            stressTest.start();
            
            // Keep main thread alive
            Thread.currentThread().join();
        } catch (IOException e) {
            System.err.println("Error loading config file: " + e.getMessage());
            System.err.println("Usage: java -jar mystress.jar [config.json] [peerIP]");
            System.err.println("   or: java -jar mystress.jar [peerIP]");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

