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
public class Report extends Document {
    
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
}
