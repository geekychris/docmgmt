package com.docmgmt.plugin;

import com.docmgmt.model.Document;

/**
 * Interface for document plugins that can perform operations using LLMs
 */
public interface DocumentPlugin {
    
    /**
     * Get the task name that this plugin handles
     * @return task name (e.g., "translate", "summarize", "analyze")
     */
    String getTaskName();
    
    /**
     * Execute the plugin on a document with given parameters
     * @param request the plugin request containing document, content, and parameters
     * @return the plugin response with results
     * @throws PluginException if plugin execution fails
     */
    PluginResponse execute(PluginRequest request) throws PluginException;
    
    /**
     * Get a description of what this plugin does
     * @return plugin description
     */
    String getDescription();
}
