package com.docmgmt.transformer;

import com.docmgmt.model.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransformerRegistry
 */
public class TransformerRegistryUnitTest {
    
    private TransformerRegistry registry;
    private MockTransformer pdfTransformer;
    private MockTransformer wordTransformer;
    
    @BeforeEach
    void setUp() {
        pdfTransformer = new MockTransformer("application/pdf", "text/plain", "PDF to Text");
        wordTransformer = new MockTransformer("application/msword", "text/plain", "Word to Text");
        
        registry = new TransformerRegistry(List.of(pdfTransformer, wordTransformer));
    }
    
    @Test
    void testRegistryInitializedWithTransformers() {
        assertEquals(2, registry.getTransformerCount());
    }
    
    @Test
    void testGetAllTransformers() {
        List<ContentTransformer> transformers = registry.getAllTransformers();
        
        assertEquals(2, transformers.size());
        assertTrue(transformers.contains(pdfTransformer));
        assertTrue(transformers.contains(wordTransformer));
    }
    
    @Test
    void testFindTransformerByContent() {
        Content pdfContent = Content.builder()
                .name("test.pdf")
                .contentType("application/pdf")
                .build();
        
        var result = registry.findTransformer(pdfContent);
        
        assertTrue(result.isPresent());
        assertEquals(pdfTransformer, result.get());
    }
    
    @Test
    void testFindTransformerByContentTypes() {
        var result = registry.findTransformer("application/pdf", "text/plain");
        
        assertTrue(result.isPresent());
        assertEquals(pdfTransformer, result.get());
    }
    
    @Test
    void testFindTransformerNotFound() {
        Content imageContent = Content.builder()
                .name("test.jpg")
                .contentType("image/jpeg")
                .build();
        
        var result = registry.findTransformer(imageContent);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testRegisterTransformer() {
        MockTransformer newTransformer = new MockTransformer("text/html", "text/plain", "HTML to Text");
        
        assertEquals(2, registry.getTransformerCount());
        
        registry.registerTransformer(newTransformer);
        
        assertEquals(3, registry.getTransformerCount());
        assertTrue(registry.getAllTransformers().contains(newTransformer));
    }
    
    @Test
    void testRegisterDuplicateTransformer() {
        registry.registerTransformer(pdfTransformer);
        
        // Should still be 2 (no duplicate)
        assertEquals(2, registry.getTransformerCount());
    }
    
    @Test
    void testRegisterNullTransformer() {
        registry.registerTransformer(null);
        
        assertEquals(2, registry.getTransformerCount());
    }
    
    @Test
    void testUnregisterTransformer() {
        registry.unregisterTransformer(pdfTransformer);
        
        assertEquals(1, registry.getTransformerCount());
        assertFalse(registry.getAllTransformers().contains(pdfTransformer));
    }
    
    @Test
    void testEmptyRegistry() {
        TransformerRegistry emptyRegistry = new TransformerRegistry(null);
        
        assertEquals(0, emptyRegistry.getTransformerCount());
        assertTrue(emptyRegistry.getAllTransformers().isEmpty());
    }
    
    @Test
    void testFindFirstMatchingTransformer() {
        // Add another PDF transformer
        MockTransformer anotherPdfTransformer = new MockTransformer("application/pdf", "text/html", "PDF to HTML");
        registry.registerTransformer(anotherPdfTransformer);
        
        Content pdfContent = Content.builder()
                .name("test.pdf")
                .contentType("application/pdf")
                .build();
        
        // Should find the first one
        var result = registry.findTransformer(pdfContent);
        assertTrue(result.isPresent());
    }
    
    /**
     * Mock transformer for testing
     */
    private static class MockTransformer extends AbstractContentTransformer {
        
        public MockTransformer(String sourceType, String targetType, String name) {
            super(sourceType, targetType, name);
        }
        
        @Override
        public byte[] transform(Content sourceContent) throws IOException, TransformationException {
            validateContent(sourceContent);
            return "Transformed text".getBytes(StandardCharsets.UTF_8);
        }
    }
}
