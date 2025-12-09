package com.docmgmt.controller;

import com.docmgmt.dto.PluginInfoDTO;
import com.docmgmt.plugin.PluginException;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.plugin.PluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.Map;

/**
 * REST controller for document plugin operations
 */
@RestController
@RequestMapping("/api/documents/{documentId}/plugins")
@Tag(name = "Document Plugins", description = "Execute plugins on documents")
public class DocumentPluginController {
    
    private final PluginService pluginService;
    
    public DocumentPluginController(PluginService pluginService) {
        this.pluginService = pluginService;
    }
    
    /**
     * Execute a plugin on a document
     */
    @PostMapping("/{taskName}")
    @Operation(summary = "Execute a plugin task on a document")
    public ResponseEntity<?> executePlugin(
            @PathVariable Long documentId,
            @PathVariable String taskName,
            @RequestBody(required = false) Map<String, Object> parameters,
            @RequestParam(required = false, defaultValue = "false") boolean saveAsMarkdown) {
        
        try {
            if (parameters == null) {
                parameters = Map.of();
            }
            
            PluginResponse response = pluginService.executePlugin(documentId, taskName, parameters, saveAsMarkdown);
            
            return ResponseEntity.ok(Map.of(
                "status", response.getStatus(),
                "data", response.getData() != null ? response.getData() : Map.of(),
                "error", response.getErrorMessage(),
                "savedAsMarkdown", saveAsMarkdown
            ));
            
        } catch (PluginException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "FAILURE",
                    "error", e.getMessage()
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "FAILURE",
                    "error", "Internal error: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Get list of available plugins (simple)
     */
    @GetMapping
    @Operation(summary = "Get available plugins for documents")
    public ResponseEntity<Map<String, String>> getAvailablePlugins() {
        Map<String, String> plugins = pluginService.getAvailablePlugins();
        return ResponseEntity.ok(plugins);
    }
    
    /**
     * Get detailed plugin information including parameters
     */
    @GetMapping("/detailed")
    @Operation(summary = "Get detailed information about all plugins")
    public ResponseEntity<List<PluginInfoDTO>> getDetailedPluginInfo() {
        List<PluginInfoDTO> plugins = pluginService.getDetailedPluginInfo();
        return ResponseEntity.ok(plugins);
    }
    
    /**
     * Get information about a specific plugin
     */
    @GetMapping("/info/{taskName}")
    @Operation(summary = "Get information about a specific plugin")
    public ResponseEntity<?> getPluginInfo(@PathVariable String taskName) {
        try {
            PluginInfoDTO info = pluginService.getPluginInfo(taskName);
            if (info == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Plugin not found: " + taskName));
            }
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
