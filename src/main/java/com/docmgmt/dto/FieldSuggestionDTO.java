package com.docmgmt.dto;

import com.docmgmt.model.Document.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for field extraction suggestions from AI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSuggestionDTO {
    
    /**
     * Current document values
     */
    private DocumentFields currentFields;
    
    /**
     * AI-suggested values
     */
    private DocumentFields suggestedFields;
    
    /**
     * Nested class containing document field values
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentFields {
        private String description;
        private String keywords;
        private List<String> tags;
        private DocumentType documentType;
        
        // Type-specific fields stored as flexible map
        private java.util.Map<String, Object> typeSpecificFields;
    }
}
