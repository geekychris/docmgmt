package com.docmgmt;

import com.docmgmt.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Base test class for all tests in the document management system
 * Configures the test environment and provides common functionality
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public abstract class BaseTest {

    /**
     * Set up before each test
     * Can be overridden by subclasses
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Common setup for all tests
    }

    /**
     * Clean up after each test
     * Can be overridden by subclasses
     */
    @AfterEach
    public void tearDown() throws Exception {
        cleanupTestFiles();
    }

    /**
     * Clean up test files after tests
     * Removes any files created in the test temp directory
     */
    protected void cleanupTestFiles() throws IOException {
        Path testTempDir = Paths.get("./target/test-temp");
        if (Files.exists(testTempDir)) {
            try (Stream<Path> pathStream = Files.list(testTempDir)) {
                pathStream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Log error but continue
                        System.err.println("Failed to delete test file: " + path);
                    }
                });
            }
        }
    }
}

