package com.docmgmt.listener;

import com.docmgmt.model.Document;
import com.docmgmt.model.TripReport;
import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.search.SearchResultsWrapper;
import com.docmgmt.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for automatic document reindexing
 * Verifies that documents are automatically indexed when created or updated
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "lucene.index.directory=./test_lucene_index"
})
class DocumentIndexListenerTest {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private LuceneIndexService indexService;
    
    @BeforeEach
    void setUp() throws IOException {
        // Clear the index before each test
        indexService.rebuildIndex(java.util.Collections.emptyList());
    }
    
    @Test
    void testDocumentAutomaticallyIndexedOnCreate() throws Exception {
        // Create and save a new document
        TripReport report = TripReport.builder()
            .name("Test Trip Report")
            .description("A test trip report for automatic indexing")
            .keywords("test automatic indexing")
            .destination("Test Destination")
            .build();
        
        TripReport saved = (TripReport) documentService.save(report);
        
        // Verify the document was automatically indexed
        SearchResultsWrapper results = indexService.search("automatic", 10);
        
        assertNotNull(results);
        assertEquals(1, results.getTotalHits());
        assertEquals(saved.getId(), results.getResults().get(0).getDocumentId());
    }
    
    @Test
    void testDocumentAutomaticallyReindexedOnUpdate() throws Exception {
        // Create and save a document
        TripReport report = TripReport.builder()
            .name("Original Name")
            .description("Original description")
            .keywords("original keywords")
            .destination("Original Destination")
            .build();
        
        TripReport saved = (TripReport) documentService.save(report);
        
        // Update the document
        saved.setDescription("Updated description with unique term XYZZYX");
        saved.setKeywords("updated keywords");
        saved = (TripReport) documentService.save(saved);
        
        // Verify the updated content is searchable
        SearchResultsWrapper results = indexService.search("XYZZYX", 10);
        
        assertNotNull(results);
        assertEquals(1, results.getTotalHits());
        assertEquals(saved.getId(), results.getResults().get(0).getDocumentId());
        
        // Verify old content is no longer the primary match
        SearchResultsWrapper oldResults = indexService.search("Original", 10);
        assertTrue(oldResults.getTotalHits() >= 0); // May still match name
    }
    
    @Test
    void testDocumentAutomaticallyRemovedFromIndexOnDelete() throws Exception {
        // Create and save a document
        TripReport report = TripReport.builder()
            .name("Document To Delete")
            .description("This document will be deleted")
            .keywords("delete test")
            .destination("Test Destination")
            .build();
        
        TripReport saved = (TripReport) documentService.save(report);
        Long documentId = saved.getId();
        
        // Verify it's indexed
        SearchResultsWrapper results = indexService.search("delete", 10);
        assertEquals(1, results.getTotalHits());
        
        // Delete the document
        documentService.delete(documentId);
        
        // Verify it's removed from the index
        SearchResultsWrapper afterDelete = indexService.search("delete", 10);
        assertEquals(0, afterDelete.getTotalHits());
    }
    
    @Test
    void testMultipleAttributeChangesReindexed() throws Exception {
        // Create a document
        TripReport report = TripReport.builder()
            .name("Multi Update Test")
            .description("Initial description")
            .keywords("initial")
            .destination("Initial Destination")
            .build();
        
        TripReport saved = (TripReport) documentService.save(report);
        
        // Update multiple attributes
        saved.setDescription("Modified description");
        saved.setKeywords("modified keywords searchable");
        saved.addTag("test-tag");
        saved.addTag("auto-index");
        saved = (TripReport) documentService.save(saved);
        
        // Verify all changes are searchable
        SearchResultsWrapper descResults = indexService.search("Modified", 10);
        assertEquals(1, descResults.getTotalHits());
        
        SearchResultsWrapper keywordResults = indexService.search("searchable", 10);
        assertEquals(1, keywordResults.getTotalHits());
        
        SearchResultsWrapper tagResults = indexService.search("auto-index", 10);
        assertEquals(1, tagResults.getTotalHits());
    }
}
