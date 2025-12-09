package com.docmgmt.plugin.impl;

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

import java.util.*;

@Component
public class EntityExtractorPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(EntityExtractorPlugin.class);
    private final ChatModel chatModel;
    
    public EntityExtractorPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "extract-entities";
    }
    
    @Override
    public String getDescription() {
        return "Extract named entities: people, organizations, locations, dates, and amounts";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "USER_CARD";
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Extract named entities from the following document.\n" +
                "Provide the results in this exact format:\n\n" +
                "PEOPLE:\n- [names]\n\n" +
                "ORGANIZATIONS:\n- [organizations]\n\n" +
                "LOCATIONS:\n- [places]\n\n" +
                "DATES:\n- [dates]\n\n" +
                "AMOUNTS:\n- [monetary amounts]\n\n" +
                "If a category has no entities, write 'None'.\n\n" +
                "Document:\n%s",
                contentToAnalyze
            );
            
            logger.debug("Extracting entities");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            // Parse entities
            Map<String, List<String>> entities = parseEntities(response);
            
            Map<String, Object> data = new HashMap<>();
            data.put("people", entities.getOrDefault("PEOPLE", new ArrayList<>()));
            data.put("organizations", entities.getOrDefault("ORGANIZATIONS", new ArrayList<>()));
            data.put("locations", entities.getOrDefault("LOCATIONS", new ArrayList<>()));
            data.put("dates", entities.getOrDefault("DATES", new ArrayList<>()));
            data.put("amounts", entities.getOrDefault("AMOUNTS", new ArrayList<>()));
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Entity extraction failed", e);
            throw new PluginException("Entity extraction failed: " + e.getMessage(), e);
        }
    }
    
    private Map<String, List<String>> parseEntities(String response) {
        Map<String, List<String>> entities = new HashMap<>();
        String currentCategory = null;
        List<String> currentList = new ArrayList<>();
        
        for (String line : response.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("^[A-Z]+:$")) {
                if (currentCategory != null) {
                    entities.put(currentCategory, new ArrayList<>(currentList));
                }
                currentCategory = trimmed.replace(":", "");
                currentList = new ArrayList<>();
            } else if (trimmed.startsWith("-") && currentCategory != null) {
                String entity = trimmed.substring(1).trim();
                if (!entity.equalsIgnoreCase("none") && !entity.isEmpty()) {
                    currentList.add(entity);
                }
            }
        }
        
        if (currentCategory != null) {
            entities.put(currentCategory, currentList);
        }
        
        return entities;
    }
}
