package com.docmgmt.transformer.impl;

import com.docmgmt.model.Content;
import com.docmgmt.transformer.AbstractContentTransformer;
import com.docmgmt.transformer.TransformationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Transformer that converts PDF documents to plain text.
 * Uses Apache PDFBox to extract text content from PDF files.
 */
@Component
public class
PdfToTextTransformer extends AbstractContentTransformer {
    
    private static final String SOURCE_TYPE = "application/pdf";
    private static final String TARGET_TYPE = "text/plain";
    private static final String NAME = "PDF to Text Transformer";
    
    public PdfToTextTransformer() {
        super(SOURCE_TYPE, TARGET_TYPE, NAME);
    }
    
    @Override
    public byte[] transform(Content sourceContent) throws IOException, TransformationException {
        validateContent(sourceContent);
        byte[] pdfBytes = getContentBytes(sourceContent);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            
            if (document.isEncrypted()) {
                throw new TransformationException("PDF document is encrypted and cannot be processed");
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Better text ordering
            
            String text = stripper.getText(document);
            
            if (text == null || text.trim().isEmpty()) {
                // Some PDFs are image-based or have no extractable text
                // Return empty string rather than failing completely
                return "[No extractable text content found - PDF may be image-based]".getBytes(StandardCharsets.UTF_8);
            }
            
            return text.getBytes(StandardCharsets.UTF_8);
            
        } catch (IOException e) {
            throw new TransformationException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canTransform(Content content) {
        // Also accept common PDF mime type variants
        if (content == null || content.getContentType() == null) {
            return false;
        }
        
        String contentType = content.getContentType().toLowerCase();
        return contentType.equals("application/pdf") || 
               contentType.equals("application/x-pdf") ||
               contentType.equals("application/x-bzpdf") ||
               contentType.equals("application/x-gzpdf");
    }
}
