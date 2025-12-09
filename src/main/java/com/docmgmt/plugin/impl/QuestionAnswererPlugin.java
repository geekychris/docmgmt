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
public class QuestionAnswererPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(QuestionAnswererPlugin.class);
    private final ChatModel chatModel;
    
    public QuestionAnswererPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "answer-question";
    }
    
    @Override
    public String getDescription() {
        return "Answer specific questions about document content with source excerpts";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "QUESTION_CIRCLE";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("question")
                .label("Your Question")
                .description("Ask a specific question about this document")
                .type(PluginParameter.ParameterType.TEXTAREA)
                .required(true)
                .build(),
            PluginParameter.builder()
                .name("targetLanguage")
                .label("Answer Language")
                .description("Language for the answer")
                .type(PluginParameter.ParameterType.SELECT)
                .required(false)
                .defaultValue("English")
                .options(List.of("English", "Spanish", "French", "German", "Italian", 
                                 "Portuguese", "Dutch", "Russian", "Japanese", "Korean", 
                                 "Chinese", "Arabic"))
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String question = request.getParameter("question", (String) null);
            String targetLanguage = request.getParameter("targetLanguage", "English");
            
            if (question == null || question.trim().isEmpty()) {
                throw new PluginException("Question parameter is required");
            }
            
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String languageInstruction = targetLanguage.equals("English") ? "" : 
                "\nIMPORTANT: Provide your answer in " + targetLanguage + ".";
            
            String prompt = String.format(
                "Based on the following document, answer this question: %s\n\n" +
                "Provide a clear, direct answer. If the information is not in the document, say so.%s\n\n" +
                "Document:\n%s",
                question,
                languageInstruction,
                contentToAnalyze
            );
            
            logger.info("Executing QuestionAnswererPlugin for question: '{}' in language: {}", question, targetLanguage);
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            logger.info("LLM response received, length: {} characters", response != null ? response.length() : 0);
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned an empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("question", question);
            data.put("answer", response.trim());
            data.put("language", targetLanguage);
            
            logger.info("Question answered successfully");
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Question answering failed", e);
            throw new PluginException("Question answering failed: " + e.getMessage(), e);
        }
    }
}
