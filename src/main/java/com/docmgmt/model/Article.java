package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Article entity - extends Document
 * Represents academic or professional articles with publication details
 */
@Entity
@Table(name = "article")
@DiscriminatorValue("ARTICLE")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Article extends Document implements DocumentFieldExtractor {
    
    {
        setDocumentType(DocumentType.ARTICLE);
    }
    
    public Article() {
        super();
    }
    
    @Column(name = "publication_date")
    private LocalDate publicationDate;
    
    @Column(name = "journal")
    private String journal;
    
    @Column(name = "volume")
    private String volume;
    
    @Column(name = "issue")
    private String issue;
    
    @Column(name = "pages")
    private String pages;
    
    @Column(name = "doi")
    private String doi;
    
    /**
     * Initialize document type after loading from database
     */
    @PostLoad
    protected void onLoad() {
        if (getDocumentType() == null) {
            setDocumentType(DocumentType.ARTICLE);
        }
    }
    
    /**
     * Set document type before persisting
     */
    @PrePersist
    protected void onCreate() {
        setDocumentType(DocumentType.ARTICLE);
    }
    
    /**
     * Get citation in standard format
     * @return formatted citation string
     */
    public String getCitation() {
        StringBuilder citation = new StringBuilder();
        if (getAuthors() != null && !getAuthors().isEmpty()) {
            citation.append(getAuthors().stream()
                .map(User::getFullName)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
            citation.append(". ");
        }
        if (getName() != null) {
            citation.append("\"").append(getName()).append("\". ");
        }
        if (journal != null) {
            citation.append(journal);
            if (volume != null) {
                citation.append(" ").append(volume);
            }
            if (issue != null) {
                citation.append("(").append(issue).append(")");
            }
            if (pages != null) {
                citation.append(": ").append(pages);
            }
            citation.append(". ");
        }
        if (publicationDate != null) {
            citation.append(publicationDate.getYear()).append(".");
        }
        if (doi != null) {
            citation.append(" DOI: ").append(doi);
        }
        return citation.toString().trim();
    }
    
    /**
     * Copy attributes to target entity
     * @param target The target entity
     */
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Article) {
            Article articleTarget = (Article) target;
            articleTarget.setPublicationDate(this.getPublicationDate());
            articleTarget.setJournal(this.getJournal());
            articleTarget.setVolume(this.getVolume());
            articleTarget.setIssue(this.getIssue());
            articleTarget.setPages(this.getPages());
            articleTarget.setDoi(this.getDoi());
        }
    }
    
    @Override
    public String getFieldExtractionRules() {
        return """
            
            ADDITIONAL REQUIRED FIELDS FOR ARTICLE TYPE (include these in your JSON):
            - journal: Name of the journal or publication (string)
            - volume: Volume number (string)
            - issue: Issue number (string)  
            - pages: Page range like "10-25" (string)
            - doi: Digital Object Identifier if available (string)
            - publicationDate: Publication date in YYYY-MM-DD format (string)
            
            Extract these fields from the content when available, or provide reasonable inferences.
            """;
    }
    
    @Override
    public java.util.Map<String, Object> getCurrentFieldValues() {
        java.util.Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("journal", journal);
        fields.put("volume", volume);
        fields.put("issue", issue);
        fields.put("pages", pages);
        fields.put("doi", doi);
        fields.put("publicationDate", publicationDate);
        return fields;
    }
    
    @Override
    public void applyExtractedFields(java.util.Map<String, Object> extractedFields) {
        if (extractedFields.containsKey("journal")) {
            this.journal = (String) extractedFields.get("journal");
        }
        if (extractedFields.containsKey("volume")) {
            this.volume = (String) extractedFields.get("volume");
        }
        if (extractedFields.containsKey("issue")) {
            this.issue = (String) extractedFields.get("issue");
        }
        if (extractedFields.containsKey("pages")) {
            this.pages = (String) extractedFields.get("pages");
        }
        if (extractedFields.containsKey("doi")) {
            this.doi = (String) extractedFields.get("doi");
        }
        if (extractedFields.containsKey("publicationDate")) {
            Object dateValue = extractedFields.get("publicationDate");
            if (dateValue instanceof String) {
                try {
                    this.publicationDate = java.time.LocalDate.parse((String) dateValue);
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
        }
    }
}
