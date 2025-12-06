package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * TripReport entity - extends Document
 * Represents a business trip report with specific attributes
 */
@Entity
@Table(name = "trip_report")
@DiscriminatorValue("TRIP_REPORT")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TripReport extends Document {
    
    {
        setDocumentType(DocumentType.TRIP_REPORT);
    }
    
    @Column(name = "destination")
    private String destination;
    
    @Column(name = "trip_start_date")
    private LocalDate tripStartDate;
    
    @Column(name = "trip_end_date")
    private LocalDate tripEndDate;
    
    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;
    
    @Column(name = "budget_amount")
    private Double budgetAmount;
    
    @Column(name = "actual_amount")
    private Double actualAmount;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_report_attendees", joinColumns = @JoinColumn(name = "trip_report_id"))
    @Column(name = "attendee")
    @Builder.Default
    private Set<String> attendees = new HashSet<>();
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "follow_up_actions", columnDefinition = "TEXT")
    private String followUpActions;
    
    public TripReport() {
        super();
    }
    
    @PostLoad
    protected void onLoad() {
        if (getDocumentType() == null) {
            setDocumentType(DocumentType.TRIP_REPORT);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        setDocumentType(DocumentType.TRIP_REPORT);
    }
    
    /**
     * Add an attendee to the trip report
     * @param attendee The attendee name to add
     * @return this trip report for method chaining
     */
    public TripReport addAttendee(String attendee) {
        if (attendees == null) {
            attendees = new HashSet<>();
        }
        attendees.add(attendee);
        return this;
    }
    
    /**
     * Remove an attendee from the trip report
     * @param attendee The attendee name to remove
     * @return this trip report for method chaining
     */
    public TripReport removeAttendee(String attendee) {
        if (attendees != null) {
            attendees.remove(attendee);
        }
        return this;
    }
    
    /**
     * Calculate the trip duration in days
     * @return number of days, or 0 if dates are not set
     */
    public long getTripDurationDays() {
        if (tripStartDate != null && tripEndDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(tripStartDate, tripEndDate) + 1;
        }
        return 0;
    }
    
    /**
     * Calculate budget variance
     * @return difference between actual and budget (positive = over budget)
     */
    public Double getBudgetVariance() {
        if (budgetAmount != null && actualAmount != null) {
            return actualAmount - budgetAmount;
        }
        return null;
    }
    
    /**
     * Check if trip is over budget
     * @return true if over budget, false otherwise
     */
    public boolean isOverBudget() {
        Double variance = getBudgetVariance();
        return variance != null && variance > 0;
    }
    
    /**
     * Copy attributes to target entity
     * Overrides the method in Document to include TripReport-specific attributes
     * @param target The target entity
     */
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof TripReport) {
            TripReport reportTarget = (TripReport) target;
            reportTarget.setDestination(this.getDestination());
            reportTarget.setTripStartDate(this.getTripStartDate());
            reportTarget.setTripEndDate(this.getTripEndDate());
            reportTarget.setPurpose(this.getPurpose());
            reportTarget.setBudgetAmount(this.getBudgetAmount());
            reportTarget.setActualAmount(this.getActualAmount());
            reportTarget.setSummary(this.getSummary());
            reportTarget.setFollowUpActions(this.getFollowUpActions());
            
            // Copy attendees
            if (this.getAttendees() != null && !this.getAttendees().isEmpty()) {
                reportTarget.setAttendees(new HashSet<>(this.getAttendees()));
            }
        }
    }
}
