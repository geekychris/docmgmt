package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Contract entity - extends Document
 * Represents legal contracts and agreements
 */
@Entity
@Table(name = "contract")
@DiscriminatorValue("CONTRACT")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Contract extends Document {
    
    {
        setDocumentType(DocumentType.CONTRACT);
    }
    
    public Contract() {
        super();
    }
    
    @Column(name = "contract_number")
    private String contractNumber;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contract_parties", joinColumns = @JoinColumn(name = "contract_id"))
    @Column(name = "party")
    @Builder.Default
    private Set<String> parties = new HashSet<>();
    
    @Column(name = "contract_value")
    private Double contractValue;
    
    @PostLoad
    protected void onLoad() {
        if (getDocumentType() == null) {
            setDocumentType(DocumentType.CONTRACT);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        setDocumentType(DocumentType.CONTRACT);
    }
    
    /**
     * Add a party to the contract
     * @param party The party name to add
     * @return this contract for method chaining
     */
    public Contract addParty(String party) {
        if (parties == null) {
            parties = new HashSet<>();
        }
        parties.add(party);
        return this;
    }
    
    /**
     * Remove a party from the contract
     * @param party The party name to remove
     * @return this contract for method chaining
     */
    public Contract removeParty(String party) {
        if (parties != null) {
            parties.remove(party);
        }
        return this;
    }
    
    /**
     * Check if contract is currently active
     * @return true if within effective dates
     */
    public boolean isActive() {
        LocalDate now = LocalDate.now();
        boolean afterStart = effectiveDate == null || !now.isBefore(effectiveDate);
        boolean beforeEnd = expirationDate == null || !now.isAfter(expirationDate);
        return afterStart && beforeEnd;
    }
    
    /**
     * Check if contract has expired
     * @return true if past expiration date
     */
    public boolean isExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }
    
    /**
     * Get remaining days until expiration
     * @return days until expiration, or null if no expiration date
     */
    public Long getDaysUntilExpiration() {
        if (expirationDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
        }
        return null;
    }
    
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Contract) {
            Contract contractTarget = (Contract) target;
            contractTarget.setContractNumber(this.getContractNumber());
            contractTarget.setEffectiveDate(this.getEffectiveDate());
            contractTarget.setExpirationDate(this.getExpirationDate());
            contractTarget.setContractValue(this.getContractValue());
            
            if (this.getParties() != null && !this.getParties().isEmpty()) {
                contractTarget.setParties(new HashSet<>(this.getParties()));
            }
        }
    }
}
