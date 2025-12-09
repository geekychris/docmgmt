package com.docmgmt.plugin.impl;

import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FAQGeneratorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FAQGeneratorPlugin.class);
    private final ChatModel chatModel;
    
    public FAQGeneratorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    
    @Override public String getTaskName() { return "generate-faq"; }
    @Override public String getDescription() { return "Generate FAQs from document content"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "QUESTION_CIRCLE"; }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("count")
                .label("Number of FAQs")
                .description("How many FAQ pairs to generate")
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
            Integer count = request.getParameter("count", 5);
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Generate %d frequently asked questions and answers based on this document.\\n\\nDocument:\\n%s",
                count, contentToAnalyze
            );
            
            logger.info("Executing FAQGeneratorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("faqs", response.trim());
            data.put("count", count);
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("FAQ generation failed", e);
            throw new PluginException("FAQ generation failed: " + e.getMessage(), e);
        }
    }
}
