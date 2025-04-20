package com.docmgmt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Configuration for file storage
 * Manages temporary upload directories and cleanup
 */
@Configuration
@EnableScheduling
public class FileStorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfig.class);

    @Value("${docmgmt.file-storage.temp-dir:./temp-uploads}")
    private String tempUploadDir;

    @Value("${docmgmt.file-storage.temp-file-ttl:24}")
    private int tempFileTtlHours;

    /**
     * Initialize temporary upload directory on startup
     * @return Path to the temporary upload directory
     * @throws IOException if directory creation fails
     */
    @Bean
    public Path tempUploadPath() throws IOException {
        Path path = Paths.get(tempUploadDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created temporary upload directory: {}", path.toAbsolutePath());
        }
        return path;
    }

    /**
     * Scheduled task to clean up temporary files
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTempFiles() {
        try {
            Path path = tempUploadPath();
            Instant cutoff = Instant.now().minus(Duration.ofHours(tempFileTtlHours));
            
            try (Stream<Path> pathStream = Files.list(path)) {
                pathStream.forEach(filePath -> {
                    try {
                        if (Files.isRegularFile(filePath)) {
                            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
                            if (lastModified.isBefore(cutoff)) {
                                Files.delete(filePath);
                                logger.debug("Deleted temporary file: {}", filePath);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error checking or deleting temporary file: {}", filePath, e);
                    }
                });
            }
            
            logger.info("Completed temporary file cleanup");
        } catch (IOException e) {
            logger.error("Error during temporary file cleanup", e);
        }
    }

    /**
     * Get temporary file path for a given filename
     * @param filename The original filename
     * @return Path for the temporary file
     */
    public Path getTempFilePath(String filename) throws IOException {
        // Generate unique file name to avoid collisions
        String uniqueFileName = System.currentTimeMillis() + "_" + filename;
        return tempUploadPath().resolve(uniqueFileName);
    }

    /**
     * Get temporary file by creating it with unique name
     * @param filename The original filename
     * @return The temporary file
     */
    public File createTempFile(String filename) throws IOException {
        Path path = getTempFilePath(filename);
        return path.toFile();
    }
}

