package com.docmgmt.transformer;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.Report;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.transformer.impl.PdfToTextTransformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PdfToTextTransformerTest {
    
    @Autowired
    private PdfToTextTransformer pdfTransformer;
    
    @Autowired
    private ContentService contentService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private TransformerRegistry transformerRegistry;
    
    private Document testDocument;
    
    @BeforeEach
    void setUp() {
        testDocument = Report.builder()
                .name("Test Document")
                .build();
        testDocument = documentService.save(testDocument);
    }
    
    @Test
    void testTransformerIsRegistered() {
        assertNotNull(pdfTransformer);
        assertTrue(transformerRegistry.getTransformerCount() > 0);
        assertTrue(transformerRegistry.getAllTransformers().contains(pdfTransformer));
    }
    
    @Test
    void testGetSourceAndTargetTypes() {
        assertEquals("application/pdf", pdfTransformer.getSourceContentType());
        assertEquals("text/plain", pdfTransformer.getTargetContentType());
    }
    
    @Test
    void testProducesIndexableContent() {
        assertTrue(pdfTransformer.producesIndexableContent());
    }
    
    @Test
    void testCanTransformPdf() throws IOException {
        byte[] pdfBytes = createSimplePdf("Test content");
        
        Content pdfContent = Content.builder()
                .name("test.pdf")
                .contentType("application/pdf")
                .content(pdfBytes)
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        
        assertTrue(pdfTransformer.canTransform(pdfContent));
    }
    
    @Test
    void testCannotTransformNonPdf() {
        Content textContent = Content.builder()
                .name("test.txt")
                .contentType("text/plain")
                .content("Text content".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        
        assertFalse(pdfTransformer.canTransform(textContent));
    }
    
    @Test
    void testTransformPdfToText() throws IOException, TransformationException {
        String expectedText = "Hello World from PDF";
        byte[] pdfBytes = createSimplePdf(expectedText);
        
        Content pdfContent = Content.builder()
                .name("test.pdf")
                .contentType("application/pdf")
                .content(pdfBytes)
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        pdfContent = contentService.save(pdfContent);
        
        byte[] textBytes = pdfTransformer.transform(pdfContent);
        String extractedText = new String(textBytes, StandardCharsets.UTF_8);
        
        assertNotNull(extractedText);
        assertTrue(extractedText.contains(expectedText));
    }
    
    @Test
    void testTransformAndAddRendition() throws IOException, TransformationException {
        String pdfText = "PDF Content to Extract";
        byte[] pdfBytes = createSimplePdf(pdfText);
        
        Content pdfContent = Content.builder()
                .name("document.pdf")
                .contentType("application/pdf")
                .content(pdfBytes)
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        pdfContent = contentService.save(pdfContent);
        
        // Transform and add as rendition
        Content textRendition = contentService.transformAndAddRendition(pdfContent.getId(), null);
        
        assertNotNull(textRendition);
        assertFalse(textRendition.isPrimary());
        assertTrue(textRendition.isIndexable());
        assertEquals("text/plain", textRendition.getContentType());
        assertTrue(textRendition.getName().contains(".plain"));
        
        // Verify the text was extracted
        byte[] extractedBytes = textRendition.getContentBytes();
        String extractedText = new String(extractedBytes, StandardCharsets.UTF_8);
        assertTrue(extractedText.contains(pdfText));
    }
    
    @Test
    void testTransformInvalidPdf() {
        Content invalidContent = Content.builder()
                .name("invalid.pdf")
                .contentType("application/pdf")
                .content("Not a real PDF".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        
        assertThrows(TransformationException.class, () -> {
            pdfTransformer.transform(invalidContent);
        });
    }
    
    @Test
    void testTransformNullContent() {
        assertThrows(TransformationException.class, () -> {
            pdfTransformer.transform(null);
        });
    }
    
    @Test
    void testTransformEmptyContent() {
        Content emptyContent = Content.builder()
                .name("empty.pdf")
                .contentType("application/pdf")
                .content(new byte[0])
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        
        assertThrows(TransformationException.class, () -> {
            pdfTransformer.transform(emptyContent);
        });
    }
    
    @Test
    void testFindTransformerForPdf() throws IOException {
        byte[] pdfBytes = createSimplePdf("Test");
        
        Content pdfContent = Content.builder()
                .name("test.pdf")
                .contentType("application/pdf")
                .content(pdfBytes)
                .sysObject(testDocument)
                .isPrimary(true)
                .build();
        
        var transformer = transformerRegistry.findTransformer(pdfContent);
        assertTrue(transformer.isPresent());
        assertEquals(pdfTransformer, transformer.get());
    }
    
    /**
     * Helper method to create a simple PDF with text content
     */
    private byte[] createSimplePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
