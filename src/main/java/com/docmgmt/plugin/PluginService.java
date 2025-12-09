package com.docmgmt.plugin;

import com.docmgmt.dto.PluginInfoDTO;
import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Service for executing document plugins
 */
@Service
public class PluginService {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);
    
    private final PluginRegistry pluginRegistry;
    private final DocumentService documentService;
    private final ContentService contentService;
    
    public PluginService(PluginRegistry pluginRegistry, 
                        DocumentService documentService,
                        ContentService contentService) {
        this.pluginRegistry = pluginRegistry;
        this.documentService = documentService;
        this.contentService = contentService;
    }
    
    /**
     * Execute a plugin on a document with specific content and save options
     * @param documentId the document ID
     * @param taskName the plugin task name
     * @param parameters additional parameters
     * @param contentId optional specific content ID to use (null for default)
     * @param saveOption save option: "none", "primary", or "secondary"
     * @param sourceContentId source content ID when saving as secondary rendition
     * @return the plugin response
     * @throws PluginException if execution fails
     */
    @Transactional
    public PluginResponse executePlugin(Long documentId, String taskName, Map<String, Object> parameters, 
                                       Long contentId, String saveOption, Long sourceContentId) 
            throws PluginException {
        
        // Find the plugin
        DocumentPlugin plugin = pluginRegistry.getPlugin(taskName)
            .orElseThrow(() -> new PluginException("No plugin found for task: " + taskName));
        
        // Load document
        Document document = documentService.findById(documentId);
        if (document == null) {
            throw new PluginException("Document not found: " + documentId);
        }
        
        // Extract text content from document or specific content
        String content;
        if (contentId != null) {
            content = extractTextFromContent(contentId);
        } else {
            content = extractTextContent(document);
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new PluginException("No text content found in document");
        }
        
        // Build request
        PluginRequest request = PluginRequest.builder()
            .document(document)
            .content(content)
            .parameters(parameters)
            .build();
        
        // Execute plugin
        logger.info("Executing plugin '{}' on document {} ({})", taskName, documentId, document.getName());
        PluginResponse response = plugin.execute(request);
        
        logger.info("Plugin '{}' completed with status: {}", taskName, response.getStatus());
        
        // Save as markdown based on save option
        if (response.getStatus() == PluginResponse.PluginStatus.SUCCESS && saveOption != null && !"none".equals(saveOption)) {
            saveResultAsMarkdown(document, plugin, response, saveOption, sourceContentId);
        }
        
        return response;
    }
    
    /**
     * Convenience method for backward compatibility - no markdown save
     */
    @Transactional
    public PluginResponse executePlugin(Long documentId, String taskName, Map<String, Object> parameters) 
            throws PluginException {
        return executePlugin(documentId, taskName, parameters, null, "none", null);
    }
    
    /**
     * Convenience method for backward compatibility - simple markdown save
     */
    @Transactional
    public PluginResponse executePlugin(Long documentId, String taskName, Map<String, Object> parameters, boolean saveAsMarkdown) 
            throws PluginException {
        return executePlugin(documentId, taskName, parameters, null, saveAsMarkdown ? "secondary" : "none", null);
    }
    
    /**
     * Save plugin result as markdown content
     * @param saveOption "primary" or "secondary"
     * @param sourceContentId content ID to link to when saving as secondary
     */
    private void saveResultAsMarkdown(Document document, DocumentPlugin plugin, PluginResponse response, 
                                     String saveOption, Long sourceContentId) {
        try {
            String markdown = formatResultAsMarkdown(plugin, response);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_result_%s.md", plugin.getTaskName(), timestamp);
            
            logger.info("Saving markdown with {} bytes of content", markdown.length());
            logger.debug("Markdown content preview: {}", markdown.substring(0, Math.min(200, markdown.length())));
            
            // Create content directly in database
            Content content = new Content();
            content.setName(filename);
            content.setContentType("text/markdown");
            content.setSysObject(document);
            content.setContent(markdown.getBytes(StandardCharsets.UTF_8));
            
            // Set primary/secondary based on save option
            if ("primary".equals(saveOption)) {
                content.setPrimary(true);
                logger.info("Saving as primary content");
            } else {
                content.setPrimary(false);
                // Link to source content if saving as secondary rendition
                if (sourceContentId != null) {
                    Content sourceContent = contentService.findById(sourceContentId);
                    if (sourceContent != null) {
                        content.setParentRendition(sourceContent);
                        logger.info("Saving as secondary rendition of content: {} (ID: {})", 
                            sourceContent.getName(), sourceContentId);
                    }
                }
            }
            
            contentService.save(content);
            logger.info("Saved plugin result as markdown: {} ({} bytes, {})", 
                filename, markdown.length(), "primary".equals(saveOption) ? "primary" : "secondary");
            
        } catch (Exception e) {
            logger.error("Failed to save result as markdown", e);
            // Don't throw - saving is optional, don't fail the whole operation
        }
    }
    
    /**
     * Format plugin response as markdown
     */
    private String formatResultAsMarkdown(DocumentPlugin plugin, PluginResponse response) {
        StringBuilder md = new StringBuilder();
        
        md.append("# ").append(plugin.getDescription()).append("\n\n");
        md.append("**Generated**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        md.append("---\n\n");
        
        if (response.getData() != null && !response.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : response.getData().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                logger.debug("Formatting key '{}' with value type: {}, isEmpty: {}", 
                    key, value != null ? value.getClass().getSimpleName() : "null",
                    value instanceof String ? ((String)value).isEmpty() : "N/A");
                
                // Skip null or empty values
                if (value == null || (value instanceof String && ((String)value).trim().isEmpty())) {
                    logger.debug("Skipping empty value for key: {}", key);
                    continue;
                }
                
                md.append("## ").append(formatKey(key)).append("\n\n");
                String formattedValue = formatValue(value);
                md.append(formattedValue);
                
                // Add extra newline if value doesn't end with one
                if (!formattedValue.endsWith("\n")) {
                    md.append("\n");
                }
                md.append("\n");
            }
        }
        
        return md.toString();
    }
    
    /**
     * Format a key for display
     */
    private String formatKey(String key) {
        return key.replaceAll("([A-Z])", " $1")
                  .replaceAll("([a-z])([A-Z])", "$1 $2")
                  .trim()
                  .substring(0, 1).toUpperCase() + 
               key.replaceAll("([A-Z])", " $1")
                  .replaceAll("([a-z])([A-Z])", "$1 $2")
                  .trim()
                  .substring(1);
    }
    
    /**
     * Format a value for markdown
     */
    private String formatValue(Object value) {
        if (value instanceof String) {
            String strValue = (String) value;
            // Ensure proper markdown formatting - preserve existing newlines
            return strValue.trim();
        } else if (value instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) value;
            if (list.isEmpty()) {
                return "*None*";
            }
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                sb.append("- ").append(item.toString()).append("\n");
            }
            return sb.toString();
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return "*None*";
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "Yes" : "No";
        } else {
            return String.valueOf(value);
        }
    }
    
    /**
     * Extract text from a specific content object
     */
    private String extractTextFromContent(Long contentId) {
        Content content = contentService.findById(contentId);
        if (content == null) {
            return null;
        }
        
        // Only extract from text content
        if (content.getContentType() == null || !content.getContentType().startsWith("text/")) {
            return null;
        }
        
        return getContentAsString(content);
    }
    
    /**
     * Extract text content from document (default behavior)
     */
    private String extractTextContent(Document document) {
        if (document.getContents() == null || document.getContents().isEmpty()) {
            return null;
        }
        
        // Look for text/plain content first (preferred)
        Optional<Content> textContent = document.getContents().stream()
            .filter(c -> "text/plain".equals(c.getContentType()))
            .findFirst();
        
        if (textContent.isPresent()) {
            return getContentAsString(textContent.get());
        }
        
        // Fallback to any text/* content
        Optional<Content> anyTextContent = document.getContents().stream()
            .filter(c -> c.getContentType() != null && c.getContentType().startsWith("text/"))
            .findFirst();
        
        if (anyTextContent.isPresent()) {
            return getContentAsString(anyTextContent.get());
        }
        
        return null;
    }
    
    /**
     * Get content as string
     */
    private String getContentAsString(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to read content {}: {}", content.getId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Get all available plugins
     */
    public Map<String, String> getAvailablePlugins() {
        Map<String, String> result = new java.util.HashMap<>();
        for (DocumentPlugin plugin : pluginRegistry.getAllPlugins()) {
            result.put(plugin.getTaskName(), plugin.getDescription());
        }
        return result;
    }
    
    /**
     * Get detailed information about all plugins
     */
    public java.util.List<PluginInfoDTO> getDetailedPluginInfo() {
        java.util.List<PluginInfoDTO> result = new java.util.ArrayList<>();
        for (DocumentPlugin plugin : pluginRegistry.getAllPlugins()) {
            result.add(buildPluginInfo(plugin));
        }
        return result;
    }
    
    /**
     * Get information about a specific plugin
     */
    public PluginInfoDTO getPluginInfo(String taskName) {
        Optional<DocumentPlugin> plugin = pluginRegistry.getPlugin(taskName);
        return plugin.map(this::buildPluginInfo).orElse(null);
    }
    
    private PluginInfoDTO buildPluginInfo(DocumentPlugin plugin) {
        String category = "General";
        String icon = "COG";
        java.util.List<PluginParameter> parameters = java.util.Collections.emptyList();
        
        if (plugin instanceof PluginMetadata) {
            PluginMetadata metadata = (PluginMetadata) plugin;
            category = metadata.getCategory();
            icon = metadata.getIcon();
            parameters = metadata.getParameters();
        }
        
        return PluginInfoDTO.builder()
            .taskName(plugin.getTaskName())
            .description(plugin.getDescription())
            .category(category)
            .icon(icon)
            .parameters(parameters)
            .build();
    }
}
