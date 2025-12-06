package com.docmgmt.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test document subclasses
 */
class DocumentSubclassesTest {

    @Test
    void testArticleCreation() {
        Article article = Article.builder()
            .name("Test Article")
            .description("Test Description")
            .author("John Doe")
            .publicationDate(LocalDate.of(2024, 1, 1))
            .journal("Nature")
            .volume("100")
            .issue("5")
            .pages("123-456")
            .doi("10.1000/test")
            .build();
        
        assertNotNull(article);
        assertEquals("Test Article", article.getName());
        assertEquals(Document.DocumentType.ARTICLE, article.getDocumentType());
        assertEquals("Nature", article.getJournal());
        assertNotNull(article.getCitation());
        assertTrue(article.getCitation().contains("Nature"));
    }

    @Test
    void testReportCreation() {
        Report report = Report.builder()
            .name("Annual Report")
            .description("Annual financial report")
            .reportDate(LocalDate.now())
            .reportNumber("RPT-2024-001")
            .department("Finance")
            .confidentialityLevel("CONFIDENTIAL")
            .build();
        
        assertNotNull(report);
        assertEquals("Annual Report", report.getName());
        assertEquals(Document.DocumentType.REPORT, report.getDocumentType());
        assertEquals("RPT-2024-001", report.getReportNumber());
        assertTrue(report.isConfidential());
    }

    @Test
    void testContractCreation() {
        Contract contract = Contract.builder()
            .name("Service Contract")
            .description("Vendor service agreement")
            .contractNumber("CNT-2024-001")
            .effectiveDate(LocalDate.now())
            .expirationDate(LocalDate.now().plusYears(1))
            .contractValue(100000.0)
            .build();
        
        contract.addParty("Company A");
        contract.addParty("Company B");
        
        assertNotNull(contract);
        assertEquals("Service Contract", contract.getName());
        assertEquals(Document.DocumentType.CONTRACT, contract.getDocumentType());
        assertEquals("CNT-2024-001", contract.getContractNumber());
        assertEquals(2, contract.getParties().size());
        assertTrue(contract.isActive());
        assertFalse(contract.isExpired());
        assertNotNull(contract.getDaysUntilExpiration());
    }

    @Test
    void testManualCreation() {
        Manual manual = Manual.builder()
            .name("User Manual")
            .description("Product user guide")
            .manualVersion("2.1")
            .productName("Product X")
            .lastReviewDate(LocalDate.now().minusMonths(6))
            .targetAudience("End Users")
            .build();
        
        assertNotNull(manual);
        assertEquals("User Manual", manual.getName());
        assertEquals(Document.DocumentType.MANUAL, manual.getDocumentType());
        assertEquals("2.1", manual.getManualVersion());
        assertEquals("Product X - v2.1", manual.getManualIdentifier());
        assertFalse(manual.needsReview()); // Less than 1 year old
    }

    @Test
    void testPresentationCreation() {
        Presentation presentation = Presentation.builder()
            .name("Quarterly Review")
            .description("Q4 business review")
            .presentationDate(LocalDate.now().plusDays(7))
            .venue("Conference Room A")
            .audience("Executives")
            .durationMinutes(90)
            .build();
        
        assertNotNull(presentation);
        assertEquals("Quarterly Review", presentation.getName());
        assertEquals(Document.DocumentType.PRESENTATION, presentation.getDocumentType());
        assertTrue(presentation.isUpcoming());
        assertEquals("1h 30m", presentation.getFormattedDuration());
    }

    @Test
    void testTripReportWithDocumentType() {
        TripReport tripReport = TripReport.builder()
            .name("Business Trip Report")
            .description("Trip to client site")
            .destination("New York")
            .tripStartDate(LocalDate.of(2024, 1, 15))
            .tripEndDate(LocalDate.of(2024, 1, 18))
            .budgetAmount(2000.0)
            .actualAmount(1850.0)
            .build();
        
        assertNotNull(tripReport);
        assertEquals("Business Trip Report", tripReport.getName());
        assertEquals(Document.DocumentType.TRIP_REPORT, tripReport.getDocumentType());
        assertEquals(4L, tripReport.getTripDurationDays());
        assertFalse(tripReport.isOverBudget());
    }

    @Test
    void testContractPartyManagement() {
        Contract contract = Contract.builder()
            .name("Test Contract")
            .build();
        
        contract.addParty("Party A");
        contract.addParty("Party B");
        assertEquals(2, contract.getParties().size());
        
        contract.removeParty("Party A");
        assertEquals(1, contract.getParties().size());
        assertTrue(contract.getParties().contains("Party B"));
    }

    @Test
    void testManualReviewStatus() {
        Manual oldManual = Manual.builder()
            .name("Old Manual")
            .lastReviewDate(LocalDate.now().minusYears(2))
            .build();
        
        assertTrue(oldManual.needsReview());
        
        Manual newManual = Manual.builder()
            .name("New Manual")
            .lastReviewDate(LocalDate.now())
            .build();
        
        assertFalse(newManual.needsReview());
    }

    @Test
    void testReportConfidentiality() {
        Report publicReport = Report.builder()
            .name("Public Report")
            .confidentialityLevel("PUBLIC")
            .build();
        
        assertFalse(publicReport.isConfidential());
        
        Report confidentialReport = Report.builder()
            .name("Secret Report")
            .confidentialityLevel("CONFIDENTIAL")
            .build();
        
        assertTrue(confidentialReport.isConfidential());
    }
}
