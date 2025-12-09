package com.docmgmt.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Metadata describing a plugin parameter
 */
@Data
@Builder
public class PluginParameter {
    
    /**
     * Parameter name (key in the parameters map)
     */
    private String name;
    
    /**
     * Display label for UI
     */
    private String label;
    
    /**
     * Parameter description/help text
     */
    private String description;
    
    /**
     * Parameter type
     */
    private ParameterType type;
    
    /**
     * Whether the parameter is required
     */
    private boolean required;
    
    /**
     * Default value (can be null)
     */
    private String defaultValue;
    
    /**
     * For SELECT type: available options
     */
    private List<String> options;
    
    /**
     * For NUMBER type: minimum value
     */
    private Integer minValue;
    
    /**
     * For NUMBER type: maximum value
     */
    private Integer maxValue;
    
    public enum ParameterType {
        TEXT,           // Single line text input
        TEXTAREA,       // Multi-line text input
        NUMBER,         // Number input
        SELECT,         // Dropdown selection
        BOOLEAN,        // Checkbox
        DOCUMENT_ID     // Document ID selector
    }
}
