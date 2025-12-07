package com.docmgmt.controller;

import com.docmgmt.dto.DocumentDTO;
import com.docmgmt.model.Document;
import com.docmgmt.service.DocumentService;
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
import java.util.stream.Collectors;

/**
 * Controller for Document entity operations
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management operations including creation, versioning, and queries")
public class DocumentController extends AbstractSysObjectController<Document, DocumentDTO, DocumentService> {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    @Autowired
    public DocumentController(DocumentService service) {
        super(service);
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
}
