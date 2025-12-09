package com.docmgmt.plugin.impl;

import com.docmgmt.model.Document;
import com.docmgmt.plugin.DocumentPlugin;
import com.docmgmt.plugin.PluginException;
import com.docmgmt.plugin.PluginMetadata;
import com.docmgmt.plugin.PluginParameter;
import com.docmgmt.plugin.PluginRequest;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.service.DocumentSimilarityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DuplicateDetectorPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetectorPlugin.class);
    private final DocumentSimilarityService similarityService;
    
    public DuplicateDetectorPlugin(DocumentSimilarityService similarityService) {
        this.similarityService = similarityService;
    }
    
    @Override
    public String getTaskName() {
        return "find-duplicates";
    }
    
    @Override
    public String getDescription() {
        return "Find similar or duplicate documents using vector embeddings";
    }
    
    @Override
    public String getCategory() {
        return "Classification";
    }
    
    @Override
    public String getIcon() {
        return "COPY";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("maxResults")
                .label("Maximum Results")
                .description("Maximum number of similar documents to find")
                .type(PluginParameter.ParameterType.NUMBER)
                .required(false)
                .defaultValue("10")
                .minValue(1)
                .maxValue(20)
                .build(),
            PluginParameter.builder()
                .name("minSimilarity")
                .label("Minimum Similarity")
                .description("Minimum similarity threshold (0-100%)")
                .type(PluginParameter.ParameterType.NUMBER)
                .required(false)
                .defaultValue("50")
                .minValue(0)
                .maxValue(100)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            Document currentDoc = request.getDocument();
            Integer maxResults = request.getParameter("maxResults", 10);
            Integer minSimilarity = request.getParameter("minSimilarity", 50);
            
            // Convert percentage to 0-1 scale
            double minSimilarityThreshold = minSimilarity / 100.0;
            
            // Use embedding-based similarity search
            List<DocumentSimilarityService.SimilarityResult> similarityResults = 
                similarityService.findSimilar(currentDoc, maxResults * 2); // Get extra for filtering
            
            if (similarityResults.isEmpty()) {
                logger.warn("No embedding found for document {}. Similarity search unavailable.", currentDoc.getId());
                return PluginResponse.builder()
                    .status(PluginResponse.PluginStatus.SUCCESS)
                    .data(Map.of(
                        "duplicates", Collections.emptyList(),
                        "totalCandidates", 0,
                        "message", "No embedding found for this document. Please ensure embeddings are generated."
                    ))
                    .build();
            }
            
            // Filter by minimum similarity and format results
            List<Map<String, Object>> duplicates = similarityResults.stream()
                .filter(result -> result.getSimilarity() >= minSimilarityThreshold)
                .limit(maxResults)
                .map(result -> {
                    Map<String, Object> item = new HashMap<>();
                    Document doc = result.getDocument();
                    item.put("documentId", doc.getId());
                    item.put("documentName", doc.getName());
                    item.put("documentDescription", doc.getDescription());
                    item.put("documentType", doc.getDocumentType() != null ? doc.getDocumentType().toString() : "Unknown");
                    item.put("similarityScore", Math.round(result.getSimilarity() * 100));
                    item.put("similarityPercentage", String.format("%.1f%%", result.getSimilarity() * 100));
                    
                    // Categorize similarity level
                    double similarity = result.getSimilarity();
                    String level;
                    if (similarity >= 0.95) {
                        level = "Very High (Likely Duplicate)";
                    } else if (similarity >= 0.80) {
                        level = "High";
                    } else if (similarity >= 0.60) {
                        level = "Medium";
                    } else {
                        level = "Low";
                    }
                    item.put("similarityLevel", level);
                    
                    return item;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> data = new HashMap<>();
            data.put("duplicates", duplicates);
            data.put("totalCandidates", similarityResults.size());
            data.put("filteredCount", duplicates.size());
            data.put("minSimilarityThreshold", minSimilarity + "%");
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Duplicate detection failed", e);
            throw new PluginException("Duplicate detection failed: " + e.getMessage(), e);
        }
    }
}
