package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Report entity - extends Document
 * Represents business, technical, or research reports
 */
@Entity
@Table(name = "report")
@DiscriminatorValue("REPORT")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Report extends Document implements DocumentFieldExtractor {
    
    {
        setDocumentType(DocumentType.REPORT);
    }
    
    public Report() {
        super();
    }
    
    @Column(name = "report_date")
    private LocalDate reportDate;
    
    @Column(name = "report_number")
    private String reportNumber;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "confidentiality_level")
    private String confidentialityLevel;
    
    @PostLoad
    protected void onLoad() {
        if (getDocumentType() == null) {
            setDocumentType(DocumentType.REPORT);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        setDocumentType(DocumentType.REPORT);
    }
    
    /**
     * Check if report is confidential
     * @return true if confidentiality level is set
     */
    public boolean isConfidential() {
        return confidentialityLevel != null && 
               !confidentialityLevel.equalsIgnoreCase("PUBLIC");
    }
    
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Report) {
            Report reportTarget = (Report) target;
            reportTarget.setReportDate(this.getReportDate());
            reportTarget.setReportNumber(this.getReportNumber());
            reportTarget.setDepartment(this.getDepartment());
            reportTarget.setConfidentialityLevel(this.getConfidentialityLevel());
        }
    }
    
    @Override
    public String getFieldExtractionRules() {
        return """
            
            ADDITIONAL REQUIRED FIELDS FOR REPORT TYPE (include these in your JSON):
            - reportNumber: Report identification number or reference code (string)
            - reportDate: Date the report was created/issued in YYYY-MM-DD format (string)
            - department: Department, division, or organization that created the report (string)
            - confidentialityLevel: Classification level like "PUBLIC", "CONFIDENTIAL", "RESTRICTED", "INTERNAL" (string)
            
            Extract these from the document content or headers. If not explicitly stated, infer reasonable values.
            For confidentialityLevel, default to "INTERNAL" if not mentioned.
            """;
    }
    
    @Override
    public java.util.Map<String, Object> getCurrentFieldValues() {
        java.util.Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("reportNumber", reportNumber);
        fields.put("reportDate", reportDate);
        fields.put("department", department);
        fields.put("confidentialityLevel", confidentialityLevel);
        return fields;
    }
    
    @Override
    public void applyExtractedFields(java.util.Map<String, Object> extractedFields) {
        if (extractedFields.containsKey("reportNumber")) {
            this.reportNumber = (String) extractedFields.get("reportNumber");
        }
        if (extractedFields.containsKey("reportDate")) {
            Object dateValue = extractedFields.get("reportDate");
            if (dateValue instanceof String) {
                try {
                    this.reportDate = java.time.LocalDate.parse((String) dateValue);
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
        }
        if (extractedFields.containsKey("department")) {
            this.department = (String) extractedFields.get("department");
        }
        if (extractedFields.containsKey("confidentialityLevel")) {
            this.confidentialityLevel = (String) extractedFields.get("confidentialityLevel");
        }
    }
}
