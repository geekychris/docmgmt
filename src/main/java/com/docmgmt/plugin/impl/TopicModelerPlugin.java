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
public class TopicModelerPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicModelerPlugin.class);
    private final ChatModel chatModel;
    
    public TopicModelerPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "identify-topics";
    }
    
    @Override
    public String getDescription() {
        return "Identify main topics and themes for document grouping and discovery";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "BOOK";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("maxTopics")
                .label("Maximum Topics")
                .description("Maximum number of topics to identify")
                .type(PluginParameter.ParameterType.NUMBER)
                .required(false)
                .defaultValue("5")
                .minValue(1)
                .maxValue(10)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            Integer maxTopics = request.getParameter("maxTopics", 5);
            
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Identify the main topics and themes in the following document.\n" +
                "Provide up to %d topics, ordered by prominence.\n\n" +
                "Format your response as:\n" +
                "TOPICS:\n" +
                "1. [Topic Name] - [brief description]\n" +
                "2. [Topic Name] - [brief description]\n\n" +
                "PRIMARY_CATEGORY:\n[overall category or domain]\n\n" +
                "Document:\n%s",
                maxTopics,
                contentToAnalyze
            );
            
            logger.debug("Identifying topics");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            // Parse topics
            List<Map<String, String>> topics = parseTopics(response);
            String primaryCategory = extractPrimaryCategory(response);
            
            Map<String, Object> data = new HashMap<>();
            data.put("topics", topics);
            data.put("primaryCategory", primaryCategory);
            data.put("topicCount", topics.size());
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Topic identification failed", e);
            throw new PluginException("Topic identification failed: " + e.getMessage(), e);
        }
    }
    
    private List<Map<String, String>> parseTopics(String response) {
        List<Map<String, String>> topics = new ArrayList<>();
        boolean inTopicsSection = false;
        
        for (String line : response.split("\n")) {
            if (line.trim().equals("TOPICS:")) {
                inTopicsSection = true;
                continue;
            }
            if (line.trim().startsWith("PRIMARY_CATEGORY:")) {
                inTopicsSection = false;
                continue;
            }
            
            if (inTopicsSection && line.matches("^\\d+\\..*")) {
                String[] parts = line.substring(line.indexOf('.') + 1).split("-", 2);
                if (parts.length >= 1) {
                    Map<String, String> topic = new HashMap<>();
                    topic.put("name", parts[0].trim());
                    topic.put("description", parts.length > 1 ? parts[1].trim() : "");
                    topics.add(topic);
                }
            }
        }
        
        return topics;
    }
    
    private String extractPrimaryCategory(String response) {
        for (String line : response.split("\n")) {
            if (line.trim().startsWith("PRIMARY_CATEGORY:")) {
                int colonIndex = line.indexOf(':');
                if (colonIndex >= 0 && colonIndex < line.length() - 1) {
                    return line.substring(colonIndex + 1).trim();
                }
            }
        }
        
        // Look for content after PRIMARY_CATEGORY: header
        String[] lines = response.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals("PRIMARY_CATEGORY:") && i + 1 < lines.length) {
                return lines[i + 1].trim();
            }
        }
        
        return "Unknown";
    }
}
