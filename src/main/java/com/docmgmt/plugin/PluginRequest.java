package com.docmgmt.plugin;

import com.docmgmt.model.Document;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request object for plugin execution
 */
@Data
@Builder
public class PluginRequest {
    
    /**
     * The document being processed
     */
    private Document document;
    
    /**
     * The content text to process
     */
    private String content;
    
    /**
     * Additional parameters for the plugin
     */
    private Map<String, Object> parameters;
    
    /**
     * Get a parameter value
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        return (T) parameters.get(key);
    }
}
