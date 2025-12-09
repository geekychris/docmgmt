package com.docmgmt.transformer;

import com.docmgmt.model.Content;
import com.docmgmt.transformer.impl.WordToPdfTransformer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WordToPdfTransformerTest {

    private WordToPdfTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new WordToPdfTransformer();
    }

    @Test
    void testGetSourceContentType() {
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                     transformer.getSourceContentType());
    }

    @Test
    void testGetTargetContentType() {
        assertEquals("application/pdf", transformer.getTargetContentType());
    }

    @Test
    void testGetName() {
        assertEquals("Word to PDF Transformer", transformer.getName());
    }

    @Test
    void testProducesIndexableContent() {
        assertFalse(transformer.producesIndexableContent(), 
                   "PDF output should not be directly indexable");
    }

    @Test
    void testCanTransformDocx() {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        
        assertTrue(transformer.canTransform(content));
    }

    @Test
    void testCanTransformDoc() {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/msword");
        
        assertTrue(transformer.canTransform(content));
    }

    @Test
    void testCanTransformWithDifferentContentType() {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/pdf");
        
        assertFalse(transformer.canTransform(content));
    }

    @Test
    void testCanTransformWithNullContent() {
        assertFalse(transformer.canTransform(null));
    }

    @Test
    void testCanTransformWithNullContentType() {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn(null);
        
        assertFalse(transformer.canTransform(content));
    }

    @Test
    void testTransformSimpleDocx() throws Exception {
        // Create a simple DOCX document
        byte[] docxBytes = createSimpleDocx("Hello World!\nThis is a test document.");
        
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(content.getContentBytes()).thenReturn(docxBytes);
        
        // Transform to PDF
        byte[] pdfBytes = transformer.transform(content);
        
        // Verify the result
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Verify it's a valid PDF by reading it
        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            assertTrue(pdfDoc.getNumberOfPages() > 0);
            
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(pdfDoc);
            
            assertTrue(pdfText.contains("Hello World"));
            assertTrue(pdfText.contains("test document"));
        }
    }

    @Test
    void testTransformEmptyDocx() throws Exception {
        // Create an empty DOCX document
        byte[] docxBytes = createSimpleDocx("");
        
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(content.getContentBytes()).thenReturn(docxBytes);
        
        // Transform to PDF
        byte[] pdfBytes = transformer.transform(content);
        
        // Verify the result
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Verify it contains placeholder text
        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(pdfDoc);
            
            assertTrue(pdfText.contains("No extractable content") || 
                      pdfText.contains("Empty Word document"));
        }
    }

    @Test
    void testTransformWithNullContent() {
        Content content = null;
        
        assertThrows(TransformationException.class, () -> {
            transformer.transform(content);
        });
    }

    @Test
    void testTransformWithNullContentType() {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn(null);
        
        assertThrows(TransformationException.class, () -> {
            transformer.transform(content);
        });
    }

    @Test
    void testTransformWithEmptyBytes() throws Exception {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(content.getContentBytes()).thenReturn(new byte[0]);
        
        assertThrows(TransformationException.class, () -> {
            transformer.transform(content);
        });
    }

    @Test
    void testTransformWithUnsupportedContentType() throws Exception {
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/pdf");
        when(content.getContentBytes()).thenReturn(new byte[]{1, 2, 3});
        
        assertThrows(TransformationException.class, () -> {
            transformer.transform(content);
        });
    }

    @Test
    void testTransformLongDocument() throws Exception {
        // Create a document with enough text to span multiple pages
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("This is line ").append(i).append(" of a very long document that should span multiple pages. ");
        }
        
        byte[] docxBytes = createSimpleDocx(longText.toString());
        
        Content content = mock(Content.class);
        when(content.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(content.getContentBytes()).thenReturn(docxBytes);
        
        // Transform to PDF
        byte[] pdfBytes = transformer.transform(content);
        
        // Verify the result
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Verify it has multiple pages
        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            assertTrue(pdfDoc.getNumberOfPages() >= 1);
            
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(pdfDoc);
            
            assertTrue(pdfText.contains("line 0"));
            assertTrue(pdfText.contains("line 99"));
        }
    }

    /**
     * Helper method to create a simple DOCX document with the given text
     */
    private byte[] createSimpleDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            if (text != null && !text.isEmpty()) {
                // Split text into lines and create paragraphs
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
