package com.docmgmt.search;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.service.ContentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lucene-based full-text search service for documents
 * Indexes all document fields and indexable content
 */
@Service
public class LuceneIndexService {
    
    private static final Logger logger = LoggerFactory.getLogger(LuceneIndexService.class);
    
    // Field names for Lucene index
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_KEYWORDS = "keywords";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_ALL = "all";  // Combined field for cross-field search
    
    @Value("${lucene.index.directory:./lucene_index}")
    private String indexDirectoryPath;
    
    private final ContentService contentService;
    
    private Directory directory;
    private StandardAnalyzer analyzer;
    private IndexWriter indexWriter;
    
    @Autowired
    public LuceneIndexService(ContentService contentService) {
        this.contentService = contentService;
    }
    
    @PostConstruct
    public void initialize() throws IOException {
        Path indexPath = Paths.get(indexDirectoryPath);
        Files.createDirectories(indexPath);
        
        directory = FSDirectory.open(indexPath);
        analyzer = new StandardAnalyzer();
        
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        
        indexWriter = new IndexWriter(directory, config);
        
        logger.info("Lucene index initialized at: {}", indexPath.toAbsolutePath());
    }
    
    @PreDestroy
    public void shutdown() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
        if (directory != null) {
            directory.close();
        }
        logger.info("Lucene index closed");
    }
    
    /**
     * Index a document with all its fields and indexable content
     * @param document the document to index
     * @throws IOException if indexing fails
     */
    public void indexDocument(Document document) throws IOException {
        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
        
        // Store document ID (not analyzed, just stored)
        luceneDoc.add(new LongPoint(FIELD_ID, document.getId()));
        luceneDoc.add(new StoredField(FIELD_ID, document.getId()));
        
        // Index and store all document fields
        StringBuilder allText = new StringBuilder();
        
        // Name field
        if (document.getName() != null) {
            luceneDoc.add(new TextField(FIELD_NAME, document.getName(), Field.Store.YES));
            allText.append(document.getName()).append(" ");
        }
        
        // Description field
        if (document.getDescription() != null) {
            luceneDoc.add(new TextField(FIELD_DESCRIPTION, document.getDescription(), Field.Store.YES));
            allText.append(document.getDescription()).append(" ");
        }
        
        // Keywords field
        if (document.getKeywords() != null) {
            luceneDoc.add(new TextField(FIELD_KEYWORDS, document.getKeywords(), Field.Store.YES));
            allText.append(document.getKeywords()).append(" ");
        }
        
        // Tags field (concatenated)
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            String tagsText = String.join(" ", document.getTags());
            luceneDoc.add(new TextField(FIELD_TAGS, tagsText, Field.Store.YES));
            allText.append(tagsText).append(" ");
        }
        
        // Index all indexable content
        List<Content> indexableContent = contentService.getIndexableContent(document);
        for (Content content : indexableContent) {
            try {
                byte[] contentBytes = content.getContentBytes();
                String contentText = new String(contentBytes, StandardCharsets.UTF_8);
                luceneDoc.add(new TextField(FIELD_CONTENT, contentText, Field.Store.NO));
                allText.append(contentText).append(" ");
            } catch (Exception e) {
                logger.warn("Failed to index content {} for document {}: {}", 
                    content.getName(), document.getId(), e.getMessage());
            }
        }
        
        // Add combined "all" field for cross-field search
        luceneDoc.add(new TextField(FIELD_ALL, allText.toString(), Field.Store.NO));
        
        // Delete any existing document with this ID and add the new one
        indexWriter.deleteDocuments(LongPoint.newExactQuery(FIELD_ID, document.getId()));
        indexWriter.addDocument(luceneDoc);
        indexWriter.commit();
        
        logger.debug("Indexed document: {} (ID: {})", document.getName(), document.getId());
    }
    
    /**
     * Remove a document from the index
     * @param documentId the document ID to remove
     * @throws IOException if deletion fails
     */
    public void removeDocument(Long documentId) throws IOException {
        indexWriter.deleteDocuments(LongPoint.newExactQuery(FIELD_ID, documentId));
        indexWriter.commit();
        
        logger.debug("Removed document from index: ID {}", documentId);
    }
    
    /**
     * Search across all fields
     * @param queryText the search query
     * @param maxResults maximum number of results to return
     * @return list of matching document IDs
     * @throws IOException if search fails
     * @throws ParseException if query parsing fails
     */
    public List<SearchResult> search(String queryText, int maxResults) throws IOException, ParseException {
        QueryParser parser = new QueryParser(FIELD_ALL, analyzer);
        Query query = parser.parse(queryText);
        
        return executeSearch(query, maxResults);
    }
    
    /**
     * Search in specific fields
     * @param fieldQueries map of field name to query text
     * @param maxResults maximum number of results to return
     * @return list of matching document IDs
     * @throws IOException if search fails
     * @throws ParseException if query parsing fails
     */
    public List<SearchResult> searchFields(Map<String, String> fieldQueries, int maxResults) 
            throws IOException, ParseException {
        
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        
        for (Map.Entry<String, String> entry : fieldQueries.entrySet()) {
            String field = entry.getKey();
            String queryText = entry.getValue();
            
            if (queryText != null && !queryText.trim().isEmpty()) {
                QueryParser parser = new QueryParser(field, analyzer);
                Query fieldQuery = parser.parse(queryText);
                queryBuilder.add(fieldQuery, BooleanClause.Occur.MUST);
            }
        }
        
        return executeSearch(queryBuilder.build(), maxResults);
    }
    
    /**
     * Advanced search with field-specific queries and boolean logic
     * @param fieldQueries map of field name to query text
     * @param operator AND or OR logic between fields
     * @param maxResults maximum number of results to return
     * @return list of matching document IDs
     * @throws IOException if search fails
     * @throws ParseException if query parsing fails
     */
    public List<SearchResult> searchFieldsWithOperator(Map<String, String> fieldQueries, 
                                                        BooleanClause.Occur operator,
                                                        int maxResults) throws IOException, ParseException {
        
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        
        for (Map.Entry<String, String> entry : fieldQueries.entrySet()) {
            String field = entry.getKey();
            String queryText = entry.getValue();
            
            if (queryText != null && !queryText.trim().isEmpty()) {
                QueryParser parser = new QueryParser(field, analyzer);
                Query fieldQuery = parser.parse(queryText);
                queryBuilder.add(fieldQuery, operator);
            }
        }
        
        return executeSearch(queryBuilder.build(), maxResults);
    }
    
    /**
     * Multi-field search (searches same query across multiple fields)
     * @param queryText the search query
     * @param fields fields to search in
     * @param maxResults maximum number of results to return
     * @return list of matching document IDs
     * @throws IOException if search fails
     * @throws ParseException if query parsing fails
     */
    public List<SearchResult> searchMultipleFields(String queryText, String[] fields, int maxResults) 
            throws IOException, ParseException {
        
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query query = parser.parse(queryText);
        
        return executeSearch(query, maxResults);
    }
    
    /**
     * Execute a Lucene query and return results
     */
    private List<SearchResult> executeSearch(Query query, int maxResults) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            TopDocs topDocs = searcher.search(query, maxResults);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                
                Long documentId = doc.getField(FIELD_ID).numericValue().longValue();
                float score = scoreDoc.score;
                
                // Extract stored fields for preview
                String name = doc.get(FIELD_NAME);
                String description = doc.get(FIELD_DESCRIPTION);
                String keywords = doc.get(FIELD_KEYWORDS);
                String tags = doc.get(FIELD_TAGS);
                
                SearchResult result = new SearchResult(documentId, score, name, description, keywords, tags);
                results.add(result);
            }
        }
        
        logger.debug("Search query '{}' returned {} results", query, results.size());
        
        return results;
    }
    
    /**
     * Rebuild the entire index by reindexing all documents
     * @param documents list of all documents to index
     * @throws IOException if indexing fails
     */
    public void rebuildIndex(List<Document> documents) throws IOException {
        // Clear the index
        indexWriter.deleteAll();
        indexWriter.commit();
        
        // Index all documents
        for (Document document : documents) {
            try {
                indexDocument(document);
            } catch (Exception e) {
                logger.error("Failed to index document {} during rebuild: {}", 
                    document.getId(), e.getMessage());
            }
        }
        
        logger.info("Index rebuilt with {} documents", documents.size());
    }
    
    /**
     * Get index statistics
     * @return map of statistics
     * @throws IOException if operation fails
     */
    public Map<String, Object> getIndexStats() throws IOException {
        Map<String, Object> stats = new HashMap<>();
        
        try (IndexReader reader = DirectoryReader.open(directory)) {
            stats.put("documentCount", reader.numDocs());
            stats.put("maxDoc", reader.maxDoc());
            stats.put("deletedDocs", reader.numDeletedDocs());
        }
        
        return stats;
    }
}
