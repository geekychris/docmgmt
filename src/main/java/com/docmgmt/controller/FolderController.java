package com.docmgmt.controller;

import com.docmgmt.dto.FolderDTO;
import com.docmgmt.model.Folder;
import com.docmgmt.model.SysObject;
import com.docmgmt.service.AbstractSysObjectService;
import com.docmgmt.service.FolderService;
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
import java.util.stream.Collectors;

/**
 * REST controller for Folder operations
 */
@RestController
@RequestMapping("/api/folders")
@Tag(name = "Folders", description = "Folder management operations for organizing documents")
public class FolderController extends AbstractSysObjectController<Folder, FolderDTO, FolderService> {

    private static final Logger logger = LoggerFactory.getLogger(FolderController.class);
    
    private final AbstractSysObjectService<SysObject, ?> sysObjectService;
    
    @Autowired
    public FolderController(FolderService service, AbstractSysObjectService<SysObject, ?> sysObjectService) {
        super(service);
        this.sysObjectService = sysObjectService;
    }
    
    @Override
    protected FolderDTO toDTO(Folder entity) {
        return FolderDTO.fromEntity(entity);
    }
    
    @Override
    protected Folder toEntity(FolderDTO dto) {
        return dto.toEntity();
    }
    
    /**
     * Find folders by path
     * @param path The folder path
     * @return List of folder DTOs
     */
    @Operation(
        summary = "Find folders by path",
        description = "Retrieve all folders with an exact path match"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Folders found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-path")
    public ResponseEntity<List<FolderDTO>> findByPath(
            @Parameter(description = "Folder path to search for", required = true, example = "/documents/reports")
            @RequestParam String path) {
        try {
            List<FolderDTO> folders = service.findByPath(path).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            logger.error("Error finding folders by path: {}", path, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding folders", e);
        }
    }
    
    /**
     * Find folders by path prefix
     * @param pathPrefix The path prefix
     * @return List of folder DTOs
     */
    @Operation(
        summary = "Find folders by path prefix",
        description = "Retrieve all folders whose path starts with the given prefix"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Folders found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-path-prefix")
    public ResponseEntity<List<FolderDTO>> findByPathStartingWith(
            @Parameter(description = "Path prefix to search for", required = true, example = "/documents")
            @RequestParam String pathPrefix) {
        try {
            List<FolderDTO> folders = service.findByPathStartingWith(pathPrefix).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            logger.error("Error finding folders by path prefix: {}", pathPrefix, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding folders", e);
        }
    }
    
    /**
     * Find root folders
     * @return List of root folder DTOs
     */
    @Operation(
        summary = "Find root folders",
        description = "Retrieve all folders that have no parent folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Root folders found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/roots")
    public ResponseEntity<List<FolderDTO>> findRootFolders() {
        try {
            List<FolderDTO> folders = service.findRootFolders().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            logger.error("Error finding root folders", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding root folders", e);
        }
    }
    
    /**
     * Find child folders
     * @param parentId The parent folder ID
     * @return List of child folder DTOs
     */
    @Operation(
        summary = "Find child folders",
        description = "Retrieve all folders that are children of the specified parent folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Child folders found"),
        @ApiResponse(responseCode = "404", description = "Parent folder not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<FolderDTO>> findChildFolders(
            @Parameter(description = "Parent folder ID", required = true)
            @PathVariable Long parentId) {
        try {
            Folder parent = service.findById(parentId);
            List<FolderDTO> folders = service.findChildFolders(parent).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(folders);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error finding child folders for parent ID: {}", parentId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding child folders", e);
        }
    }
    
    /**
     * Get folder hierarchy
     * @param rootId The root folder ID
     * @return List of all folders in the hierarchy
     */
    @Operation(
        summary = "Get folder hierarchy",
        description = "Retrieve all folders in a hierarchy starting from the specified root folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hierarchy retrieved"),
        @ApiResponse(responseCode = "404", description = "Root folder not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{rootId}/hierarchy")
    public ResponseEntity<List<FolderDTO>> getFolderHierarchy(
            @Parameter(description = "Root folder ID", required = true)
            @PathVariable Long rootId) {
        try {
            List<FolderDTO> folders = service.getFolderHierarchy(rootId).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(folders);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting folder hierarchy for root ID: {}", rootId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting folder hierarchy", e);
        }
    }
    
    /**
     * Add item to folder
     * @param folderId The folder ID
     * @param itemId The SysObject ID to add
     * @return The updated folder DTO
     */
    @Operation(
        summary = "Add item to folder",
        description = "Add a SysObject (document, folder, etc.) to a folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added successfully"),
        @ApiResponse(responseCode = "404", description = "Folder or item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{folderId}/items/{itemId}")
    public ResponseEntity<FolderDTO> addItemToFolder(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId,
            @Parameter(description = "Item (SysObject) ID to add", required = true)
            @PathVariable Long itemId) {
        try {
            SysObject item = sysObjectService.findById(itemId);
            Folder folder = service.addItemToFolder(folderId, item);
            
            return ResponseEntity.ok(toDTO(folder));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error adding item {} to folder {}", itemId, folderId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding item to folder", e);
        }
    }
    
    /**
     * Remove item from folder
     * @param folderId The folder ID
     * @param itemId The SysObject ID to remove
     * @return The updated folder DTO
     */
    @Operation(
        summary = "Remove item from folder",
        description = "Remove a SysObject from a folder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item removed successfully"),
        @ApiResponse(responseCode = "404", description = "Folder or item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{folderId}/items/{itemId}")
    public ResponseEntity<FolderDTO> removeItemFromFolder(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId,
            @Parameter(description = "Item (SysObject) ID to remove", required = true)
            @PathVariable Long itemId) {
        try {
            SysObject item = sysObjectService.findById(itemId);
            Folder folder = service.removeItemFromFolder(folderId, item);
            
            return ResponseEntity.ok(toDTO(folder));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error removing item {} from folder {}", itemId, folderId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error removing item from folder", e);
        }
    }
}
