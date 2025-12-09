package com.docmgmt.plugin.impl;

import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ReadabilityScorerPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ReadabilityScorerPlugin.class);
    private final ChatModel chatModel;
    
    public ReadabilityScorerPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    
    @Override public String getTaskName() { return "readability-score"; }
    @Override public String getDescription() { return "Analyze document readability and complexity level"; }
    @Override public String getCategory() { return "Quality"; }
    @Override public String getIcon() { return "EYE"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Analyze the readability of this document. Provide: reading level, readability score (1-10), sentence complexity, vocabulary assessment, and improvement suggestions.\\n\\nDocument:\\n%s",
                contentToAnalyze
            );
            
            logger.info("Executing ReadabilityScorerPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("analysis", response.trim());
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Readability analysis failed", e);
            throw new PluginException("Readability analysis failed: " + e.getMessage(), e);
        }
    }
}
