package com.docmgmt.plugin;

import com.docmgmt.BaseTest;
import com.docmgmt.model.Article;
import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for TranslatorPlugin
 */
public class TranslatorPluginTest extends BaseTest {
    
    @Autowired
    private PluginService pluginService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private ContentService contentService;
    
    @Test
    public void testTranslatorPluginRegistered() {
        Map<String, String> plugins = pluginService.getAvailablePlugins();
        
        assertNotNull(plugins);
        assertTrue(plugins.containsKey("translate"));
        assertEquals("Detects the language of document content and translates it to a target language", 
            plugins.get("translate"));
    }
    
    @Test
    public void testTranslateDocument() throws Exception {
        // Create a document with English content
        Article article = Article.builder()
            .name("Test Article")
            .description("Article for translation test")
            .build();
        
        article = (Article) documentService.save(article);
        
        // Add English text content
        String englishText = "Hello, this is a test document. How are you today?";
        MockMultipartFile file = new MockMultipartFile(
            "content", 
            "test.txt", 
            "text/plain", 
            englishText.getBytes()
        );
        
        contentService.createContentInDatabase(file, article);
        
        // Execute translation plugin
        Map<String, Object> parameters = Map.of("targetLanguage", "Spanish");
        PluginResponse response = pluginService.executePlugin(article.getId(), "translate", parameters);
        
        // Verify response
        assertNotNull(response);
        assertEquals(PluginResponse.PluginStatus.SUCCESS, response.getStatus());
        
        // Verify data
        String originalLang = response.getData("originalLanguage");
        String targetLang = response.getData("targetLanguage");
        String translatedContent = response.getData("translatedContent");
        
        assertNotNull(originalLang);
        assertEquals("en", originalLang.toLowerCase());
        
        assertNotNull(targetLang);
        assertEquals("es", targetLang.toLowerCase());
        
        assertNotNull(translatedContent);
        assertFalse(translatedContent.isEmpty());
        
        // Translated content should be different from original
        assertNotEquals(englishText, translatedContent);
        
        System.out.println("Original: " + englishText);
        System.out.println("Translated: " + translatedContent);
    }
}
