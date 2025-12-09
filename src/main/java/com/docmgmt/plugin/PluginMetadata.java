package com.docmgmt.plugin;

import java.util.Collections;
import java.util.List;

/**
 * Optional interface for plugins that want to provide parameter metadata
 * for UI generation
 */
public interface PluginMetadata {
    
    /**
     * Get list of parameters this plugin accepts
     * @return list of parameter definitions
     */
    default List<PluginParameter> getParameters() {
        return Collections.emptyList();
    }
    
    /**
     * Get category for grouping in UI
     * @return category name
     */
    default String getCategory() {
        return "General";
    }
    
    /**
     * Get icon name (Vaadin icon)
     * @return icon name without "VaadinIcon." prefix
     */
    default String getIcon() {
        return "COG";
    }
}
