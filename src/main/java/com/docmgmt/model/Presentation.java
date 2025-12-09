package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Presentation entity - extends Document
 * Represents slides, keynotes, and presentation materials
 */
@Entity
@Table(name = "presentation")
@DiscriminatorValue("PRESENTATION")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Presentation extends Document implements DocumentFieldExtractor {
    
    {
        setDocumentType(DocumentType.PRESENTATION);
    }
    
    public Presentation() {
        super();
    }
    
    @Column(name = "presentation_date")
    private LocalDate presentationDate;
    
    @Column(name = "venue")
    private String venue;
    
    @Column(name = "audience")
    private String audience;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @PostLoad
    protected void onLoad() {
        if (getDocumentType() == null) {
            setDocumentType(DocumentType.PRESENTATION);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        setDocumentType(DocumentType.PRESENTATION);
    }
    
    /**
     * Get formatted duration string
     * @return duration in hours and minutes format
     */
    public String getFormattedDuration() {
        if (durationMinutes == null) {
            return "Unknown duration";
        }
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + " minutes";
    }
    
    /**
     * Check if presentation is upcoming
     * @return true if presentation date is in the future
     */
    public boolean isUpcoming() {
        return presentationDate != null && presentationDate.isAfter(LocalDate.now());
    }
    
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Presentation) {
            Presentation presentationTarget = (Presentation) target;
            presentationTarget.setPresentationDate(this.getPresentationDate());
            presentationTarget.setVenue(this.getVenue());
            presentationTarget.setAudience(this.getAudience());
            presentationTarget.setDurationMinutes(this.getDurationMinutes());
        }
    }
    
    @Override
    public String getFieldExtractionRules() {
        return """
            
            ADDITIONAL REQUIRED FIELDS FOR PRESENTATION TYPE (include these in your JSON):
            - presentationDate: Date when presentation was/will be given in YYYY-MM-DD format (string)
            - venue: Location, conference, or event name where presented (string)
            - audience: Target audience description like "Technical Team", "Executives", "Conference Attendees" (string)
            - durationMinutes: Presentation length in minutes as a number (number)
            
            Extract these from the presentation title slide, footer, or context.
            For durationMinutes, estimate based on slide count if not explicit (e.g., ~1-2 minutes per slide).
            """;
    }
    
    @Override
    public java.util.Map<String, Object> getCurrentFieldValues() {
        java.util.Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("presentationDate", presentationDate);
        fields.put("venue", venue);
        fields.put("audience", audience);
        fields.put("durationMinutes", durationMinutes);
        return fields;
    }
    
    @Override
    public void applyExtractedFields(java.util.Map<String, Object> extractedFields) {
        if (extractedFields.containsKey("presentationDate")) {
            Object dateValue = extractedFields.get("presentationDate");
            if (dateValue instanceof String) {
                try {
                    this.presentationDate = java.time.LocalDate.parse((String) dateValue);
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
        }
        if (extractedFields.containsKey("venue")) {
            this.venue = (String) extractedFields.get("venue");
        }
        if (extractedFields.containsKey("audience")) {
            this.audience = (String) extractedFields.get("audience");
        }
        if (extractedFields.containsKey("durationMinutes")) {
            Object durationObj = extractedFields.get("durationMinutes");
            if (durationObj instanceof Number) {
                this.durationMinutes = ((Number) durationObj).intValue();
            } else if (durationObj instanceof String) {
                try {
                    this.durationMinutes = Integer.parseInt((String) durationObj);
                } catch (Exception e) {
                    // Invalid number format, skip
                }
            }
        }
    }
}
