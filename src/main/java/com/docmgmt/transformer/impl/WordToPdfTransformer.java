package com.docmgmt.transformer.impl;

import com.docmgmt.model.Content;
import com.docmgmt.transformer.AbstractContentTransformer;
import com.docmgmt.transformer.TransformationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Transformer that converts Word documents (.doc and .docx) to PDF format.
 * Uses Apache POI to read Word files and PDFBox to generate PDFs.
 * 
 * Note: This implementation provides basic text extraction and PDF generation.
 * Complex formatting, images, tables, and other advanced Word features may not be fully preserved.
 */
@Component
public class WordToPdfTransformer extends AbstractContentTransformer {
    
    private static final String SOURCE_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String SOURCE_TYPE_DOC = "application/msword";
    private static final String TARGET_TYPE = "application/pdf";
    private static final String NAME = "Word to PDF Transformer";
    
    // PDF layout constants
    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 12;
    private static final float LEADING = 1.5f * FONT_SIZE;
    private static final float MAX_LINE_WIDTH = PDRectangle.LETTER.getWidth() - 2 * MARGIN;
    
    public WordToPdfTransformer() {
        super(SOURCE_TYPE_DOCX, TARGET_TYPE, NAME);
    }
    
    @Override
    public byte[] transform(Content sourceContent) throws IOException, TransformationException {
        validateContent(sourceContent);
        byte[] wordBytes = getContentBytes(sourceContent);
        
        String contentType = sourceContent.getContentType();
        
        try {
            if (isDocxFormat(contentType)) {
                return convertDocxToPdf(wordBytes);
            } else if (isDocFormat(contentType)) {
                return convertDocToPdf(wordBytes);
            } else {
                throw new TransformationException("Unsupported Word document format: " + contentType);
            }
        } catch (IOException e) {
            throw new TransformationException("Failed to convert Word document to PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert a .docx file to PDF
     */
    private byte[] convertDocxToPdf(byte[] wordBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(wordBytes);
             XWPFDocument document = new XWPFDocument(inputStream);
             PDDocument pdfDocument = new PDDocument()) {
            
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            
            if (paragraphs.isEmpty()) {
                // Create empty PDF with a message
                createSimplePdf(pdfDocument, "[Empty Word document]");
            } else {
                // Extract text and add to PDF
                StringBuilder fullText = new StringBuilder();
                for (XWPFParagraph paragraph : paragraphs) {
                    String text = paragraph.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        fullText.append(text).append("\n");
                    }
                }
                
                String textContent = fullText.toString().trim();
                if (textContent.isEmpty()) {
                    createSimplePdf(pdfDocument, "[No extractable content]");
                } else {
                    createSimplePdf(pdfDocument, textContent);
                }
            }
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            pdfDocument.save(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Convert a .doc file (legacy format) to PDF
     */
    private byte[] convertDocToPdf(byte[] wordBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(wordBytes);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document);
             PDDocument pdfDocument = new PDDocument()) {
            
            String text = extractor.getText();
            
            if (text == null || text.trim().isEmpty()) {
                createSimplePdf(pdfDocument, "[No extractable content]");
            } else {
                createSimplePdf(pdfDocument, text);
            }
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            pdfDocument.save(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Create a simple PDF with text content, handling page breaks and line wrapping
     */
    private void createSimplePdf(PDDocument pdfDocument, String text) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // Wrap all text into lines
        List<String> lines = wrapText(text, font, FONT_SIZE);
        
        PDPage currentPage = null;
        PDPageContentStream contentStream = null;
        float yPosition = 0;
        
        try {
            for (String line : lines) {
                // Check if we need a new page
                if (currentPage == null || yPosition - LEADING < MARGIN) {
                    // Close previous content stream if exists
                    if (contentStream != null) {
                        contentStream.endText();
                        contentStream.close();
                    }
                    
                    // Create new page
                    currentPage = new PDPage(PDRectangle.LETTER);
                    pdfDocument.addPage(currentPage);
                    yPosition = PDRectangle.LETTER.getHeight() - MARGIN;
                    
                    // Start new content stream
                    contentStream = new PDPageContentStream(pdfDocument, currentPage);
                    contentStream.beginText();
                    contentStream.setFont(font, FONT_SIZE);
                    contentStream.newLineAtOffset(MARGIN, yPosition);
                }
                
                // Add line to current page
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -LEADING);
                yPosition -= LEADING;
            }
        } finally {
            if (contentStream != null) {
                contentStream.endText();
                contentStream.close();
            }
        }
    }
    
    /**
     * Wrap text to fit within the specified width
     */
    private List<String> wrapText(String text, PDType1Font font, float fontSize) throws IOException {
        List<String> lines = new java.util.ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (width > MAX_LINE_WIDTH && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    @Override
    public boolean canTransform(Content content) {
        if (content == null || content.getContentType() == null) {
            return false;
        }
        
        String contentType = content.getContentType().toLowerCase();
        return isDocxFormat(contentType) || isDocFormat(contentType);
    }
    
    @Override
    public boolean producesIndexableContent() {
        return false; // PDF output is not directly indexable as text
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
