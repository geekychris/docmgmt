package com.docmgmt.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response object from plugin execution
 */
@Data
@Builder
public class PluginResponse {
    
    /**
     * Status of the plugin execution
     */
    private PluginStatus status;
    
    /**
     * Result data from the plugin
     */
    private Map<String, Object> data;
    
    /**
     * Error message if execution failed
     */
    private String errorMessage;
    
    /**
     * Get a result value
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        if (data == null) {
            return null;
        }
        return (T) data.get(key);
    }
    
    /**
     * Get a result value with default
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        if (data == null || !data.containsKey(key)) {
            return defaultValue;
        }
        return (T) data.get(key);
    }
    
    public enum PluginStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE
    }
}
