package com.docmgmt;

import com.docmgmt.controller.ContentControllerIntegrationTest;
import com.docmgmt.controller.DocumentControllerIntegrationTest;
import com.docmgmt.controller.FileStoreControllerIntegrationTest;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.repository.DocumentRepository;
import com.docmgmt.repository.FileStoreRepository;
import com.docmgmt.service.ContentServiceTest;
import com.docmgmt.service.DocumentServiceTest;
import com.docmgmt.service.FileStoreServiceTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;

/**
 * Test suite that organizes and runs all tests for the Document Management System
 * Controls test execution order and handles setup/cleanup between test classes
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class DocumentManagementTestSuite {

    private static final Logger logger = LoggerFactory.getLogger(DocumentManagementTestSuite.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private FileStoreRepository fileStoreRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Value("${docmgmt.file-storage.temp-dir:./target/test-temp}")
    private String tempUploadDir;

    /**
     * Set up test environment before any tests run
     */
    @BeforeAll
    void setUp() throws IOException {
        logger.info("Setting up test environment");
        
        // Clean up any existing test data
        cleanupAll();
        
        // Create necessary directories
        Path tempDir = Paths.get(tempUploadDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        logger.info("Test environment setup complete");
    }

    /**
     * Clean up after all tests have run
     */
    @AfterAll
    void tearDown() {
        logger.info("Cleaning up test environment");
        cleanupAll();
        logger.info("Test environment cleanup complete");
    }

    /**
     * Clean up all test resources
     */
    private void cleanupAll() {
        // Clean database
        logger.info("Cleaning database");
        contentRepository.deleteAll();
        documentRepository.deleteAll();
        fileStoreRepository.deleteAll();
        
        // Clear caches if any
        clearCaches();
        
        // Clean file storage
        cleanFileStorage();
    }

    /**
     * Clear all Spring caches
     */
    private void clearCaches() {
        if (cacheManager != null) {
            logger.info("Clearing caches");
            Collection<String> cacheNames = cacheManager.getCacheNames();
            cacheNames.forEach(cacheName -> {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
                logger.debug("Cleared cache: {}", cacheName);
            });
        }
    }

    /**
     * Clean file storage directory
     */
    private void cleanFileStorage() {
        logger.info("Cleaning file storage");
        Path tempDir = Paths.get(tempUploadDir);
        if (Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                    .filter(path -> !path.equals(tempDir))
                    .sorted((a, b) -> b.toString().length() - a.toString().length())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete path: {}", path, e);
                        }
                    });
            } catch (IOException e) {
                logger.warn("Error cleaning file storage: {}", e.getMessage());
            }
        }
    }

    // ----- SERVICE TESTS -----

    /**
     * Group service layer tests
     * These run first to validate core business logic
     */
    @Nested
    @Order(1)
    class ServiceTests {
        
        @Nested
        @Order(1)
        class FileStoreServiceTests extends FileStoreServiceTest {}
        
        @Nested
        @Order(2)
        class ContentServiceTests extends ContentServiceTest {}
        
        @Nested
        @Order(3)
        class DocumentServiceTests extends DocumentServiceTest {}
    }

    // ----- CONTROLLER TESTS -----

    /**
     * Group controller layer tests
     * These run after service tests are validated
     */
    @Nested
    @Order(2)
    class ControllerTests {
        
        @Nested
        @Order(1)
        class FileStoreControllerTests extends FileStoreControllerIntegrationTest {}
        
        @Nested
        @Order(2)
        class ContentControllerTests extends ContentControllerIntegrationTest {}
        
        @Nested
        @Order(3)
        class DocumentControllerTests extends DocumentControllerIntegrationTest {}
    }
}

