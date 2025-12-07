package com.docmgmt.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Content rendition functionality
 */
public class ContentUnitTest {
    
    private Content primaryContent;
    private Report testDocument;
    
    @BeforeEach
    void setUp() {
        testDocument = Report.builder()
                .name("Test Document")
                .build();
        
        primaryContent = Content.builder()
                .name("primary.pdf")
                .contentType("application/pdf")
                .content("PDF content".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDocument)
                .isPrimary(true)
                .isIndexable(false)
                .build();
    }
    
    @Test
    void testDefaultPrimaryFlag() {
        Content content = Content.builder()
                .name("test.txt")
                .content("test".getBytes())
                .build();
        
        assertTrue(content.isPrimary());
    }
    
    @Test
    void testDefaultIndexableFlag() {
        Content content = Content.builder()
                .name("test.txt")
                .content("test".getBytes())
                .build();
        
        assertFalse(content.isIndexable());
    }
    
    @Test
    void testPrimaryIsNotSecondaryRendition() {
        assertTrue(primaryContent.isPrimary());
        assertFalse(primaryContent.isSecondaryRendition());
    }
    
    @Test
    void testAddSecondaryRendition() {
        Content secondary = Content.builder()
                .name("secondary.txt")
                .contentType("text/plain")
                .content("Extracted text".getBytes(StandardCharsets.UTF_8))
                .build();
        
        primaryContent.addSecondaryRendition(secondary);
        
        assertEquals(1, primaryContent.getSecondaryRenditions().size());
        assertFalse(secondary.isPrimary());
        assertEquals(primaryContent, secondary.getParentRendition());
        assertTrue(secondary.isSecondaryRendition());
    }
    
    @Test
    void testAddMultipleSecondaryRenditions() {
        Content secondary1 = Content.builder().name("secondary1.txt").build();
        Content secondary2 = Content.builder().name("secondary2.txt").build();
        Content secondary3 = Content.builder().name("secondary3.txt").build();
        
        primaryContent.addSecondaryRendition(secondary1);
        primaryContent.addSecondaryRendition(secondary2);
        primaryContent.addSecondaryRendition(secondary3);
        
        assertEquals(3, primaryContent.getSecondaryRenditions().size());
        assertTrue(primaryContent.getSecondaryRenditions().contains(secondary1));
        assertTrue(primaryContent.getSecondaryRenditions().contains(secondary2));
        assertTrue(primaryContent.getSecondaryRenditions().contains(secondary3));
    }
    
    @Test
    void testRemoveSecondaryRendition() {
        Content secondary = Content.builder().name("secondary.txt").build();
        
        primaryContent.addSecondaryRendition(secondary);
        assertEquals(1, primaryContent.getSecondaryRenditions().size());
        
        primaryContent.removeSecondaryRendition(secondary);
        assertEquals(0, primaryContent.getSecondaryRenditions().size());
        assertNull(secondary.getParentRendition());
    }
    
    @Test
    void testRemoveAllSecondaryRenditions() {
        Content secondary1 = Content.builder().name("secondary1.txt").build();
        Content secondary2 = Content.builder().name("secondary2.txt").build();
        
        primaryContent.addSecondaryRendition(secondary1);
        primaryContent.addSecondaryRendition(secondary2);
        assertEquals(2, primaryContent.getSecondaryRenditions().size());
        
        primaryContent.removeAllSecondaryRenditions();
        assertEquals(0, primaryContent.getSecondaryRenditions().size());
        assertNull(secondary1.getParentRendition());
        assertNull(secondary2.getParentRendition());
    }
    
    @Test
    void testAddNullSecondaryRendition() {
        primaryContent.addSecondaryRendition(null);
        assertEquals(0, primaryContent.getSecondaryRenditions().size());
    }
    
    @Test
    void testRemoveNullSecondaryRendition() {
        assertDoesNotThrow(() -> primaryContent.removeSecondaryRendition(null));
    }
    
    @Test
    void testSecondaryRenditionFlagAutoSet() {
        Content secondary = Content.builder()
                .name("secondary.txt")
                .isPrimary(true)  // Start as primary
                .build();
        
        assertTrue(secondary.isPrimary());
        
        primaryContent.addSecondaryRendition(secondary);
        
        // Should be changed to non-primary
        assertFalse(secondary.isPrimary());
        assertTrue(secondary.isSecondaryRendition());
    }
    
    @Test
    void testClonePreservesRenditionFlags() {
        Content original = Content.builder()
                .name("original.pdf")
                .contentType("application/pdf")
                .isPrimary(false)
                .isIndexable(true)
                .build();
        
        Content clone = original.createClone();
        
        assertEquals(original.isPrimary(), clone.isPrimary());
        assertEquals(original.isIndexable(), clone.isIndexable());
        assertNull(clone.getParentRendition());
        assertEquals(0, clone.getSecondaryRenditions().size());
    }
    
    @Test
    void testIndexableFlagIndependent() {
        Content indexable = Content.builder()
                .name("indexable.txt")
                .isPrimary(true)
                .isIndexable(true)
                .build();
        
        Content nonIndexable = Content.builder()
                .name("non-indexable.jpg")
                .isPrimary(true)
                .isIndexable(false)
                .build();
        
        assertTrue(indexable.isIndexable());
        assertFalse(nonIndexable.isIndexable());
        
        // Both can be primary
        assertTrue(indexable.isPrimary());
        assertTrue(nonIndexable.isPrimary());
    }
}
