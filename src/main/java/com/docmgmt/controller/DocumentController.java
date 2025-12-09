package com.docmgmt.controller;

import com.docmgmt.dto.DocumentDTO;
import com.docmgmt.dto.FieldSuggestionDTO;
import com.docmgmt.model.Document;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.DocumentFieldExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Document entity operations
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management operations including creation, versioning, and queries")
public class DocumentController extends AbstractSysObjectController<Document, DocumentDTO, DocumentService> {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentFieldExtractionService fieldExtractionService;
    
    @Autowired
    public DocumentController(DocumentService service, DocumentFieldExtractionService fieldExtractionService) {
        super(service);
        this.fieldExtractionService = fieldExtractionService;
    }
    
    @Override
    protected DocumentDTO toDTO(Document entity) {
        return DocumentDTO.fromEntity(entity);
    }
    
    @Override
    protected Document toEntity(DocumentDTO dto) {
        return dto.toEntity();
    }
    
    /**
     * Find documents by document type
     * @param documentType The document type
     * @return List of document DTOs
     */
    @Operation(
        summary = "Find documents by type",
        description = "Retrieve all documents of a specific type (ARTICLE, MANUAL, REPORT, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documents found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-type/{documentType}")
    public ResponseEntity<List<DocumentDTO>> findByDocumentType(
            @Parameter(description = "Document type to search for", required = true)
            @PathVariable Document.DocumentType documentType) {
        try {
            List<DocumentDTO> documents = service.findByDocumentType(documentType).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error finding documents by type: {}", documentType, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding documents", e);
        }
    }
    
    /**
     * Find documents by tag
     * @param tag The tag to search for
     * @return List of document DTOs
     */
    @Operation(
        summary = "Find documents by tag",
        description = "Retrieve all documents that contain the specified tag"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documents found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-tag/{tag}")
    public ResponseEntity<List<DocumentDTO>> findByTag(
            @Parameter(description = "Tag to search for", required = true, example = "java")
            @PathVariable String tag) {
        try {
            List<DocumentDTO> documents = service.findByTag(tag).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error finding documents by tag: {}", tag, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding documents", e);
        }
    }
    
    /**
     * Find documents by keywords
     * @param keywords The keywords to search for
     * @return List of document DTOs
     */
    @Operation(
        summary = "Search documents by keywords",
        description = "Search for documents using keyword matching (searches in the keywords field)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documents found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<List<DocumentDTO>> searchByKeywords(
            @Parameter(description = "Keywords to search for", required = true, example = "spring boot java")
            @RequestParam String keywords) {
        try {
            List<DocumentDTO> documents = service.findByKeywords(keywords).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error searching documents by keywords: {}", keywords, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching documents", e);
        }
    }
    
    /**
     * Extract field suggestions for a document using AI
     * @param id The document ID
     * @return Field suggestions including current and suggested values
     */
    @Operation(
        summary = "Extract field suggestions using AI",
        description = "Analyze document content using AI (Ollama) to suggest metadata fields like description, keywords, tags, and document type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Field suggestions extracted successfully"),
        @ApiResponse(responseCode = "400", description = "Document has no text content for extraction"),
        @ApiResponse(responseCode = "404", description = "Document not found"),
        @ApiResponse(responseCode = "500", description = "Error during field extraction")
    })
    @GetMapping("/{id}/extract-fields")
    public ResponseEntity<FieldSuggestionDTO> extractFields(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long id) {
        try {
            FieldSuggestionDTO suggestions = fieldExtractionService.extractFieldsFromDocument(id, null);
            return ResponseEntity.ok(suggestions);
        } catch (EntityNotFoundException e) {
            logger.error("Document not found: {}", id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for document {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error extracting fields for document: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error extracting fields", e);
        }
    }
    
    /**
     * Apply selected field suggestions to a document
     * @param id The document ID
     * @param request Request containing fields to apply and suggested values
     * @return Updated document DTO
     */
    @Operation(
        summary = "Apply field suggestions to document",
        description = "Update document fields with AI-suggested values based on user selection"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fields applied successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found"),
        @ApiResponse(responseCode = "500", description = "Error applying fields")
    })
    @PostMapping("/{id}/apply-fields")
    public ResponseEntity<DocumentDTO> applyFields(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long id,
            @RequestBody ApplyFieldsRequest request) {
        try {
            Document updated = fieldExtractionService.applyFieldSuggestions(
                id, 
                request.getFieldsToApply(), 
                request.getSuggestedFields()
            );
            return ResponseEntity.ok(toDTO(updated));
        } catch (EntityNotFoundException e) {
            logger.error("Document not found: {}", id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found", e);
        } catch (Exception e) {
            logger.error("Error applying fields for document: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error applying fields", e);
        }
    }
    
    /**
     * Batch extract and apply fields for multiple documents
     * @param request Request containing list of document IDs
     * @return Map of document ID to result status
     */
    @Operation(
        summary = "Batch extract and apply fields",
        description = "Extract and automatically apply AI-suggested fields for multiple documents. Only non-null extracted fields are applied."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch processing completed"),
        @ApiResponse(responseCode = "500", description = "Error during batch processing")
    })
    @PostMapping("/batch-extract-fields")
    public ResponseEntity<Map<Long, String>> batchExtractFields(
            @RequestBody BatchExtractRequest request) {
        try {
            Map<Long, String> results = fieldExtractionService.extractAndApplyFieldsForDocuments(
                request.getDocumentIds()
            );
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error during batch field extraction", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during batch extraction", e);
        }
    }
    
    /**
     * Request body for applying field suggestions
     */
    public static class ApplyFieldsRequest {
        private Map<String, Boolean> fieldsToApply;
        private FieldSuggestionDTO.DocumentFields suggestedFields;
        
        public Map<String, Boolean> getFieldsToApply() {
            return fieldsToApply;
        }
        
        public void setFieldsToApply(Map<String, Boolean> fieldsToApply) {
            this.fieldsToApply = fieldsToApply;
        }
        
        public FieldSuggestionDTO.DocumentFields getSuggestedFields() {
            return suggestedFields;
        }
        
        public void setSuggestedFields(FieldSuggestionDTO.DocumentFields suggestedFields) {
            this.suggestedFields = suggestedFields;
        }
    }
    
    /**
     * Request body for batch field extraction
     */
    public static class BatchExtractRequest {
        private List<Long> documentIds;
        
        public List<Long> getDocumentIds() {
            return documentIds;
        }
        
        public void setDocumentIds(List<Long> documentIds) {
            this.documentIds = documentIds;
        }
    }
}
