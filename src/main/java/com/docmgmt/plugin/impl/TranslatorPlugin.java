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

import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin for translating document content to a target language
 */
@Component
public class TranslatorPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslatorPlugin.class);
    
    private final ChatModel chatModel;
    
    public TranslatorPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "translate";
    }
    
    @Override
    public String getDescription() {
        return "Detects the language of document content and translates it to a target language";
    }
    
    @Override
    public String getCategory() {
        return "Content Analysis";
    }
    
    @Override
    public String getIcon() {
        return "GLOBE";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("targetLanguage")
                .label("Target Language")
                .description("Language to translate to")
                .type(PluginParameter.ParameterType.SELECT)
                .required(false)
                .defaultValue("English")
                .options(List.of("English", "Spanish", "French", "German", "Italian", 
                    "Portuguese", "Russian", "Chinese", "Japanese", "Korean", "Arabic", "Hindi"))
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            if (content == null || content.trim().isEmpty()) {
                throw new PluginException("No content provided for translation");
            }
            
            // Get target language from parameters (default to English)
            String targetLanguage = request.getParameter("targetLanguage", "English");
            
            // Limit content length for LLM processing
            String contentToTranslate = content;
            if (content.length() > 4000) {
                contentToTranslate = content.substring(0, 4000) + "...";
                logger.info("Content truncated to 4000 characters for translation");
            }
            
            // Step 1: Detect source language
            String detectionPrompt = String.format(
                "Analyze the following text and identify its language. " +
                "Respond ONLY with the ISO 639-1 two-letter language code (e.g., 'en' for English, 'es' for Spanish, 'fr' for French, 'de' for German, etc.).\n\n" +
                "Text:\n%s",
                contentToTranslate
            );
            
            logger.debug("Detecting language for content");
            String detectionResponse = chatModel.call(new Prompt(detectionPrompt)).getResult().getOutput().getContent();
            String sourceLanguageCode = extractLanguageCode(detectionResponse);
            
            logger.info("Detected source language: {}", sourceLanguageCode);
            
            // Step 2: Translate content
            String translationPrompt = String.format(
                "Translate the following text from %s to %s. " +
                "Provide ONLY the translation without any additional explanation or commentary.\n\n" +
                "Text to translate:\n%s",
                getLanguageName(sourceLanguageCode),
                targetLanguage,
                contentToTranslate
            );
            
            logger.debug("Translating content to {}", targetLanguage);
            String translatedContent = chatModel.call(new Prompt(translationPrompt)).getResult().getOutput().getContent();
            
            // Get target language code
            String targetLanguageCodePrompt = String.format(
                "What is the ISO 639-1 two-letter language code for '%s'? " +
                "Respond ONLY with the two-letter code.",
                targetLanguage
            );
            String targetLanguageCodeResponse = chatModel.call(new Prompt(targetLanguageCodePrompt)).getResult().getOutput().getContent();
            String targetLanguageCode = extractLanguageCode(targetLanguageCodeResponse);
            
            // Build response
            Map<String, Object> data = new HashMap<>();
            data.put("originalLanguage", sourceLanguageCode);
            data.put("originalLanguageName", getLanguageName(sourceLanguageCode));
            data.put("targetLanguage", targetLanguageCode);
            data.put("targetLanguageName", targetLanguage);
            data.put("originalContent", contentToTranslate);
            data.put("translatedContent", translatedContent);
            data.put("truncated", content.length() > 4000);
            
            logger.info("Translation completed: {} -> {}", sourceLanguageCode, targetLanguageCode);
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            logger.error("Translation failed", e);
            throw new PluginException("Translation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract language code from LLM response
     */
    private String extractLanguageCode(String response) {
        if (response == null) {
            return "unknown";
        }
        
        // Clean up the response and extract 2-letter code
        String cleaned = response.trim().toLowerCase();
        
        // Look for a 2-letter code pattern
        Pattern pattern = Pattern.compile("\\b([a-z]{2})\\b");
        Matcher matcher = pattern.matcher(cleaned);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback: if the entire response is 2 letters
        if (cleaned.matches("[a-z]{2}")) {
            return cleaned;
        }
        
        return "unknown";
    }
    
    /**
     * Get a readable language name from code
     */
    private String getLanguageName(String code) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("en", "English");
        languageNames.put("es", "Spanish");
        languageNames.put("fr", "French");
        languageNames.put("de", "German");
        languageNames.put("it", "Italian");
        languageNames.put("pt", "Portuguese");
        languageNames.put("ru", "Russian");
        languageNames.put("zh", "Chinese");
        languageNames.put("ja", "Japanese");
        languageNames.put("ko", "Korean");
        languageNames.put("ar", "Arabic");
        languageNames.put("hi", "Hindi");
        
        return languageNames.getOrDefault(code.toLowerCase(), "Unknown");
    }
}
