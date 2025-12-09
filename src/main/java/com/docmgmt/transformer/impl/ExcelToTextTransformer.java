package com.docmgmt.transformer.impl;

import com.docmgmt.model.Content;
import com.docmgmt.transformer.AbstractContentTransformer;
import com.docmgmt.transformer.TransformationException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

/**
 * Transformer that converts Excel files (XLS and XLSX) to plain text.
 * Uses Apache POI to extract text content from Excel spreadsheets.
 */
@Component
public class ExcelToTextTransformer extends AbstractContentTransformer {
    
    private static final String SOURCE_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SOURCE_TYPE_XLS = "application/vnd.ms-excel";
    private static final String TARGET_TYPE = "text/plain";
    private static final String NAME = "Excel to Text Transformer";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public ExcelToTextTransformer() {
        super(SOURCE_TYPE_XLSX, TARGET_TYPE, NAME);
    }
    
    @Override
    public byte[] transform(Content sourceContent) throws IOException, TransformationException {
        validateContent(sourceContent);
        byte[] excelBytes = getContentBytes(sourceContent);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes)) {
            Workbook workbook = createWorkbook(sourceContent.getContentType(), inputStream);
            
            StringBuilder textContent = new StringBuilder();
            DataFormatter dataFormatter = new DataFormatter();
            
            // Iterate through all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                
                // Add sheet name as header
                textContent.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n\n");
                
                // Iterate through all rows
                for (Row row : sheet) {
                    boolean hasContent = false;
                    StringBuilder rowText = new StringBuilder();
                    
                    // Iterate through all cells in the row
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell, dataFormatter);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            hasContent = true;
                            if (rowText.length() > 0) {
                                rowText.append("\t");
                            }
                            rowText.append(cellValue);
                        }
                    }
                    
                    // Only add rows that have content
                    if (hasContent) {
                        textContent.append(rowText).append("\n");
                    }
                }
                
                textContent.append("\n");
            }
            
            workbook.close();
            
            String result = textContent.toString().trim();
            if (result.isEmpty()) {
                return "[No extractable text content found in Excel file]".getBytes(StandardCharsets.UTF_8);
            }
            
            return result.getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new TransformationException("Failed to extract text from Excel: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create appropriate workbook based on content type
     */
    private Workbook createWorkbook(String contentType, ByteArrayInputStream inputStream) throws IOException {
        if (contentType.equals(SOURCE_TYPE_XLS)) {
            return new HSSFWorkbook(inputStream);
        } else {
            return new XSSFWorkbook(inputStream);
        }
    }
    
    /**
     * Extract cell value as string
     */
    private String getCellValueAsString(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return "";
        }
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return DATE_FORMAT.format(cell.getDateCellValue());
                    }
                    return dataFormatter.formatCellValue(cell);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return dataFormatter.formatCellValue(cell);
                    } catch (Exception e) {
                        return cell.getCellFormula();
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    @Override
    public boolean canTransform(Content content) {
        if (content == null || content.getContentType() == null) {
            return false;
        }
        
        String contentType = content.getContentType().toLowerCase();
        return contentType.equals(SOURCE_TYPE_XLSX.toLowerCase()) || 
               contentType.equals(SOURCE_TYPE_XLS.toLowerCase()) ||
               contentType.equals("application/excel") ||
               contentType.equals("application/x-excel") ||
               contentType.equals("application/x-msexcel");
    }
}
