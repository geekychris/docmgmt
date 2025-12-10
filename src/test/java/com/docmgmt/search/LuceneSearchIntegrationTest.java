package com.docmgmt.search;

import com.docmgmt.model.Content;
import com.docmgmt.model.Report;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LuceneSearchIntegrationTest {
    
    @Autowired
    private LuceneIndexService searchService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private ContentService contentService;
    
    private Report testDoc1;
    private Report testDoc2;
    private Report testDoc3;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create test documents with different content
        testDoc1 = Report.builder()
                .name("Spring Framework Guide")
                .description("Comprehensive guide to Spring Boot applications")
                .keywords("spring boot java framework")
                .tags(new HashSet<>(List.of("java", "spring", "tutorial")))
                .build();
        testDoc1 = (Report) documentService.save(testDoc1);
        
        // Add indexable content to testDoc1
        Content textContent1 = Content.builder()
                .name("content1.txt")
                .contentType("text/plain")
                .content("Spring Framework is a powerful Java framework for building enterprise applications.".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDoc1)
                .isPrimary(true)
                .isIndexable(true)
                .build();
        contentService.save(textContent1);
        
        testDoc2 = Report.builder()
                .name("Python Programming")
                .description("Learn Python from scratch")
                .keywords("python programming scripting")
                .tags(new HashSet<>(List.of("python", "beginner", "tutorial")))
                .build();
        testDoc2 = (Report) documentService.save(testDoc2);
        
        // Add indexable content to testDoc2
        Content textContent2 = Content.builder()
                .name("content2.txt")
                .contentType("text/plain")
                .content("Python is a versatile programming language used for web development and data science.".getBytes(StandardCharsets.UTF_8))
                .sysObject(testDoc2)
                .isPrimary(true)
                .isIndexable(true)
                .build();
        contentService.save(textContent2);
        
        testDoc3 = Report.builder()
                .name("Database Design")
                .description("SQL and NoSQL database design principles")
                .keywords("database sql nosql design")
                .tags(new HashSet<>(List.of("database", "sql", "design")))
                .build();
        testDoc3 = (Report) documentService.save(testDoc3);
        
        // Rebuild index with only test documents (clears existing index)
        List<com.docmgmt.model.Document> testDocuments = List.of(testDoc1, testDoc2, testDoc3);
        searchService.rebuildIndex(testDocuments);
    }
    
    @Test
    void testSimpleSearch() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("spring", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getDocumentId().equals(testDoc1.getId())));
    }
    
    @Test
    void testSearchInName() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("Framework", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertEquals(testDoc1.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testSearchInDescription() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("comprehensive", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertEquals(testDoc1.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testSearchInKeywords() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("programming", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getDocumentId().equals(testDoc2.getId())));
    }
    
    @Test
    void testSearchInTags() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("tutorial", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getDocumentId().equals(testDoc1.getId())));
        assertTrue(results.stream().anyMatch(r -> r.getDocumentId().equals(testDoc2.getId())));
    }
    
    @Test
    void testSearchInIndexableContent() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("enterprise", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertEquals(testDoc1.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testFieldSpecificSearchName() throws IOException, ParseException {
        Map<String, String> fieldQueries = new HashMap<>();
        fieldQueries.put(LuceneIndexService.FIELD_NAME, "Python");
        
        SearchResultsWrapper wrapper = searchService.searchFields(fieldQueries, 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertEquals(1, results.size());
        assertEquals(testDoc2.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testFieldSpecificSearchMultipleFields() throws IOException, ParseException {
        Map<String, String> fieldQueries = new HashMap<>();
        fieldQueries.put(LuceneIndexService.FIELD_NAME, "Database");
        fieldQueries.put(LuceneIndexService.FIELD_KEYWORDS, "sql");
        
        SearchResultsWrapper wrapper = searchService.searchFields(fieldQueries, 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertEquals(1, results.size());
        assertEquals(testDoc3.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testFieldSpecificSearchWithOROperator() throws IOException, ParseException {
        Map<String, String> fieldQueries = new HashMap<>();
        fieldQueries.put(LuceneIndexService.FIELD_NAME, "Spring");
        fieldQueries.put(LuceneIndexService.FIELD_NAME, "Python");
        
        SearchResultsWrapper wrapper = searchService.searchFieldsWithOperator(
                fieldQueries, 
                org.apache.lucene.search.BooleanClause.Occur.SHOULD, 
                10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
    }
    
    @Test
    void testMultiFieldSearch() throws IOException, ParseException {
        String[] fields = {LuceneIndexService.FIELD_NAME, LuceneIndexService.FIELD_DESCRIPTION};
        SearchResultsWrapper wrapper = searchService.searchMultipleFields("guide", fields, 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertEquals(testDoc1.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testSearchNoResults() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("nonexistent", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testRemoveDocument() throws IOException, ParseException {
        // First, verify document is in index
        SearchResultsWrapper beforeWrapper = searchService.search("Python", 10);
        List<SearchResult> beforeResults = beforeWrapper.getResults();
        assertTrue(beforeResults.stream().anyMatch(r -> r.getDocumentId().equals(testDoc2.getId())));
        
        // Remove document from index
        searchService.removeDocument(testDoc2.getId());
        
        // Verify document is no longer in results
        SearchResultsWrapper afterWrapper = searchService.search("Python", 10);
        List<SearchResult> afterResults = afterWrapper.getResults();
        assertFalse(afterResults.stream().anyMatch(r -> r.getDocumentId().equals(testDoc2.getId())));
    }
    
    @Test
    void testRebuildIndex() throws IOException {
        // Clear and rebuild index
        List<com.docmgmt.model.Document> allDocs = List.of(testDoc1, testDoc2, testDoc3);
        searchService.rebuildIndex(allDocs);
        
        // Verify index statistics
        Map<String, Object> stats = searchService.getIndexStats();
        
        assertNotNull(stats);
        assertTrue((Integer) stats.get("documentCount") >= 3);
    }
    
    @Test
    void testIndexStats() throws IOException {
        Map<String, Object> stats = searchService.getIndexStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("documentCount"));
        assertTrue(stats.containsKey("maxDoc"));
        assertTrue(stats.containsKey("deletedDocs"));
        
        assertTrue((Integer) stats.get("documentCount") >= 0);
    }
    
    @Test
    void testSearchResultContainsMetadata() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("Spring", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        SearchResult result = results.get(0);
        
        assertNotNull(result.getDocumentId());
        assertTrue(result.getScore() > 0);
        assertNotNull(result.getName());
    }
    
    @Test
    void testUpdateDocument() throws IOException, ParseException {
        // Update document
        testDoc1.setName("Updated Spring Framework Guide");
        testDoc1 = (Report) documentService.save(testDoc1);
        
        // Reindex
        searchService.indexDocument(testDoc1);
        
        // Search for updated content
        SearchResultsWrapper wrapper = searchService.search("Updated", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertEquals(testDoc1.getId(), results.get(0).getDocumentId());
    }
    
    @Test
    void testWildcardSearch() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("prog*", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getDocumentId().equals(testDoc2.getId())));
    }
    
    @Test
    void testPhraseSearch() throws IOException, ParseException {
        SearchResultsWrapper wrapper = searchService.search("\"Spring Framework\"", 10);
        List<SearchResult> results = wrapper.getResults();
        
        assertFalse(results.isEmpty());
        assertEquals(testDoc1.getId(), results.get(0).getDocumentId());
    }
}
