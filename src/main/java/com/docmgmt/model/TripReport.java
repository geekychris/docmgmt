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
public class TripReport extends Document implements DocumentFieldExtractor {
    
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
    
    @Override
    public String getFieldExtractionRules() {
        return """
            
            ADDITIONAL REQUIRED FIELDS FOR TRIP_REPORT TYPE (include these in your JSON):
            - destination: Trip destination city/location (string)
            - tripStartDate: Trip start date in YYYY-MM-DD format (string)
            - tripEndDate: Trip end date in YYYY-MM-DD format (string)
            - purpose: Purpose or objective of the business trip (string)
            - budgetAmount: Budgeted amount as a number without currency symbols (number)
            - actualAmount: Actual spent amount as a number without currency symbols (number)
            - attendees: Array of names of people who attended/traveled (array of strings)
            - summary: Brief summary of trip outcomes and activities (string)
            - followUpActions: Required follow-up actions or next steps (string)
            
            Extract these from the report body, especially the header/intro and conclusion sections.
            For amounts, extract only numeric values (e.g., 5000 not "$5,000").
            """;
    }
    
    @Override
    public java.util.Map<String, Object> getCurrentFieldValues() {
        java.util.Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("destination", destination);
        fields.put("tripStartDate", tripStartDate);
        fields.put("tripEndDate", tripEndDate);
        fields.put("purpose", purpose);
        fields.put("budgetAmount", budgetAmount);
        fields.put("actualAmount", actualAmount);
        fields.put("attendees", attendees);
        fields.put("summary", summary);
        fields.put("followUpActions", followUpActions);
        return fields;
    }
    
    @Override
    public void applyExtractedFields(java.util.Map<String, Object> extractedFields) {
        if (extractedFields.containsKey("destination")) {
            this.destination = (String) extractedFields.get("destination");
        }
        if (extractedFields.containsKey("tripStartDate")) {
            Object dateValue = extractedFields.get("tripStartDate");
            if (dateValue instanceof String) {
                try {
                    this.tripStartDate = java.time.LocalDate.parse((String) dateValue);
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
        }
        if (extractedFields.containsKey("tripEndDate")) {
            Object dateValue = extractedFields.get("tripEndDate");
            if (dateValue instanceof String) {
                try {
                    this.tripEndDate = java.time.LocalDate.parse((String) dateValue);
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
        }
        if (extractedFields.containsKey("purpose")) {
            this.purpose = (String) extractedFields.get("purpose");
        }
        if (extractedFields.containsKey("budgetAmount")) {
            Object valueObj = extractedFields.get("budgetAmount");
            if (valueObj instanceof Number) {
                this.budgetAmount = ((Number) valueObj).doubleValue();
            } else if (valueObj instanceof String) {
                try {
                    this.budgetAmount = Double.parseDouble((String) valueObj);
                } catch (Exception e) {
                    // Invalid number, skip
                }
            }
        }
        if (extractedFields.containsKey("actualAmount")) {
            Object valueObj = extractedFields.get("actualAmount");
            if (valueObj instanceof Number) {
                this.actualAmount = ((Number) valueObj).doubleValue();
            } else if (valueObj instanceof String) {
                try {
                    this.actualAmount = Double.parseDouble((String) valueObj);
                } catch (Exception e) {
                    // Invalid number, skip
                }
            }
        }
        if (extractedFields.containsKey("attendees")) {
            Object attendeesValue = extractedFields.get("attendees");
            if (attendeesValue instanceof java.util.Collection) {
                this.attendees = new HashSet<>();
                for (Object attendee : (java.util.Collection<?>) attendeesValue) {
                    this.attendees.add(attendee.toString());
                }
            }
        }
        if (extractedFields.containsKey("summary")) {
            this.summary = (String) extractedFields.get("summary");
        }
        if (extractedFields.containsKey("followUpActions")) {
            this.followUpActions = (String) extractedFields.get("followUpActions");
        }
    }
}
