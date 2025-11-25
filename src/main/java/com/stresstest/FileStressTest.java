package com.stresstest;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FileStressTest {
    private final Config.FileTestConfig config;
    private final ExecutorService executor;
    private final File testFolder;
    private final Random random = new Random();
    private final AtomicLong filesCreated = new AtomicLong(0);
    private final AtomicLong filesDeleted = new AtomicLong(0);
    private final AtomicLong filesRead = new AtomicLong(0);
    private final AtomicLong filesWritten = new AtomicLong(0);
    private final AtomicLong filesAppended = new AtomicLong(0);
    private final AtomicLong filesRenamed = new AtomicLong(0);
    private final AtomicLong dirsCreated = new AtomicLong(0);
    private final AtomicLong dirsDeleted = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong bytesRead = new AtomicLong(0);
    private volatile boolean running = false;

    public FileStressTest(Config.FileTestConfig config) {
        this.config = config;
        this.executor = Executors.newCachedThreadPool();
        this.testFolder = new File(config.testFolderPath);
    }

    public void start() {
        if (!config.enabled) {
            System.out.println("File Stress Test is disabled in config");
            return;
        }

        // Create test folder if it doesn't exist
        if (!testFolder.exists()) {
            if (!testFolder.mkdirs()) {
                System.err.println("Failed to create test folder: " + config.testFolderPath);
                return;
            }
            log("Created test folder: " + config.testFolderPath);
        }

        running = true;
        System.out.println("Starting File Stress Test...");
        System.out.println("  Test Folder: " + config.testFolderPath);
        System.out.println("  Threads: " + config.threadCount);
        System.out.println("  Iterations per thread: " + config.iterations);
        System.out.println("  File size range: " + config.minFileSize + " - " + config.maxFileSize + " bytes");
        System.out.println();

        for (int i = 0; i < config.threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> runTest(threadId));
        }
    }

    private void runTest(int threadId) {
        for (int i = 0; i < config.iterations && running; i++) {
            try {
                // Random operation selection
                double rand = random.nextDouble();
                
                if (rand < config.mkdirProbability) {
                    createSubDirectory(threadId);
                } else if (rand < config.mkdirProbability + config.rmdirProbability) {
                    deleteRandomDirectory(threadId);
                } else if (rand < config.mkdirProbability + config.rmdirProbability + config.deleteProbability) {
                    deleteRandomFile(threadId);
                } else {
                    // File operations (create, read, write, append, rename)
                    performFileOperation(threadId);
                }

                if (config.delayBetweenOperations > 0) {
                    Thread.sleep(config.delayBetweenOperations);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log("Thread " + threadId + " error: " + e.getMessage());
            }
        }
    }

    private void performFileOperation(int threadId) throws IOException {
        int operation = random.nextInt(5);
        
        switch (operation) {
            case 0:
                createRandomFile(threadId);
                break;
            case 1:
                readRandomFile(threadId);
                break;
            case 2:
                writeRandomFile(threadId);
                break;
            case 3:
                appendRandomFile(threadId);
                break;
            case 4:
                renameRandomFile(threadId);
                break;
        }
    }

    private File getRandomSubDirectory() {
        int depth = random.nextInt(config.maxSubDirDepth) + 1;
        File current = testFolder;
        
        for (int i = 0; i < depth; i++) {
            File[] subDirs = current.listFiles(File::isDirectory);
            if (subDirs != null && subDirs.length > 0 && random.nextBoolean()) {
                current = subDirs[random.nextInt(subDirs.length)];
            } else {
                break;
            }
        }
        
        return current;
    }

    private void createSubDirectory(int threadId) {
        try {
            File parentDir = getRandomSubDirectory();
            String dirName = "dir_" + System.currentTimeMillis() + "_" + threadId + "_" + random.nextInt(10000);
            File newDir = new File(parentDir, dirName);
            
            if (newDir.mkdirs()) {
                dirsCreated.incrementAndGet();
                log("Thread " + threadId + ": Created directory " + newDir.getAbsolutePath());
            }
        } catch (Exception e) {
            log("Thread " + threadId + ": Failed to create directory - " + e.getMessage());
        }
    }

    private void createRandomFile(int threadId) throws IOException {
        File parentDir = getRandomSubDirectory();
        String fileName = "file_" + System.currentTimeMillis() + "_" + threadId + "_" + random.nextInt(10000) + ".dat";
        File file = new File(parentDir, fileName);
        
        int fileSize = config.minFileSize + random.nextInt(config.maxFileSize - config.minFileSize + 1);
        byte[] data = new byte[fileSize];
        random.nextBytes(data);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            bytesWritten.addAndGet(fileSize);
            filesCreated.incrementAndGet();
            log("Thread " + threadId + ": Created file " + file.getName() + " (" + fileSize + " bytes)");
        }
    }

    private void readRandomFile(int threadId) throws IOException {
        File file = getRandomFile();
        if (file == null) return;
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                totalRead += bytesRead;
            }
            this.bytesRead.addAndGet(totalRead);
            filesRead.incrementAndGet();
            log("Thread " + threadId + ": Read file " + file.getName() + " (" + totalRead + " bytes)");
        } catch (FileNotFoundException e) {
            // File might have been deleted by another thread
        }
    }

    private void writeRandomFile(int threadId) throws IOException {
        File file = getRandomFile();
        if (file == null) {
            createRandomFile(threadId);
            return;
        }
        
        int fileSize = config.minFileSize + random.nextInt(config.maxFileSize - config.minFileSize + 1);
        byte[] data = new byte[fileSize];
        random.nextBytes(data);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            bytesWritten.addAndGet(fileSize);
            filesWritten.incrementAndGet();
            log("Thread " + threadId + ": Wrote to file " + file.getName() + " (" + fileSize + " bytes)");
        } catch (FileNotFoundException e) {
            // File might have been deleted by another thread
        }
    }

    private void appendRandomFile(int threadId) throws IOException {
        File file = getRandomFile();
        if (file == null) {
            createRandomFile(threadId);
            return;
        }
        
        int appendSize = random.nextInt(config.maxFileSize / 2) + 1;
        byte[] data = new byte[appendSize];
        random.nextBytes(data);
        
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(data);
            bytesWritten.addAndGet(appendSize);
            filesAppended.incrementAndGet();
            log("Thread " + threadId + ": Appended to file " + file.getName() + " (" + appendSize + " bytes)");
        } catch (FileNotFoundException e) {
            // File might have been deleted by another thread
        }
    }

    private void renameRandomFile(int threadId) {
        File file = getRandomFile();
        if (file == null) return;
        
        String newName = "renamed_" + System.currentTimeMillis() + "_" + threadId + "_" + random.nextInt(10000) + ".dat";
        File newFile = new File(file.getParent(), newName);
        
        if (file.renameTo(newFile)) {
            filesRenamed.incrementAndGet();
            log("Thread " + threadId + ": Renamed file " + file.getName() + " -> " + newFile.getName());
        }
    }

    private void deleteRandomFile(int threadId) {
        File file = getRandomFile();
        if (file == null) return;
        
        if (file.delete()) {
            filesDeleted.incrementAndGet();
            log("Thread " + threadId + ": Deleted file " + file.getName());
        }
    }

    private void deleteRandomDirectory(int threadId) {
        File dir = getRandomDirectory();
        if (dir == null || dir.equals(testFolder)) return; // Don't delete root test folder
        
        try {
            deleteDirectory(dir);
            dirsDeleted.incrementAndGet();
            log("Thread " + threadId + ": Deleted directory " + dir.getName());
        } catch (IOException e) {
            log("Thread " + threadId + ": Failed to delete directory " + dir.getName() + " - " + e.getMessage());
        }
    }

    private File getRandomFile() {
        List<File> files = new ArrayList<>();
        collectFiles(testFolder, files);
        
        if (files.isEmpty()) {
            return null;
        }
        
        return files.get(random.nextInt(files.size()));
    }

    private File getRandomDirectory() {
        List<File> dirs = new ArrayList<>();
        collectDirectories(testFolder, dirs);
        
        if (dirs.isEmpty()) {
            return null;
        }
        
        return dirs.get(random.nextInt(dirs.size()));
    }

    private void collectFiles(File dir, List<File> files) {
        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File f : fileArray) {
                if (f.isFile()) {
                    files.add(f);
                } else if (f.isDirectory()) {
                    collectFiles(f, files);
                }
            }
        }
    }

    private void collectDirectories(File dir, List<File> dirs) {
        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File f : fileArray) {
                if (f.isDirectory()) {
                    dirs.add(f);
                    collectDirectories(f, dirs);
                }
            }
        }
    }

    private void deleteDirectory(File dir) throws IOException {
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Clean up test folder
        cleanup();
    }

    private void cleanup() {
        log("Cleaning up test folder...");
        try {
            File[] files = testFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            log("Test folder cleaned up successfully");
        } catch (IOException e) {
            System.err.println("Error cleaning up test folder: " + e.getMessage());
        }
    }

    public void printStatistics() {
        System.out.println("File Stress Test:");
        System.out.println("  Files created: " + filesCreated.get());
        System.out.println("  Files read: " + filesRead.get());
        System.out.println("  Files written: " + filesWritten.get());
        System.out.println("  Files appended: " + filesAppended.get());
        System.out.println("  Files renamed: " + filesRenamed.get());
        System.out.println("  Files deleted: " + filesDeleted.get());
        System.out.println("  Directories created: " + dirsCreated.get());
        System.out.println("  Directories deleted: " + dirsDeleted.get());
        System.out.println("  Total bytes written: " + bytesWritten.get());
        System.out.println("  Total bytes read: " + bytesRead.get());
    }

    private void log(String message) {
        System.out.println("[FILE TEST] " + message);
    }
}

