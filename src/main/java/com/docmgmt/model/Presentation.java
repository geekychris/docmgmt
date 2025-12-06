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
public class Presentation extends Document {
    
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
}
