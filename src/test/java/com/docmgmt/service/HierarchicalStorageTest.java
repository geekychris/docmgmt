package com.docmgmt.service;

import com.docmgmt.model.Article;
import com.docmgmt.model.Content;
import com.docmgmt.model.FileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test hierarchical directory structure for file storage
 */
class HierarchicalStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void testStoragePathIsHierarchical() throws IOException {
        // Create a FileStore
        FileStore fileStore = FileStore.builder()
            .name("test-store")
            .rootPath(tempDir.toString())
            .status(FileStore.Status.ACTIVE)
            .build();
        fileStore.setId(1L);

        // Create a document
        Article doc = Article.builder()
            .name("Test Document")
            .build();
        doc.setId(1L);

        // Create content with file store
        Content content = Content.builder()
            .name("test-file.pdf")
            .contentType("application/pdf")
            .fileStore(fileStore)
            .sysObject(doc)
            .build();

        // Simulate what ContentService.generateStoragePath() would do
        String storagePath = generateHierarchicalPath("test-file.pdf");
        content.setStoragePath(storagePath);

        // Verify the path is hierarchical (format: aa/bb/cc/dd/uuid.ext)
        Pattern hierarchicalPattern = Pattern.compile("^[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf$");
        assertTrue(hierarchicalPattern.matcher(storagePath).matches(),
            "Storage path should be hierarchical: " + storagePath);

        // Write content
        byte[] testData = "Test content".getBytes();
        content.setContentBytes(testData);

        // Verify file was created in correct location
        Path filePath = Paths.get(fileStore.getFullPath(storagePath));
        assertTrue(Files.exists(filePath), "File should exist at: " + filePath);

        // Verify directory structure was created (4 levels deep)
        Path parent1 = filePath.getParent(); // dd directory
        Path parent2 = parent1.getParent();  // cc directory
        Path parent3 = parent2.getParent();  // bb directory
        Path parent4 = parent3.getParent();  // aa directory

        assertTrue(Files.isDirectory(parent1));
        assertTrue(Files.isDirectory(parent2));
        assertTrue(Files.isDirectory(parent3));
        assertTrue(Files.isDirectory(parent4));

        // Each directory name should be 2 characters
        assertEquals(2, parent1.getFileName().toString().length());
        assertEquals(2, parent2.getFileName().toString().length());
        assertEquals(2, parent3.getFileName().toString().length());
        assertEquals(2, parent4.getFileName().toString().length());

        // Verify content can be read back
        byte[] readData = content.getContentBytes();
        assertArrayEquals(testData, readData);
    }

    @Test
    void testCleanupRemovesEmptyDirectories() throws IOException {
        // Create a FileStore
        FileStore fileStore = FileStore.builder()
            .name("test-store")
            .rootPath(tempDir.toString())
            .status(FileStore.Status.ACTIVE)
            .build();
        fileStore.setId(1L);

        // Create a document
        Article doc = Article.builder()
            .name("Test Document")
            .build();
        doc.setId(1L);

        // Create content with file store
        Content content = Content.builder()
            .name("test-file.txt")
            .contentType("text/plain")
            .fileStore(fileStore)
            .sysObject(doc)
            .build();

        String storagePath = generateHierarchicalPath("test-file.txt");
        content.setStoragePath(storagePath);

        // Write content
        content.setContentBytes("Test content".getBytes());

        Path filePath = Paths.get(fileStore.getFullPath(storagePath));
        Path parent1 = filePath.getParent();
        Path parent2 = parent1.getParent();
        Path parent3 = parent2.getParent();
        Path parent4 = parent3.getParent();

        // Verify directories exist
        assertTrue(Files.exists(parent1));
        assertTrue(Files.exists(parent2));
        assertTrue(Files.exists(parent3));
        assertTrue(Files.exists(parent4));

        // Clean up storage
        content.cleanupStorage();

        // Verify file and all empty parent directories are removed
        assertFalse(Files.exists(filePath), "File should be deleted");
        assertFalse(Files.exists(parent1), "Empty directory level 1 should be removed");
        assertFalse(Files.exists(parent2), "Empty directory level 2 should be removed");
        assertFalse(Files.exists(parent3), "Empty directory level 3 should be removed");
        assertFalse(Files.exists(parent4), "Empty directory level 4 should be removed");
    }

    @Test
    void testCleanupStopsAtNonEmptyDirectory() throws IOException {
        // Create a FileStore
        FileStore fileStore = FileStore.builder()
            .name("test-store")
            .rootPath(tempDir.toString())
            .status(FileStore.Status.ACTIVE)
            .build();
        fileStore.setId(1L);

        // Create a document
        Article doc = Article.builder()
            .name("Test Document")
            .build();
        doc.setId(1L);

        // Create two content items that share some parent directories
        Content content1 = Content.builder()
            .name("test-file1.txt")
            .contentType("text/plain")
            .fileStore(fileStore)
            .sysObject(doc)
            .build();

        Content content2 = Content.builder()
            .name("test-file2.txt")
            .contentType("text/plain")
            .fileStore(fileStore)
            .sysObject(doc)
            .build();

        // Use paths that share the first 2 directory levels
        String path1 = "aa/bb/cc/dd/file1.txt";
        String path2 = "aa/bb/ee/ff/file2.txt";

        content1.setStoragePath(path1);
        content2.setStoragePath(path2);

        // Write both contents
        content1.setContentBytes("Content 1".getBytes());
        content2.setContentBytes("Content 2".getBytes());

        Path filePath1 = Paths.get(fileStore.getFullPath(path1));
        Path aaDir = filePath1.getParent().getParent().getParent().getParent();
        Path bbDir = filePath1.getParent().getParent().getParent();

        // Clean up content1
        content1.cleanupStorage();

        // Verify file1 is deleted but aa/bb directories remain (because file2 exists in aa/bb/ee/ff)
        assertFalse(Files.exists(filePath1), "File 1 should be deleted");
        assertTrue(Files.exists(aaDir), "Shared directory 'aa' should remain");
        assertTrue(Files.exists(bbDir), "Shared directory 'bb' should remain");

        // Verify file2 still exists
        Path filePath2 = Paths.get(fileStore.getFullPath(path2));
        assertTrue(Files.exists(filePath2), "File 2 should still exist");
    }

    /**
     * Simulates ContentService.generateStoragePath()
     */
    private String generateHierarchicalPath(String originalFilename) {
        String uuid = java.util.UUID.randomUUID().toString();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String uuidNoDashes = uuid.replace("-", "");
        String level1 = uuidNoDashes.substring(0, 2);
        String level2 = uuidNoDashes.substring(2, 4);
        String level3 = uuidNoDashes.substring(4, 6);
        String level4 = uuidNoDashes.substring(6, 8);

        return String.format("%s/%s/%s/%s/%s%s",
            level1, level2, level3, level4, uuid, extension);
    }
}
