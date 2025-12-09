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
import java.util.stream.Collectors;

@Component
public class KeywordExtractorPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(KeywordExtractorPlugin.class);
    private final ChatModel chatModel;
    
    public KeywordExtractorPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "extract-keywords";
    }
    
    @Override
    public String getDescription() {
        return "Extract relevant keywords and tags for searchability and categorization";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "TAGS";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("maxKeywords")
                .label("Maximum Keywords")
                .description("Maximum number of keywords to extract")
                .type(PluginParameter.ParameterType.NUMBER)
                .required(false)
                .defaultValue("10")
                .minValue(1)
                .maxValue(50)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            Integer maxKeywords = request.getParameter("maxKeywords", 10);
            
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Analyze the following document and extract the most relevant keywords and tags.\n" +
                "Provide up to %d keywords that would be useful for searching and categorizing this document.\n" +
                "Return ONLY a comma-separated list of keywords, nothing else.\n\n" +
                "Document:\n%s",
                maxKeywords,
                contentToAnalyze
            );
            
            logger.debug("Extracting keywords");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            // Parse keywords
            List<String> keywords = Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .limit(maxKeywords)
                .collect(Collectors.toList());
            
            Map<String, Object> data = new HashMap<>();
            data.put("keywords", keywords);
            data.put("keywordsString", String.join(", ", keywords));
            data.put("tags", new HashSet<>(keywords));
            data.put("count", keywords.size());
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Keyword extraction failed", e);
            throw new PluginException("Keyword extraction failed: " + e.getMessage(), e);
        }
    }
}
