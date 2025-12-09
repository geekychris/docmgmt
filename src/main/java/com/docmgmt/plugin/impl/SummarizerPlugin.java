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
import java.util.List;
import java.util.Map;

@Component
public class SummarizerPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(SummarizerPlugin.class);
    private final ChatModel chatModel;
    
    public SummarizerPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "summarize";
    }
    
    @Override
    public String getDescription() {
        return "Generate executive summary with key points and action items";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "FILE_TEXT";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("length")
                .label("Summary Length")
                .description("Choose summary detail level")
                .type(PluginParameter.ParameterType.SELECT)
                .required(false)
                .defaultValue("standard")
                .options(List.of("brief", "standard", "detailed"))
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String length = request.getParameter("length", "standard"); // brief, standard, detailed
            
            String contentToSummarize = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String lengthInstruction;
            switch (length.toLowerCase()) {
                case "brief":
                    lengthInstruction = "in 2-3 sentences";
                    break;
                case "detailed":
                    lengthInstruction = "in detail with multiple paragraphs";
                    break;
                default:
                    lengthInstruction = "in a concise paragraph";
            }
            
            String prompt = String.format(
                "Analyze the following document and provide:\n" +
                "1. A summary %s\n" +
                "2. List the key points (3-5 bullet points)\n" +
                "3. List any action items or next steps mentioned\n\n" +
                "Document:\n%s",
                lengthInstruction,
                contentToSummarize
            );
            
            logger.info("Generating {} summary", length);
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            logger.info("Summary generated, response length: {} characters", response != null ? response.length() : 0);
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned an empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("summary", response.trim());
            data.put("length", length);
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Summarization failed", e);
            throw new PluginException("Summarization failed: " + e.getMessage(), e);
        }
    }
}
