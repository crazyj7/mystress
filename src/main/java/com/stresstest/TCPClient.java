package com.stresstest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TCPClient implements Runnable {
    private final Config.ClientConfig config;
    private final Config.TestConfig testConfig;
    private final AtomicLong connectionCount;
    private final AtomicLong totalBytesSent;
    private final Random random = new Random();

    public TCPClient(Config.ClientConfig config, Config.TestConfig testConfig,
                     AtomicLong connectionCount, AtomicLong totalBytesSent) {
        this.config = config;
        this.testConfig = testConfig;
        this.connectionCount = connectionCount;
        this.totalBytesSent = totalBytesSent;
    }

    @Override
    public void run() {
        byte[] data = new byte[testConfig.dataSize];
        random.nextBytes(data);

        for (int i = 0; i < testConfig.iterations; i++) {
            int port = config.tcpPortRange.start + 
                      random.nextInt(config.tcpPortRange.end - config.tcpPortRange.start + 1);
            
            // Retry connection on failure with 1 second delay
            boolean connected = false;
            while (!connected) {
                try (Socket socket = new Socket(config.serverHost, port)) {
                    connected = true;
                    connectionCount.incrementAndGet();
                    
                    InputStream input = socket.getInputStream();
                    OutputStream output = socket.getOutputStream();
                    
                    // Send data
                    output.write(data);
                    output.flush();
                    
                    // Receive echo
                    byte[] buffer = new byte[testConfig.dataSize];
                    int bytesRead = input.read(buffer);
                    if (bytesRead > 0) {
                        totalBytesSent.addAndGet(bytesRead);
                    }
                    
                    socket.close();
                    
                    if (testConfig.delayBetweenConnections > 0) {
                        Thread.sleep(testConfig.delayBetweenConnections);
                    }
                } catch (IOException e) {
                    // Connection failed - retry after 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
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
    }
}

