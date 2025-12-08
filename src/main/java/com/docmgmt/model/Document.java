package com.docmgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Entity
@Table(name = "document")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "doc_type", discriminatorType = DiscriminatorType.STRING)
@EntityListeners(com.docmgmt.listener.DocumentIndexListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class Document extends SysObject {

    public enum DocumentType {
        ARTICLE, REPORT, CONTRACT, MANUAL, PRESENTATION, TRIP_REPORT, OTHER
    }
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Document type - kept for backward compatibility and easier querying.
     * Subclasses should set this in their constructors or use @PostLoad.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();
    
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;
    
    /**
     * Add a tag to the document
     * @param tag The tag to add
     * @return this document for method chaining
     */
    public Document addTag(String tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        return this;
    }
    
    /**
     * Remove a tag from the document
     * @param tag The tag to remove
     * @return this document for method chaining
     */
    public Document removeTag(String tag) {
        if (tags != null) {
            tags.remove(tag);
        }
        return this;
    }
    
    /**
     * Copy attributes to target entity
     * Overrides the method in SysObject to include Document-specific attributes
     * @param target The target entity
     */
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Document) {
            Document documentTarget = (Document) target;
            documentTarget.setDescription(this.getDescription());
            documentTarget.setDocumentType(this.getDocumentType());
            documentTarget.setKeywords(this.getKeywords());
            
            // Copy tags
            if (this.getTags() != null && !this.getTags().isEmpty()) {
                documentTarget.setTags(new HashSet<>(this.getTags()));
            }
        }
    }
}
