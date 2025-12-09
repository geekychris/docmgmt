package com.docmgmt.plugin.impl;

import com.docmgmt.model.Document;
import com.docmgmt.plugin.DocumentPlugin;
import com.docmgmt.plugin.PluginException;
import com.docmgmt.plugin.PluginMetadata;
import com.docmgmt.plugin.PluginParameter;
import com.docmgmt.plugin.PluginRequest;
import com.docmgmt.plugin.PluginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ClassifierPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassifierPlugin.class);
    private final ChatModel chatModel;
    
    public ClassifierPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "classify";
    }
    
    @Override
    public String getDescription() {
        return "Automatically determine document type and suggest categorization";
    }
    
    @Override
    public String getCategory() {
        return "Classification";
    }
    
    @Override
    public String getIcon() {
        return "FOLDER_OPEN";
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String availableTypes = String.join(", ", 
                "ARTICLE", "REPORT", "CONTRACT", "MANUAL", "PRESENTATION", "TRIP_REPORT", "OTHER");
            
            String prompt = String.format(
                "Analyze the following document and classify it.\\n\\n" +
                "Available document types: %s\\n\\n" +
                "Provide your classification in this format:\\n" +
                "DOCUMENT_TYPE:\\n[type from the list above]\\n\\n" +
                "CONFIDENCE:\\n[high/medium/low]\\n\\n" +
                "REASONING:\\n[brief explanation]\\n\\n" +
                "SUGGESTED_FOLDER:\\n[logical folder path for this document]\\n\\n" +
                "Document:\\n%s",
                availableTypes,
                contentToAnalyze
            );
            
            logger.debug("Classifying document");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            // Parse response
            Map<String, String> parsed = parseClassificationResponse(response);
            
            // Map to Document.DocumentType enum
            String typeStr = parsed.getOrDefault("DOCUMENT_TYPE", "OTHER").toUpperCase();
            Document.DocumentType documentType;
            try {
                documentType = Document.DocumentType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                documentType = Document.DocumentType.OTHER;
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("documentType", documentType);
            data.put("documentTypeString", documentType.toString());
            data.put("confidence", parsed.getOrDefault("CONFIDENCE", "unknown"));
            data.put("reasoning", parsed.getOrDefault("REASONING", ""));
            data.put("suggestedFolder", parsed.getOrDefault("SUGGESTED_FOLDER", ""));
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Classification failed", e);
            throw new PluginException("Classification failed: " + e.getMessage(), e);
        }
    }
    
    private Map<String, String> parseClassificationResponse(String response) {
        Map<String, String> result = new HashMap<>();
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : response.split("\\n")) {
            if (line.matches("^[A-Z_]+:$")) {
                if (currentSection != null) {
                    result.put(currentSection, currentContent.toString().trim());
                }
                currentSection = line.replace(":", "").trim();
                currentContent = new StringBuilder();
            } else if (currentSection != null && !line.trim().isEmpty()) {
                currentContent.append(line).append("\\n");
            }
        }
        
        if (currentSection != null) {
            result.put(currentSection, currentContent.toString().trim());
        }
        
        return result;
    }
}
