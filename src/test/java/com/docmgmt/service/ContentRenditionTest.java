package com.docmgmt.service;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.Report;
import com.docmgmt.model.SysObject;
import com.docmgmt.transformer.TransformationException;
import com.docmgmt.transformer.TransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ContentRenditionTest {
    
    @Autowired
    private ContentService contentService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private TransformerRegistry transformerRegistry;
    
    private Document testDocument;
    private Content primaryContent;
    
    @BeforeEach
    void setUp() {
        // Create a test document - use Report
        testDocument = Report.builder()
                .name("Test Document")
                .build();
        testDocument = documentService.save(testDocument);
        
        // Create primary content
        primaryContent = Content.builder()
                .name("primary.txt")
                .contentType("text/plain")
                .content("Primary content data".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDocument)
                .isPrimary(true)
                .isIndexable(true)
                .build();
        primaryContent = contentService.save(primaryContent);
    }
    
    @Test
    void testContentIsDefaultPrimary() {
        assertTrue(primaryContent.isPrimary());
        assertFalse(primaryContent.isSecondaryRendition());
    }
    
    @Test
    void testAddSecondaryRendition() {
        // Add a secondary rendition
        byte[] secondaryBytes = "Secondary rendition".getBytes(StandardCharsets.UTF_8);
        Content secondary = contentService.addRendition(
                primaryContent.getId(),
                "secondary.txt",
                secondaryBytes,
                "text/plain",
                false
        );
        
        assertNotNull(secondary);
        assertNotNull(secondary.getId());
        assertFalse(secondary.isPrimary());
        assertTrue(secondary.isSecondaryRendition());
        assertEquals(primaryContent.getId(), secondary.getParentRendition().getId());
        assertFalse(secondary.isIndexable());
    }
    
    @Test
    void testGetAllRenditions() {
        // Add multiple secondary renditions
        contentService.addRendition(
                primaryContent.getId(),
                "secondary1.txt",
                "Secondary 1".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        contentService.addRendition(
                primaryContent.getId(),
                "secondary2.txt",
                "Secondary 2".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        List<Content> renditions = contentService.getAllRenditions(primaryContent.getId());
        
        assertEquals(3, renditions.size()); // 1 primary + 2 secondary
        assertTrue(renditions.get(0).isPrimary());
        assertFalse(renditions.get(1).isPrimary());
        assertFalse(renditions.get(2).isPrimary());
    }
    
    @Test
    void testRemoveSecondaryRenditions() {
        // Add secondary renditions
        contentService.addRendition(
                primaryContent.getId(),
                "secondary1.txt",
                "Secondary 1".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        contentService.addRendition(
                primaryContent.getId(),
                "secondary2.txt",
                "Secondary 2".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        assertEquals(3, contentService.getAllRenditions(primaryContent.getId()).size());
        
        // Remove all secondary renditions
        contentService.removeSecondaryRenditions(primaryContent.getId());
        
        List<Content> renditionsAfter = contentService.getAllRenditions(primaryContent.getId());
        assertEquals(1, renditionsAfter.size());
        assertTrue(renditionsAfter.get(0).isPrimary());
    }
    
    @Test
    void testUpdatePrimaryContentRemovesSecondaryRenditions() throws IOException {
        // Add secondary renditions
        contentService.addRendition(
                primaryContent.getId(),
                "secondary.txt",
                "Secondary".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        assertEquals(2, contentService.getAllRenditions(primaryContent.getId()).size());
        
        // Update primary content
        byte[] newBytes = "Updated primary content".getBytes(StandardCharsets.UTF_8);
        contentService.updatePrimaryContent(primaryContent.getId(), newBytes);
        
        // Secondary renditions should be removed
        List<Content> renditionsAfter = contentService.getAllRenditions(primaryContent.getId());
        assertEquals(1, renditionsAfter.size());
        assertTrue(renditionsAfter.get(0).isPrimary());
    }
    
    @Test
    void testCannotAddRenditionToSecondary() {
        // Add a secondary rendition
        Content secondary = contentService.addRendition(
                primaryContent.getId(),
                "secondary.txt",
                "Secondary".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        // Try to add rendition to the secondary (should fail)
        assertThrows(IllegalArgumentException.class, () -> {
            contentService.addRendition(
                    secondary.getId(),
                    "tertiary.txt",
                    "Tertiary".getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    false
            );
        });
    }
    
    @Test
    void testGetIndexableContent() {
        // Create multiple content items with different indexable flags
        Content indexable1 = Content.builder()
                .name("indexable1.txt")
                .contentType("text/plain")
                .content("Indexable 1".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDocument)
                .isPrimary(true)
                .isIndexable(true)
                .build();
        contentService.save(indexable1);
        
        Content nonIndexable = Content.builder()
                .name("image.jpg")
                .contentType("image/jpeg")
                .content(new byte[100])
                .sysObject(testDocument)
                .isPrimary(true)
                .isIndexable(false)
                .build();
        contentService.save(nonIndexable);
        
        List<Content> indexableContent = contentService.getIndexableContent(testDocument);
        
        // Should include primaryContent and indexable1, but not nonIndexable
        assertEquals(2, indexableContent.size());
        assertTrue(indexableContent.stream().allMatch(Content::isIndexable));
    }
    
    @Test
    void testCascadeDeleteSecondaryRenditions() {
        // Add secondary renditions
        Content secondary1 = contentService.addRendition(
                primaryContent.getId(),
                "secondary1.txt",
                "Secondary 1".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        Content secondary2 = contentService.addRendition(
                primaryContent.getId(),
                "secondary2.txt",
                "Secondary 2".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                false
        );
        
        Long secondary1Id = secondary1.getId();
        Long secondary2Id = secondary2.getId();
        
        // Delete primary content
        contentService.delete(primaryContent.getId());
        
        // Secondary renditions should also be deleted (cascade)
        assertThrows(Exception.class, () -> contentService.findById(secondary1Id));
        assertThrows(Exception.class, () -> contentService.findById(secondary2Id));
    }
    
    @Test
    void testTransformerRegistryIsConfigured() {
        // Verify that at least one transformer is registered (PdfToTextTransformer)
        assertTrue(transformerRegistry.getTransformerCount() > 0);
    }
}
