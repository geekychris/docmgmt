package com.docmgmt.controller;

import com.docmgmt.dto.BaseSysObjectDTO;
import com.docmgmt.dto.SysObjectVersionDTO;
import com.docmgmt.model.SysObject;
import com.docmgmt.service.AbstractSysObjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract controller for SysObject entities
 * @param <T> The concrete SysObject type
 * @param <D> The concrete DTO type
 * @param <S> The concrete service type
 */
public abstract class AbstractSysObjectController<T extends SysObject, D extends BaseSysObjectDTO, S extends AbstractSysObjectService<T, ?>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSysObjectController.class);
    
    protected final S service;
    
    protected AbstractSysObjectController(S service) {
        this.service = service;
    }
    
    /**
     * Convert entity to DTO
     * This must be implemented by child classes
     * @param entity the entity to convert
     * @return the DTO
     */
    protected abstract D toDTO(T entity);
    
    /**
     * Convert DTO to entity
     * This must be implemented by child classes
     * @param dto the DTO to convert
     * @return the entity
     */
    protected abstract T toEntity(D dto);
    
    /**
     * Update entity from DTO
     * @param entity the entity to update
     * @param dto the DTO with update values
     */
    protected void updateEntityFromDTO(T entity, D dto) {
        dto.updateEntity(entity);
    }
    
    /**
     * Get all objects
     * @return List of all objects as DTOs
     */
    @Operation(summary = "Get all items", description = "Retrieve all items including all versions")
    @GetMapping
    public ResponseEntity<List<D>> getAll() {
        try {
            List<D> dtos = service.findAll().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error retrieving all objects", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving objects", e);
        }
    }
    
    /**
     * Get all latest versions
     * @return List of latest versions as DTOs
     */
    @Operation(summary = "Get latest versions", description = "Retrieve only the latest version of each item")
    @GetMapping("/latest")
    public ResponseEntity<List<D>> getAllLatestVersions() {
        try {
            List<D> dtos = service.findAllLatestVersions().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error retrieving latest object versions", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving latest versions", e);
        }
    }
    
    /**
     * Get object by ID
     * @param id The object ID
     * @return The object as DTO
     */
    @Operation(summary = "Get item by ID", description = "Retrieve a specific item by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item found"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<D> getById(@Parameter(description = "Item ID") @PathVariable Long id) {
        try {
            T entity = service.findById(id);
            return ResponseEntity.ok(toDTO(entity));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error retrieving object with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving object", e);
        }
    }
    
    /**
     * Create a new object
     * @param dto The object data as DTO
     * @return The created object as DTO
     */
    @Operation(summary = "Create item", description = "Create a new item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Item created"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<D> create(@Valid @RequestBody D dto) {
        try {
            // Ensure ID is null for creation
            dto.setId(null);
            
            T entity = toEntity(dto);
            T savedEntity = service.save(entity);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(savedEntity));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error creating object", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating object", e);
        }
    }
    
    /**
     * Update an existing object
     * @param id The object ID
     * @param dto The updated object data as DTO
     * @return The updated object as DTO
     */
    @Operation(summary = "Update item", description = "Update an existing item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item updated"),
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{id}")
    public ResponseEntity<D> update(
            @Parameter(description = "Item ID") @PathVariable Long id, 
            @Valid @RequestBody D dto) {
        try {
            T entity = service.findById(id);
            
            // Update entity from DTO
            updateEntityFromDTO(entity, dto);
            
            T savedEntity = service.save(entity);
            return ResponseEntity.ok(toDTO(savedEntity));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error updating object with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating object", e);
        }
    }
    
    /**
     * Delete an object
     * @param id The object ID
     * @return Empty response with NO_CONTENT status
     */
    @Operation(summary = "Delete item", description = "Delete an item by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Item deleted"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Item ID") @PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error deleting object with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting object", e);
        }
    }
    
    /**
     * Create a major version of an object
     * @param id The object ID
     * @return The new version as DTO
     */
    @Operation(
        summary = "Create major version",
        description = "Create a new major version (e.g., 1.0 → 2.0)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Major version created"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @PostMapping("/{id}/versions/major")
    public ResponseEntity<D> createMajorVersion(
            @Parameter(description = "Item ID") @PathVariable Long id) {
        try {
            T newVersion = service.createMajorVersion(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(newVersion));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error creating major version for object with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating major version", e);
        }
    }
    
    /**
     * Create a minor version of an object
     * @param id The object ID
     * @return The new version as DTO
     */
    @Operation(
        summary = "Create minor version",
        description = "Create a new minor version (e.g., 1.0 → 1.1)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Minor version created"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @PostMapping("/{id}/versions/minor")
    public ResponseEntity<D> createMinorVersion(
            @Parameter(description = "Item ID") @PathVariable Long id) {
        try {
            T newVersion = service.createMinorVersion(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(newVersion));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error creating minor version for object with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating minor version", e);
        }
    }
    
    /**
     * Get version history of an object
     * @param id The object ID
     * @return List of version information DTOs
     */
    @Operation(
        summary = "Get version history",
        description = "Retrieve complete version history for an item"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version history retrieved"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @GetMapping("/{id}/versions/history")
    public ResponseEntity<List<SysObjectVersionDTO>> getVersionHistory(
            @Parameter(description = "Item ID") @PathVariable Long id) {
        try {
            List<T> history = service.getVersionHistory(id);
            
            // Mark the first object as the current version
            List<SysObjectVersionDTO> versionDTOs = new java.util.ArrayList<>();
            
            if (!history.isEmpty()) {
                for (int i = 0; i < history.size(); i++) {
                    boolean isLatest = (i == 0);
                    boolean hasChildVersions = (i > 0) || !service.findChildVersions(history.get(i).getId()).isEmpty();
                    
                    versionDTOs.add(SysObjectVersionDTO.fromEntity(history.get(i), isLatest, hasChildVersions));
                }
            }
            
            return ResponseEntity.ok(versionDTOs);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error retrieving version history for object with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving version history", e);
        }
    }
}
