package com.docmgmt.service;

import com.docmgmt.model.Document;
import com.docmgmt.model.DocumentEmbedding;
import com.docmgmt.repository.DocumentEmbeddingRepository;
import com.docmgmt.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for document similarity search using vector embeddings
 */
@Service
public class DocumentSimilarityService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentSimilarityService.class);
    private static final int MAX_CONTENT_LENGTH = 8000; // Limit for embedding generation
    
    private final EmbeddingModel embeddingModel;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final ContentService contentService;
    private final ObjectMapper objectMapper;
    
    public DocumentSimilarityService(EmbeddingModel embeddingModel,
                                    DocumentEmbeddingRepository embeddingRepository,
                                    DocumentRepository documentRepository,
                                    ContentService contentService,
                                    ObjectMapper objectMapper) {
        this.embeddingModel = embeddingModel;
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.contentService = contentService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate and store embedding for a document
     */
    @Transactional
    public DocumentEmbedding generateEmbedding(Document document) {
        try {
            // Extract document content
            String content = extractDocumentContent(document);
            if (content == null || content.trim().isEmpty()) {
                logger.warn("No content found for document {}", document.getId());
                return null;
            }
            
            // Calculate content hash
            String contentHash = calculateHash(content);
            
            // Check if embedding already exists and is up-to-date
            Optional<DocumentEmbedding> existing = embeddingRepository.findByDocumentId(document.getId());
            if (existing.isPresent() && contentHash.equals(existing.get().getContentHash())) {
                logger.debug("Embedding for document {} is up-to-date", document.getId());
                return existing.get();
            }
            
            // Generate embedding
            logger.info("Generating embedding for document {}", document.getId());
            List<Double> embedding = generateEmbeddingVector(content);
            
            // Store embedding
            DocumentEmbedding docEmbedding = existing.orElse(new DocumentEmbedding());
            docEmbedding.setDocument(document);
            docEmbedding.setEmbedding(serializeEmbedding(embedding));
            docEmbedding.setContentHash(contentHash);
            
            return embeddingRepository.save(docEmbedding);
            
        } catch (Exception e) {
            logger.error("Failed to generate embedding for document {}", document.getId(), e);
            return null;
        }
    }
    
    /**
     * Find similar documents using cosine similarity
     */
    public List<SimilarityResult> findSimilar(Long documentId, int limit) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        return findSimilar(document, limit);
    }
    
    /**
     * Find similar documents using cosine similarity
     */
    public List<SimilarityResult> findSimilar(Document document, int limit) {
        // Use document ID for more reliable lookup
        Optional<DocumentEmbedding> docEmbedding = embeddingRepository.findByDocumentId(document.getId());
        if (docEmbedding.isEmpty()) {
            logger.warn("No embedding found for document {}", document.getId());
            return Collections.emptyList();
        }
        
        try {
            List<Double> targetVector = deserializeEmbedding(docEmbedding.get().getEmbedding());
            return findSimilarByVector(targetVector, document.getId(), limit);
        } catch (Exception e) {
            logger.error("Error finding similar documents", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Find similar documents by query text
     */
    public List<SimilarityResult> findSimilarByText(String queryText, int limit) {
        try {
            List<Double> queryVector = generateEmbeddingVector(queryText);
            return findSimilarByVector(queryVector, null, limit);
        } catch (Exception e) {
            logger.error("Error finding similar documents by text", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Find similar documents by vector with optional field filters
     */
    public List<SimilarityResult> findSimilarByTextWithFilters(String queryText, Map<String, String> filters, int limit) {
        try {
            List<Double> queryVector = generateEmbeddingVector(queryText);
            List<SimilarityResult> results = findSimilarByVector(queryVector, null, limit * 2); // Get more to filter
            
            // Apply filters
            if (filters != null && !filters.isEmpty()) {
                results = results.stream()
                    .filter(result -> matchesFilters(result.getDocument(), filters))
                    .limit(limit)
                    .collect(Collectors.toList());
            }
            
            return results.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error finding similar documents with filters", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Rebuild all embeddings
     */
    @Transactional
    public void rebuildAllEmbeddings() {
        logger.info("Rebuilding all document embeddings...");
        List<Document> documents = documentRepository.findAll();
        rebuildEmbeddings(documents);
    }
    
    /**
     * Rebuild embeddings for specific documents
     */
    @Transactional
    public void rebuildEmbeddings(List<Document> documents) {
        logger.info("Rebuilding embeddings for {} documents...", documents.size());
        int success = 0;
        int failed = 0;
        
        for (Document doc : documents) {
            try {
                // Reload document with contents to avoid LazyInitializationException
                Document reloadedDoc = documentRepository.findById(doc.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + doc.getId()));
                generateEmbedding(reloadedDoc);
                success++;
            } catch (Exception e) {
                logger.error("Failed to generate embedding for document {}", doc.getId(), e);
                failed++;
            }
        }
        
        logger.info("Embedding rebuild complete. Success: {}, Failed: {}", success, failed);
    }
    
    // Private helper methods
    
    private List<SimilarityResult> findSimilarByVector(List<Double> targetVector, Long excludeId, int limit) {
        List<DocumentEmbedding> allEmbeddings = embeddingRepository.findAll();
        
        List<SimilarityResult> results = new ArrayList<>();
        for (DocumentEmbedding embedding : allEmbeddings) {
            // Skip the query document itself
            if (excludeId != null && embedding.getDocument().getId().equals(excludeId)) {
                continue;
            }
            
            try {
                List<Double> vector = deserializeEmbedding(embedding.getEmbedding());
                double similarity = cosineSimilarity(targetVector, vector);
                results.add(new SimilarityResult(embedding.getDocument(), similarity));
            } catch (Exception e) {
                logger.error("Error calculating similarity for document {}", embedding.getDocument().getId(), e);
            }
        }
        
        // Sort by similarity descending
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        
        return results.stream().limit(limit).collect(Collectors.toList());
    }
    
    private String extractDocumentContent(Document document) {
        StringBuilder content = new StringBuilder();
        
        // Add metadata
        if (document.getName() != null) {
            content.append(document.getName()).append(" ");
        }
        if (document.getDescription() != null) {
            content.append(document.getDescription()).append(" ");
        }
        if (document.getKeywords() != null) {
            content.append(String.join(" ", document.getKeywords())).append(" ");
        }
        if (document.getTags() != null) {
            content.append(String.join(" ", document.getTags())).append(" ");
        }
        
        // Add actual content
        if (document.getContents() != null) {
            for (var docContent : document.getContents()) {
                if (docContent.getContentType() != null && docContent.getContentType().startsWith("text/")) {
                    try {
                        byte[] bytes = contentService.getContentBytes(docContent.getId());
                        String text = new String(bytes, StandardCharsets.UTF_8);
                        content.append(text).append(" ");
                    } catch (Exception e) {
                        logger.warn("Could not extract content {}", docContent.getId(), e);
                    }
                }
            }
        }
        
        String result = content.toString().trim();
        if (result.length() > MAX_CONTENT_LENGTH) {
            result = result.substring(0, MAX_CONTENT_LENGTH);
        }
        
        return result;
    }
    
    private List<Double> generateEmbeddingVector(String text) {
        EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
        EmbeddingResponse response = embeddingModel.call(request);
        
        float[] floatArray = response.getResults().get(0).getOutput();
        List<Double> doubleList = new ArrayList<>(floatArray.length);
        for (float f : floatArray) {
            doubleList.add((double) f);
        }
        return doubleList;
    }
    
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Vectors must be same length");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return content.hashCode() + "";
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private String serializeEmbedding(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
    }
    
    private List<Double> deserializeEmbedding(String json) {
        try {
            return objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize embedding", e);
        }
    }
    
    private boolean matchesFilters(Document document, Map<String, String> filters) {
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String field = filter.getKey().toLowerCase();
            String value = filter.getValue().toLowerCase();
            
            switch (field) {
                case "name":
                    if (document.getName() == null || !document.getName().toLowerCase().contains(value)) {
                        return false;
                    }
                    break;
                case "description":
                    if (document.getDescription() == null || !document.getDescription().toLowerCase().contains(value)) {
                        return false;
                    }
                    break;
                case "type":
                    if (document.getDocumentType() == null || !document.getDocumentType().toString().toLowerCase().contains(value)) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }
    
    /**
     * Result object for similarity search
     */
    public static class SimilarityResult {
        private final Document document;
        private final double similarity;
        
        public SimilarityResult(Document document, double similarity) {
            this.document = document;
            this.similarity = similarity;
        }
        
        public Document getDocument() {
            return document;
        }
        
        public double getSimilarity() {
            return similarity;
        }
    }
}
