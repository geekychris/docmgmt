package com.docmgmt.plugin;

/**
 * Exception thrown when a plugin execution fails
 */
public class PluginException extends Exception {
    
    public PluginException(String message) {
        super(message);
    }
    
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
