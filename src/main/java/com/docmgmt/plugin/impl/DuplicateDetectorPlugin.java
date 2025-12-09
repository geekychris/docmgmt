package com.docmgmt.plugin.impl;

import com.docmgmt.model.Document;
import com.docmgmt.plugin.DocumentPlugin;
import com.docmgmt.plugin.PluginException;
import com.docmgmt.plugin.PluginMetadata;
import com.docmgmt.plugin.PluginParameter;
import com.docmgmt.plugin.PluginRequest;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;

@Component
public class DuplicateDetectorPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetectorPlugin.class);
    private final ChatModel chatModel;
    private final DocumentService documentService;
    
    public DuplicateDetectorPlugin(ChatModel chatModel, DocumentService documentService) {
        this.chatModel = chatModel;
        this.documentService = documentService;
    }
    
    @Override
    public String getTaskName() {
        return "find-duplicates";
    }
    
    @Override
    public String getDescription() {
        return "Find similar or duplicate documents in the system with similarity scores";
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
                .description("Maximum number of duplicates to find")
                .type(PluginParameter.ParameterType.NUMBER)
                .required(false)
                .defaultValue("5")
                .minValue(1)
                .maxValue(20)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            Document currentDoc = request.getDocument();
            Integer maxResults = request.getParameter("maxResults", 5);
            
            String contentToAnalyze = content.length() > 2000 ? content.substring(0, 2000) : content;
            
            // Get all other documents (basic implementation - in production would use vector search)
            List<Document> allDocs = documentService.findAllLatestVersions();
            List<Map<String, Object>> potentialDuplicates = new ArrayList<>();
            
            // Simple heuristic filtering first
            for (Document doc : allDocs) {
                if (doc.getId().equals(currentDoc.getId())) {
                    continue;
                }
                
                // Check name similarity
                if (doc.getName() != null && currentDoc.getName() != null) {
                    String similarity = calculateBasicSimilarity(currentDoc.getName(), doc.getName());
                    if (similarity.equals("high") || similarity.equals("medium")) {
                        Map<String, Object> candidate = new HashMap<>();
                        candidate.put("documentId", doc.getId());
                        candidate.put("documentName", doc.getName());
                        candidate.put("nameSimilarity", similarity);
                        potentialDuplicates.add(candidate);
                    }
                }
            }
            
            // Use LLM for deeper analysis of top candidates
            List<Map<String, Object>> analyzedDuplicates = new ArrayList<>();
            int analyzed = 0;
            
            for (Map<String, Object> candidate : potentialDuplicates) {
                if (analyzed >= maxResults) break;
                
                Long docId = (Long) candidate.get("documentId");
                String docName = (String) candidate.get("documentName");
                
                String prompt = String.format(
                    "Compare these two documents and rate their similarity from 0-100.\\n" +
                    "Focus on content, structure, and purpose.\\n\\n" +
                    "Document 1 (name: %s):\\n%s\\n\\n" +
                    "Document 2 (name: %s)\\n\\n" +
                    "Provide only the similarity score (0-100) and nothing else.",
                    currentDoc.getName(),
                    contentToAnalyze.substring(0, Math.min(500, contentToAnalyze.length())),
                    docName
                );
                
                try {
                    String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
                    int similarityScore = extractScore(response);
                    
                    if (similarityScore >= 50) { // Only include if significantly similar
                        candidate.put("similarityScore", similarityScore);
                        analyzedDuplicates.add(candidate);
                        analyzed++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to analyze similarity for document {}: {}", docId, e.getMessage());
                }
            }
            
            // Sort by similarity score
            analyzedDuplicates.sort((a, b) -> {
                Integer scoreA = (Integer) a.getOrDefault("similarityScore", 0);
                Integer scoreB = (Integer) b.getOrDefault("similarityScore", 0);
                return scoreB.compareTo(scoreA);
            });
            
            Map<String, Object> data = new HashMap<>();
            data.put("duplicates", analyzedDuplicates.subList(0, Math.min(maxResults, analyzedDuplicates.size())));
            data.put("totalCandidates", potentialDuplicates.size());
            data.put("analyzedCount", analyzed);
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Duplicate detection failed", e);
            throw new PluginException("Duplicate detection failed: " + e.getMessage(), e);
        }
    }
    
    private String calculateBasicSimilarity(String str1, String str2) {
        String s1 = str1.toLowerCase();
        String s2 = str2.toLowerCase();
        
        if (s1.equals(s2)) return "high";
        if (s1.contains(s2) || s2.contains(s1)) return "high";
        
        // Simple word overlap
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        double overlap = (double) intersection.size() / Math.max(words1.size(), words2.size());
        
        if (overlap > 0.6) return "high";
        if (overlap > 0.3) return "medium";
        return "low";
    }
    
    private int extractScore(String response) {
        try {
            // Extract first number found
            String cleaned = response.trim().replaceAll("[^0-9]", "");
            if (!cleaned.isEmpty()) {
                int score = Integer.parseInt(cleaned.substring(0, Math.min(3, cleaned.length())));
                return Math.min(100, Math.max(0, score));
            }
        } catch (Exception e) {
            logger.warn("Failed to extract score from: {}", response);
        }
        return 0;
    }
}
