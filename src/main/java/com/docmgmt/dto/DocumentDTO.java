package com.docmgmt.dto;

import com.docmgmt.model.Document;
import com.docmgmt.model.SysObject;
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
        
        // Map content
        if (document.getContents() != null && !document.getContents().isEmpty()) {
            dto.setContents(document.getContents().stream()
                    .map(ContentDTO::fromEntity)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * Convert from DTO to entity
     * @return the entity
     */
    public Document toEntity() {
        Document document = Document.builder()
                .description(this.getDescription())
                .documentType(this.getDocumentType())
                .author(this.getAuthor())
                .keywords(this.getKeywords())
                .build();
        
        document.setId(this.getId());
        document.setName(this.getName());
        document.setMajorVersion(this.getMajorVersion());
        document.setMinorVersion(this.getMinorVersion());
        
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

