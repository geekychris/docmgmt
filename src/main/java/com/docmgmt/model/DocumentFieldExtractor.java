package com.docmgmt.model;

/**
 * Interface for document types to provide AI field extraction rules
 */
public interface DocumentFieldExtractor {
    
    /**
     * Get the field extraction rules for AI prompts.
     * This defines what type-specific fields should be extracted from document content.
     * 
     * @return Field extraction rules in a format suitable for AI prompts
     */
    String getFieldExtractionRules();
    
    /**
     * Get a map of current field values for this document
     * 
     * @return Map of field names to their current values
     */
    java.util.Map<String, Object> getCurrentFieldValues();
    
    /**
     * Apply extracted field values to this document
     * 
     * @param extractedFields Map of field names to extracted values
     */
    void applyExtractedFields(java.util.Map<String, Object> extractedFields);
}
