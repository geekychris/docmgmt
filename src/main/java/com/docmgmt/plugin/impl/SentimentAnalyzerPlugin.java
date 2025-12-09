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

import java.util.HashMap;
import java.util.Map;

@Component
public class SentimentAnalyzerPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalyzerPlugin.class);
    private final ChatModel chatModel;
    
    public SentimentAnalyzerPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "analyze-sentiment";
    }
    
    @Override
    public String getDescription() {
        return "Analyze tone, sentiment, and emotional indicators in document content";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "SMILEY_O";
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Analyze the sentiment and tone of the following document.\n\n" +
                "Provide your analysis in this format:\n" +
                "OVERALL_SENTIMENT:\n[positive/negative/neutral/mixed]\n\n" +
                "TONE:\n[formal/informal/professional/casual/technical/emotional]\n\n" +
                "EMOTION_INDICATORS:\n[list key emotional words or phrases]\n\n" +
                "CONFIDENCE_SCORE:\n[0-10]\n\n" +
                "SUMMARY:\n[brief explanation]\n\n" +
                "Document:\n%s",
                contentToAnalyze
            );
            
            logger.debug("Analyzing sentiment");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            // Parse response
            Map<String, String> parsed = parseSentimentResponse(response);
            
            Map<String, Object> data = new HashMap<>();
            data.put("sentiment", parsed.getOrDefault("OVERALL_SENTIMENT", "unknown"));
            data.put("tone", parsed.getOrDefault("TONE", "unknown"));
            data.put("emotionIndicators", parsed.getOrDefault("EMOTION_INDICATORS", ""));
            data.put("confidenceScore", parsed.getOrDefault("CONFIDENCE_SCORE", "0"));
            data.put("summary", parsed.getOrDefault("SUMMARY", ""));
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Sentiment analysis failed", e);
            throw new PluginException("Sentiment analysis failed: " + e.getMessage(), e);
        }
    }
    
    private Map<String, String> parseSentimentResponse(String response) {
        Map<String, String> result = new HashMap<>();
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : response.split("\n")) {
            if (line.matches("^[A-Z_]+:$")) {
                if (currentSection != null) {
                    result.put(currentSection, currentContent.toString().trim());
                }
                currentSection = line.replace(":", "").trim();
                currentContent = new StringBuilder();
            } else if (currentSection != null && !line.trim().isEmpty()) {
                currentContent.append(line).append("\n");
            }
        }
        
        if (currentSection != null) {
            result.put(currentSection, currentContent.toString().trim());
        }
        
        return result;
    }
}
