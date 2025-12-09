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
public class Manual extends Document implements DocumentFieldExtractor {
    
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
    
    @Override
    public String getFieldExtractionRules() {
        return """
            
            ADDITIONAL REQUIRED FIELDS FOR MANUAL TYPE (include these in your JSON):
            - manualVersion: Version number like "1.0", "2.3" (string)
            - productName: Name of the product or system being documented (string)
            - lastReviewDate: When manual was last reviewed in YYYY-MM-DD format (string)
            - targetAudience: Who should use this manual, like "End Users", "Administrators", "Developers" (string)
            
            Extract these fields from the content when available, or provide reasonable inferences based on the document context.
            """;
    }
    
    @Override
    public java.util.Map<String, Object> getCurrentFieldValues() {
        java.util.Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("manualVersion", manualVersion);
        fields.put("productName", productName);
        fields.put("lastReviewDate", lastReviewDate);
        fields.put("targetAudience", targetAudience);
        return fields;
    }
    
    @Override
    public void applyExtractedFields(java.util.Map<String, Object> extractedFields) {
        if (extractedFields.containsKey("manualVersion")) {
            this.manualVersion = (String) extractedFields.get("manualVersion");
        }
        if (extractedFields.containsKey("productName")) {
            this.productName = (String) extractedFields.get("productName");
        }
        if (extractedFields.containsKey("lastReviewDate")) {
            Object dateValue = extractedFields.get("lastReviewDate");
            if (dateValue instanceof String) {
                try {
                    this.lastReviewDate = java.time.LocalDate.parse((String) dateValue);
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
        }
        if (extractedFields.containsKey("targetAudience")) {
            this.targetAudience = (String) extractedFields.get("targetAudience");
        }
    }
}
