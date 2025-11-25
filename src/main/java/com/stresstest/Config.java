package com.stresstest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;

public class Config {
    public ServerConfig server;
    public ClientConfig client;
    public TestConfig networkTest;
    public FileTestConfig fileTest;

    public static class ServerConfig {
        public boolean tcpEnabled;
        public PortRange tcpPortRange;
        public boolean udpEnabled;
        public PortRange udpPortRange;
    }

    public static class ClientConfig {
        public boolean tcpEnabled;
        public int tcpThreadCount;
        public PortRange tcpPortRange;
        public boolean udpEnabled;
        public int udpThreadCount;
        public PortRange udpPortRange;
        public String serverHost;
    }

    public static class TestConfig {
        public boolean enabled;
        public int dataSize;
        public int iterations;
        public long delayBetweenIterations;
        public long delayBetweenConnections;
    }

    public static class PortRange {
        public int start;
        public int end;
    }

    public static class FileTestConfig {
        public boolean enabled;
        public String testFolderPath;
        public int threadCount;
        public int iterations;
        public int minFileSize;
        public int maxFileSize;
        public int maxSubDirDepth;
        public long delayBetweenOperations;
        public double deleteProbability;  // 0.0 ~ 1.0, 파일/디렉토리 삭제 확률
        public double mkdirProbability;    // 0.0 ~ 1.0, 디렉토리 생성 확률
        public double rmdirProbability;    // 0.0 ~ 1.0, 디렉토리 삭제 확률
    }

    public static Config load(String configPath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileReader reader = new FileReader(configPath)) {
            return gson.fromJson(reader, Config.class);
        }
    }
}

