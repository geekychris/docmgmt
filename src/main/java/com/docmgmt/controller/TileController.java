package com.docmgmt.controller;

import com.docmgmt.dto.TileConfigurationDTO;
import com.docmgmt.dto.TileDTO;
import com.docmgmt.model.TileConfiguration;
import com.docmgmt.service.TileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for tile display operations
 */
@RestController
@RequestMapping("/api/tiles")
@Tag(name = "Tiles", description = "Tile display and configuration operations")
public class TileController {
    
    private static final Logger logger = LoggerFactory.getLogger(TileController.class);
    
    private final TileService tileService;
    
    @Autowired
    public TileController(TileService tileService) {
        this.tileService = tileService;
    }
    
    /**
     * Get tiles for a folder by name
     */
    @Operation(
        summary = "Get tiles for a folder",
        description = "Retrieve all tiles (documents) for display in the specified folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tiles retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{folderName}")
    public ResponseEntity<List<TileDTO>> getTilesByFolderName(
            @Parameter(description = "Folder name", required = true)
            @PathVariable String folderName) {
        try {
            List<TileDTO> tiles = tileService.getTilesByFolderName(folderName);
            return ResponseEntity.ok(tiles);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting tiles for folder: {}", folderName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error retrieving tiles", e);
        }
    }
    
    /**
     * Get tile configuration for a folder
     */
    @Operation(
        summary = "Get tile configuration",
        description = "Retrieve tile display configuration for a folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/config/{folderId}")
    public ResponseEntity<TileConfigurationDTO> getConfiguration(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId) {
        try {
            TileConfiguration config = tileService.getConfiguration(folderId);
            return ResponseEntity.ok(TileConfigurationDTO.fromEntity(config));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting configuration for folder: {}", folderId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error retrieving configuration", e);
        }
    }
    
    /**
     * Get tile configuration by folder name
     */
    @Operation(
        summary = "Get tile configuration by folder name",
        description = "Retrieve tile display configuration for a folder by name"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/config/by-name/{folderName}")
    public ResponseEntity<TileConfigurationDTO> getConfigurationByName(
            @Parameter(description = "Folder name", required = true)
            @PathVariable String folderName) {
        try {
            TileConfiguration config = tileService.getConfigurationByFolderName(folderName);
            return ResponseEntity.ok(TileConfigurationDTO.fromEntity(config));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting configuration for folder: {}", folderName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error retrieving configuration", e);
        }
    }
    
    /**
     * Save or update tile configuration
     */
    @Operation(
        summary = "Save tile configuration",
        description = "Create or update tile display configuration for a folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration saved successfully"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/config")
    public ResponseEntity<TileConfigurationDTO> saveConfiguration(
            @RequestBody TileConfigurationDTO dto) {
        try {
            TileConfiguration config = tileService.saveConfiguration(dto);
            return ResponseEntity.ok(TileConfigurationDTO.fromEntity(config));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error saving configuration", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error saving configuration", e);
        }
    }
    
    /**
     * Delete tile configuration
     */
    @Operation(
        summary = "Delete tile configuration",
        description = "Delete tile display configuration (will revert to defaults)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Configuration deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/config/{id}")
    public ResponseEntity<Void> deleteConfiguration(
            @Parameter(description = "Configuration ID", required = true)
            @PathVariable Long id) {
        try {
            tileService.deleteConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error deleting configuration: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error deleting configuration", e);
        }
    }
}
