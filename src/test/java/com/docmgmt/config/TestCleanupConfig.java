package com.docmgmt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Configuration for cleaning up test resources
 * Handles deletion of temporary files and directories created during tests
 */
@Configuration
@Profile({"test", "ci"})
public class TestCleanupConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestCleanupConfig.class);
    
    @Value("${docmgmt.file-storage.temp-dir:./target/test-temp}")
    private String tempUploadDir;
    
    /**
     * Clean up resources when test context is closed
     * @param event The context closed event
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        cleanupTemporaryFiles();
    }
    
    /**
     * Clean up resources when bean is destroyed
     */
    @PreDestroy
    public void cleanupTemporaryFiles() {
        Path tempDir = Paths.get(tempUploadDir);
        
        if (Files.exists(tempDir)) {
            try {
                logger.info("Deleting temporary test files from: {}", tempDir);
                deleteDirectory(tempDir);
                logger.info("Temporary test files deleted successfully");
            } catch (IOException e) {
                logger.warn("Error cleaning up temporary test files: {}", e.getMessage());
            }
        }
        
        // Clean up any other test files in target/test-temp
        cleanupTargetTestTemp();
    }
    
    /**
     * Delete a directory and all its contents
     * @param directory Directory to delete
     */
    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
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
    
    /**
     * Clean up any other test files in target/test-temp
     */
    private void cleanupTargetTestTemp() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("file:target/test-temp/**");
            
            for (Resource resource : resources) {
                try {
                    Path path = resource.getFile().toPath();
                    if (Files.isDirectory(path)) {
                        deleteDirectory(path);
                    } else {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to delete resource: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            logger.warn("Error cleaning up target/test-temp directory", e);
        }
    }
}

