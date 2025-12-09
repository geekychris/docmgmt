package com.docmgmt.transformer;

import com.docmgmt.model.Content;
import com.docmgmt.transformer.impl.WordToPdfTransformer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify WordToPdfTransformer is properly integrated
 * with the TransformerRegistry and Spring context.
 */
@SpringBootTest
class WordToPdfTransformerIntegrationTest {

    @Autowired
    private TransformerRegistry transformerRegistry;

    @Autowired
    private WordToPdfTransformer wordToPdfTransformer;

    @Test
    void testWordToPdfTransformerIsRegistered() {
        assertNotNull(wordToPdfTransformer, "WordToPdfTransformer should be autowired");
        assertNotNull(transformerRegistry, "TransformerRegistry should be autowired");
        
        // Verify transformer is registered
        assertTrue(transformerRegistry.getTransformerCount() > 0, 
                  "Registry should have transformers");
        
        assertTrue(transformerRegistry.getAllTransformers().contains(wordToPdfTransformer),
                  "Registry should contain WordToPdfTransformer");
    }

    @Test
    void testFindWordToPdfTransformer() {
        Optional<ContentTransformer> transformer = transformerRegistry.findTransformer(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/pdf"
        );
        
        assertTrue(transformer.isPresent(), "Should find Word to PDF transformer");
        assertInstanceOf(WordToPdfTransformer.class, transformer.get());
    }

    @Test
    void testFindTransformerForDocFormat() throws Exception {
        Optional<ContentTransformer> transformer = transformerRegistry.findTransformer(
            "application/msword",
            "application/pdf"
        );
        
        // Note: This might not find the transformer because the registry searches by 
        // exact getSourceContentType() which returns the .docx type. 
        // The canTransform() method handles all variants.
        // This is a known limitation of the findTransformer(String, String) method.
        
        // Instead, we should use findTransformer(Content)
        Content docContent = new Content();
        docContent.setContentType("application/msword");
        docContent.setContentBytes(new byte[]{1, 2, 3});
        
        Optional<ContentTransformer> contentTransformer = 
            transformerRegistry.findTransformer(docContent);
        
        assertTrue(contentTransformer.isPresent(), 
                  "Should find transformer using canTransform method");
        assertInstanceOf(WordToPdfTransformer.class, contentTransformer.get());
    }

    @Test
    void testEndToEndConversionViaRegistry() throws Exception {
        // Create a Word document
        byte[] docxBytes = createSimpleDocx("Integration Test\nThis is a test of the complete flow.");
        
        Content wordContent = new Content();
        wordContent.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        wordContent.setContentBytes(docxBytes);
        
        // Find transformer via registry
        Optional<ContentTransformer> transformer = transformerRegistry.findTransformer(wordContent);
        assertTrue(transformer.isPresent(), "Should find Word to PDF transformer");
        
        // Transform
        byte[] pdfBytes = transformer.get().transform(wordContent);
        
        // Verify PDF
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            assertTrue(pdfDoc.getNumberOfPages() > 0);
            
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(pdfDoc);
            
            assertTrue(pdfText.contains("Integration Test"));
            assertTrue(pdfText.contains("complete flow"));
        }
    }

    @Test
    void testTransformerProperties() {
        assertEquals("Word to PDF Transformer", wordToPdfTransformer.getName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    wordToPdfTransformer.getSourceContentType());
        assertEquals("application/pdf", wordToPdfTransformer.getTargetContentType());
        assertFalse(wordToPdfTransformer.producesIndexableContent(),
                   "PDF output should not be indexable");
    }

    @Test
    void testCanTransformVariousFormats() throws Exception {
        String[] supportedTypes = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-word.document.macroenabled.12",
            "application/msword",
            "application/vnd.ms-word",
            "application/x-msword"
        };
        
        for (String type : supportedTypes) {
            Content content = new Content();
            content.setContentType(type);
            content.setContentBytes(new byte[]{1, 2, 3});
            
            assertTrue(wordToPdfTransformer.canTransform(content),
                      "Should support content type: " + type);
        }
    }

    @Test
    void testCannotTransformNonWordFormats() throws Exception {
        String[] unsupportedTypes = {
            "application/pdf",
            "text/plain",
            "application/json",
            "image/png"
        };
        
        for (String type : unsupportedTypes) {
            Content content = new Content();
            content.setContentType(type);
            content.setContentBytes(new byte[]{1, 2, 3});
            
            assertFalse(wordToPdfTransformer.canTransform(content),
                       "Should not support content type: " + type);
        }
    }

    /**
     * Helper method to create a simple DOCX document
     */
    private byte[] createSimpleDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            if (text != null && !text.isEmpty()) {
                String[] lines = text.split("\\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        XWPFParagraph paragraph = document.createParagraph();
                        paragraph.createRun().setText(line);
                    }
                }
            }
            
            document.write(out);
            return out.toByteArray();
        }
    }
}
