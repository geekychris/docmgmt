package com.docmgmt.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry for managing document plugins
 */
@Component
public class PluginRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);
    
    private final Map<String, DocumentPlugin> plugins = new HashMap<>();
    
    /**
     * Constructor that auto-discovers all DocumentPlugin beans
     */
    public PluginRegistry(List<DocumentPlugin> pluginList) {
        for (DocumentPlugin plugin : pluginList) {
            registerPlugin(plugin);
        }
        logger.info("Registered {} document plugins", plugins.size());
    }
    
    /**
     * Register a plugin
     */
    public void registerPlugin(DocumentPlugin plugin) {
        String taskName = plugin.getTaskName();
        if (plugins.containsKey(taskName)) {
            logger.warn("Plugin for task '{}' already registered, overwriting", taskName);
        }
        plugins.put(taskName, plugin);
        logger.info("Registered plugin: {} - {}", taskName, plugin.getDescription());
    }
    
    /**
     * Get a plugin by task name
     */
    public Optional<DocumentPlugin> getPlugin(String taskName) {
        return Optional.ofNullable(plugins.get(taskName));
    }
    
    /**
     * Get all registered plugins
     */
    public Collection<DocumentPlugin> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
    
    /**
     * Get all available task names
     */
    public Set<String> getAvailableTasks() {
        return Collections.unmodifiableSet(plugins.keySet());
    }
}
