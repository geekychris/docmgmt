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
public class Article extends Document {
    
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
        if (getAuthor() != null) {
            citation.append(getAuthor()).append(". ");
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
}
