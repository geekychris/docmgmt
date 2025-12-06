package com.docmgmt.dto;

import com.docmgmt.model.*;
import com.docmgmt.model.Document.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO for Document entity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO extends BaseSysObjectDTO {
    
    private String description;
    
    @NotNull(message = "Document type is required")
    private Document.DocumentType documentType;
    
    private Set<String> tags;
    
    private String author;
    
    private String keywords;
    
    /**
     * Convert from Document entity to DTO
     * @param document the entity
     * @return the DTO
     */
    public static DocumentDTO fromEntity(Document document) {
        DocumentDTO dto = DocumentDTO.builder()
                .id(document.getId())
                .name(document.getName())
                .majorVersion(document.getMajorVersion())
                .minorVersion(document.getMinorVersion())
                .description(document.getDescription())
                .documentType(document.getDocumentType())
                .author(document.getAuthor())
                .keywords(document.getKeywords())
                .createdAt(document.getCreatedAt())
                .modifiedAt(document.getModifiedAt())
                .build();
        
        if (document.getParentVersion() != null) {
            dto.setParentVersionId(document.getParentVersion().getId());
        }
        
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            dto.setTags(new HashSet<>(document.getTags()));
        }
        
        // Map content - safely handle lazy-loaded collections
        try {
            if (document.getContents() != null && !document.getContents().isEmpty()) {
                dto.setContents(document.getContents().stream()
                        .map(ContentDTO::fromEntity)
                        .collect(Collectors.toList()));
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Contents not loaded - skip it
            dto.setContents(null);
        }
        
        return dto;
    }
    
    /**
     * Convert from DTO to entity
     * @return the entity
     */
    public Document toEntity() {
        // Create the appropriate document subclass based on type
        Document document;
        DocumentType type = this.getDocumentType() != null ? this.getDocumentType() : DocumentType.OTHER;
        
        switch (type) {
            case ARTICLE:
                document = Article.builder().build();
                break;
            case REPORT:
                document = Report.builder().build();
                break;
            case CONTRACT:
                document = Contract.builder().build();
                break;
            case MANUAL:
                document = Manual.builder().build();
                break;
            case PRESENTATION:
                document = Presentation.builder().build();
                break;
            case TRIP_REPORT:
                document = TripReport.builder().build();
                break;
            default:
                // Default to Article for OTHER type
                document = Article.builder().build();
                break;
        }
        
        // Set common fields
        document.setId(this.getId());
        document.setName(this.getName());
        document.setMajorVersion(this.getMajorVersion());
        document.setMinorVersion(this.getMinorVersion());
        document.setDescription(this.getDescription());
        document.setAuthor(this.getAuthor());
        document.setKeywords(this.getKeywords());
        
        if (this.getTags() != null && !this.getTags().isEmpty()) {
            document.setTags(new HashSet<>(this.getTags()));
        }
        
        return document;
    }
    
    /**
     * Update an entity with values from this DTO
     * @param sysObject the entity to update
     */
    @Override
    public void updateEntity(SysObject sysObject) {
        super.updateEntity(sysObject);
        
        if (sysObject instanceof Document) {
            Document document = (Document) sysObject;
            document.setDescription(this.getDescription());
            document.setDocumentType(this.getDocumentType());
            document.setAuthor(this.getAuthor());
            document.setKeywords(this.getKeywords());
            
            if (this.getTags() != null) {
                document.setTags(new HashSet<>(this.getTags()));
            }
        }
    }
}

