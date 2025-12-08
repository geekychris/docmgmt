package com.docmgmt.transformer.impl;

import com.docmgmt.model.Content;
import com.docmgmt.transformer.AbstractContentTransformer;
import com.docmgmt.transformer.TransformationException;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Transformer that converts Word documents (.doc and .docx) to plain text.
 * Uses Apache POI to extract text content from Word files.
 */
@Component
public class WordToTextTransformer extends AbstractContentTransformer {
    
    private static final String SOURCE_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String SOURCE_TYPE_DOC = "application/msword";
    private static final String TARGET_TYPE = "text/plain";
    private static final String NAME = "Word to Text Transformer";
    
    public WordToTextTransformer() {
        super(SOURCE_TYPE_DOCX, TARGET_TYPE, NAME);
    }
    
    @Override
    public byte[] transform(Content sourceContent) throws IOException, TransformationException {
        validateContent(sourceContent);
        byte[] wordBytes = getContentBytes(sourceContent);
        
        String contentType = sourceContent.getContentType();
        String extractedText;
        
        try {
            if (isDocxFormat(contentType)) {
                extractedText = extractTextFromDocx(wordBytes);
            } else if (isDocFormat(contentType)) {
                extractedText = extractTextFromDoc(wordBytes);
            } else {
                throw new TransformationException("Unsupported Word document format: " + contentType);
            }
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                return "[No extractable text content found in Word document]".getBytes(StandardCharsets.UTF_8);
            }
            
            return extractedText.getBytes(StandardCharsets.UTF_8);
            
        } catch (IOException e) {
            throw new TransformationException("Failed to extract text from Word document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract text from a .docx file (Office Open XML format)
     */
    private String extractTextFromDocx(byte[] wordBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(wordBytes);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            return extractor.getText();
        }
    }
    
    /**
     * Extract text from a .doc file (legacy Word 97-2003 format)
     */
    private String extractTextFromDoc(byte[] wordBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(wordBytes);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            
            return extractor.getText();
        }
    }
    
    @Override
    public boolean canTransform(Content content) {
        if (content == null || content.getContentType() == null) {
            return false;
        }
        
        String contentType = content.getContentType().toLowerCase();
        return isDocxFormat(contentType) || isDocFormat(contentType);
    }
    
    private boolean isDocxFormat(String contentType) {
        if (contentType == null) return false;
        String lowerType = contentType.toLowerCase();
        return lowerType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               lowerType.equals("application/vnd.ms-word.document.macroenabled.12");
    }
    
    private boolean isDocFormat(String contentType) {
        if (contentType == null) return false;
        String lowerType = contentType.toLowerCase();
        return lowerType.equals("application/msword") ||
               lowerType.equals("application/vnd.ms-word") ||
               lowerType.equals("application/x-msword");
    }
    
    @Override
    public String getSourceContentType() {
        // Return the most common type, but canTransform handles all variants
        return SOURCE_TYPE_DOCX;
    }
}
