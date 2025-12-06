package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Manual entity - extends Document
 * Represents user manuals, technical documentation, and guides
 */
@Entity
@Table(name = "manual")
@DiscriminatorValue("MANUAL")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Manual extends Document {
    
    {
        setDocumentType(DocumentType.MANUAL);
    }
    
    public Manual() {
        super();
    }
    
    @Column(name = "manual_version")
    private String manualVersion;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "last_review_date")
    private LocalDate lastReviewDate;
    
    @Column(name = "target_audience")
    private String targetAudience;
    
    @PostLoad
    protected void onLoad() {
        if (getDocumentType() == null) {
            setDocumentType(DocumentType.MANUAL);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        setDocumentType(DocumentType.MANUAL);
    }
    
    /**
     * Check if manual needs review (older than 1 year)
     * @return true if review is needed
     */
    public boolean needsReview() {
        if (lastReviewDate == null) {
            return true;
        }
        return lastReviewDate.plusYears(1).isBefore(LocalDate.now());
    }
    
    /**
     * Get full manual identifier
     * @return formatted identifier with product and version
     */
    public String getManualIdentifier() {
        StringBuilder identifier = new StringBuilder();
        if (productName != null) {
            identifier.append(productName);
        }
        if (manualVersion != null) {
            if (identifier.length() > 0) {
                identifier.append(" - ");
            }
            identifier.append("v").append(manualVersion);
        }
        return identifier.toString();
    }
    
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Manual) {
            Manual manualTarget = (Manual) target;
            manualTarget.setManualVersion(this.getManualVersion());
            manualTarget.setProductName(this.getProductName());
            manualTarget.setLastReviewDate(this.getLastReviewDate());
            manualTarget.setTargetAudience(this.getTargetAudience());
        }
    }
}
