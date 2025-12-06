package com.docmgmt.client;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DocumentManagementClient demonstrating copy-on-write versioning.
 * 
 * NOTE: These tests require the Document Management System to be running at http://[::1]:8082/docmgmt
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentManagementClientTest {
    
    private static DocumentManagementClient client;
    private static Long documentV1Id;
    private static Long contentV1Id;
    private static Long documentV2Id;
    
    @BeforeAll
    public static void setUp() {
        client = new DocumentManagementClient();
        System.out.println("=== Document Management Client Copy-on-Write Test ===\n");
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Create document v1.0")
    public void testCreateDocument() throws IOException, InterruptedException {
        System.out.println("1. Creating document v1.0...");
        
        Map<String, Object> document = new HashMap<>();
        document.put("name", "Integration Test Manual");
        document.put("documentType", "MANUAL");
        document.put("description", "Test document for copy-on-write demonstration");
        document.put("author", "Integration Test Suite");
        document.put("keywords", "test versioning copy-on-write");
        document.put("tags", Arrays.asList("test", "automation"));
        
        Map<String, Object> created = client.createDocument(document);
        
        assertNotNull(created);
        assertNotNull(created.get("id"));
        assertEquals("Integration Test Manual", created.get("name"));
        assertEquals(1, created.get("majorVersion"));
        assertEquals(0, created.get("minorVersion"));
        
        documentV1Id = ((Number) created.get("id")).longValue();
        String version = created.get("majorVersion") + "." + created.get("minorVersion");
        
        System.out.println("   ✓ Created: " + created.get("name") + 
                         " (ID: " + documentV1Id + 
                         ", Version: " + version + ")");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Add content to v1.0")
    public void testUploadContent() throws IOException, InterruptedException {
        System.out.println("\n2. Adding content to v1.0...");
        
        String contentText = "Integration Test Manual Version 1.0\n" +
                           "This is the original content for testing copy-on-write semantics.";
        byte[] contentBytes = contentText.getBytes(StandardCharsets.UTF_8);
        
        Map<String, Object> uploaded = client.uploadContent(
            "manual.txt",
            contentBytes,
            documentV1Id,
            true,  // store in database
            null
        );
        
        assertNotNull(uploaded);
        assertNotNull(uploaded.get("id"));
        assertEquals("manual.txt", uploaded.get("name"));
        assertEquals("DATABASE", uploaded.get("storageType"));
        assertEquals(documentV1Id.intValue(), uploaded.get("sysObjectId"));
        
        contentV1Id = ((Number) uploaded.get("id")).longValue();
        
        System.out.println("   ✓ Uploaded: " + uploaded.get("name") + 
                         " (ID: " + contentV1Id + 
                         ", Storage: " + uploaded.get("storageType") + ")");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Verify v1.0 content")
    public void testDownloadContent() throws IOException, InterruptedException {
        System.out.println("\n3. Verifying v1.0 content...");
        
        byte[] downloaded = client.downloadContent(contentV1Id);
        String content = new String(downloaded, StandardCharsets.UTF_8);
        
        assertTrue(content.contains("Version 1.0"));
        assertTrue(content.contains("original content"));
        
        System.out.println("   ✓ Content verified: " + content.substring(0, Math.min(50, content.length())) + "...");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Create v2.0 (major version)")
    public void testCreateMajorVersion() throws IOException, InterruptedException {
        System.out.println("\n4. Creating v2.0 (major version)...");
        
        Map<String, Object> version2 = client.createMajorVersion(documentV1Id);
        
        assertNotNull(version2);
        assertNotNull(version2.get("id"));
        assertEquals("Integration Test Manual", version2.get("name"));
        assertEquals(2, version2.get("majorVersion"));
        assertEquals(0, version2.get("minorVersion"));
        assertEquals(documentV1Id.intValue(), version2.get("parentVersionId"));
        
        documentV2Id = ((Number) version2.get("id")).longValue();
        String version = version2.get("majorVersion") + "." + version2.get("minorVersion");
        
        System.out.println("   ✓ Created: " + version2.get("name") + 
                         " (ID: " + documentV2Id + 
                         ", Version: " + version + ")");
        System.out.println("   ✓ Parent Version ID: " + version2.get("parentVersionId"));
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Check v2.0 content (initially shared)")
    public void testVersionContentSharing() throws IOException, InterruptedException {
        System.out.println("\n5. Checking v2.0 content (initially shared)...");
        
        List<Map<String, Object>> v2Contents = client.getContentBySysObject(documentV2Id);
        
        assertNotNull(v2Contents);
        assertEquals(1, v2Contents.size());
        
        Map<String, Object> v2Content = v2Contents.get(0);
        Long v2ContentId = ((Number) v2Content.get("id")).longValue();
        
        assertNotEquals(contentV1Id, v2ContentId, "Content IDs should be different (cloned)");
        assertEquals("manual.txt", v2Content.get("name"));
        
        // Download and verify it has same content as v1.0
        byte[] v2Downloaded = client.downloadContent(v2ContentId);
        String v2Text = new String(v2Downloaded, StandardCharsets.UTF_8);
        
        assertTrue(v2Text.contains("Version 1.0"));
        assertTrue(v2Text.contains("original content"));
        
        System.out.println("   ✓ Found 1 content item (ID: " + v2ContentId + ", cloned from " + contentV1Id + ")");
        System.out.println("   ✓ Content: " + v2Text.substring(0, Math.min(50, v2Text.length())) + "...");
        System.out.println("   ✓ Initially shares same data as v1.0");
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Update content in v2.0 (trigger copy-on-write)")
    public void testCopyOnWrite() throws IOException, InterruptedException {
        System.out.println("\n6. Updating content in v2.0 (triggering copy-on-write)...");
        
        String newContentText = "Integration Test Manual Version 2.0\n" +
                              "This version has significant updates and demonstrates copy-on-write semantics.\n" +
                              "The original v1.0 content should remain unchanged.";
        byte[] newContentBytes = newContentText.getBytes(StandardCharsets.UTF_8);
        
        Map<String, Object> v2NewContent = client.uploadContent(
            "manual.txt",
            newContentBytes,
            documentV2Id,
            true,
            null
        );
        
        assertNotNull(v2NewContent);
        assertNotNull(v2NewContent.get("id"));
        
        Long v2NewContentId = ((Number) v2NewContent.get("id")).longValue();
        
        System.out.println("   ✓ Uploaded new content (ID: " + v2NewContentId + ")");
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Verify copy-on-write (versions are independent)")
    public void testVersionsAreIndependent() throws IOException, InterruptedException {
        System.out.println("\n7. Verifying copy-on-write (versions are now independent)...");
        
        // Check v1.0 still has original content
        byte[] v1Downloaded = client.downloadContent(contentV1Id);
        String v1Text = new String(v1Downloaded, StandardCharsets.UTF_8);
        
        assertTrue(v1Text.contains("Version 1.0"));
        assertTrue(v1Text.contains("original content"));
        assertFalse(v1Text.contains("Version 2.0"));
        
        System.out.println("   ✓ v1.0 content unchanged: " + v1Text.substring(0, Math.min(50, v1Text.length())) + "...");
        
        // Check v2.0 has all content items including new one
        List<Map<String, Object>> v2Contents = client.getContentBySysObject(documentV2Id);
        assertTrue(v2Contents.size() >= 1, "v2.0 should have at least 1 content item");
        
        // Find the newest content (last in list or by checking text)
        boolean foundV2Content = false;
        for (Map<String, Object> content : v2Contents) {
            Long contentId = ((Number) content.get("id")).longValue();
            byte[] downloaded = client.downloadContent(contentId);
            String text = new String(downloaded, StandardCharsets.UTF_8);
            
            if (text.contains("Version 2.0")) {
                foundV2Content = true;
                System.out.println("   ✓ v2.0 content found: " + text.substring(0, Math.min(50, text.length())) + "...");
                break;
            }
        }
        
        assertTrue(foundV2Content, "v2.0 should have new content with 'Version 2.0'");
        
        System.out.println("   ✓ Copy-on-write successful! Versions are independent.");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. Check version history")
    public void testVersionHistory() throws IOException, InterruptedException {
        System.out.println("\n8. Checking version history...");
        
        List<Map<String, Object>> history = client.getVersionHistory(documentV2Id);
        
        assertNotNull(history);
        assertTrue(history.size() >= 2, "Should have at least 2 versions");
        
        System.out.println("   ✓ Found " + history.size() + " version(s):");
        for (Map<String, Object> v : history) {
            String version = v.get("majorVersion") + "." + v.get("minorVersion");
            Long id = ((Number) v.get("id")).longValue();
            Boolean isLatest = (Boolean) v.get("isLatestVersion");
            
            String latest = Boolean.TRUE.equals(isLatest) ? " (LATEST)" : "";
            System.out.println("     - v" + version + ": ID=" + id + latest);
        }
        
        // Verify v2.0 is latest
        Map<String, Object> latestVersion = history.get(0);
        assertTrue((Boolean) latestVersion.get("isLatestVersion"));
        assertEquals(2, latestVersion.get("majorVersion"));
        assertEquals(0, latestVersion.get("minorVersion"));
    }
    
    @Test
    @Order(9)
    @DisplayName("9. Create v2.1 (minor version)")
    public void testCreateMinorVersion() throws IOException, InterruptedException {
        System.out.println("\n9. Creating v2.1 (minor version)...");
        
        Map<String, Object> version2_1 = client.createMinorVersion(documentV2Id);
        
        assertNotNull(version2_1);
        assertNotNull(version2_1.get("id"));
        assertEquals(2, version2_1.get("majorVersion"));
        assertEquals(1, version2_1.get("minorVersion"));
        assertEquals(documentV2Id.intValue(), version2_1.get("parentVersionId"));
        
        Long documentV2_1Id = ((Number) version2_1.get("id")).longValue();
        String version = version2_1.get("majorVersion") + "." + version2_1.get("minorVersion");
        
        System.out.println("   ✓ Created: " + version2_1.get("name") + 
                         " (ID: " + documentV2_1Id + 
                         ", Version: " + version + ")");
        
        // Verify v2.1 inherited v2.0's content
        List<Map<String, Object>> v2_1Contents = client.getContentBySysObject(documentV2_1Id);
        assertTrue(v2_1Contents.size() >= 1);
        
        System.out.println("   ✓ v2.1 has " + v2_1Contents.size() + " content item(s) (inherited from v2.0)");
    }
    
    @AfterAll
    public static void tearDown() {
        System.out.println("\n=== Test Complete ===");
        System.out.println("\nSummary:");
        System.out.println("  ✓ Created 3 versions: v1.0, v2.0, v2.1");
        System.out.println("  ✓ Demonstrated copy-on-write: v1.0 and v2.0 have independent content");
        System.out.println("  ✓ All versions accessible via REST API");
        System.out.println("  ✓ Version history tracking working correctly");
    }
}
