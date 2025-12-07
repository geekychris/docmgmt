package com.docmgmt.controller;

import com.docmgmt.dto.FileStoreDTO;
import com.docmgmt.dto.SpaceInfoDTO;
import com.docmgmt.model.FileStore;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.service.FileStoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for FileStore operations
 */
@RestController
@RequestMapping("/api/filestores")
@Tag(name = "File Stores", description = "File store management for external content storage")
public class FileStoreController {

    private static final Logger logger = LoggerFactory.getLogger(FileStoreController.class);
    
    private final FileStoreService fileStoreService;
    private final ContentRepository contentRepository;
    
    @Autowired
    public FileStoreController(FileStoreService fileStoreService, ContentRepository contentRepository) {
        this.fileStoreService = fileStoreService;
        this.contentRepository = contentRepository;
    }
    
    /**
     * Get all file stores
     * @return List of file store DTOs
     */
    @GetMapping
    public ResponseEntity<List<FileStoreDTO>> getAllFileStores() {
        try {
            List<FileStoreDTO> fileStores = fileStoreService.findAll().stream()
                    .map(fileStore -> {
                        FileStoreDTO dto = FileStoreDTO.fromEntity(fileStore);
                        dto.setContentCount((long) contentRepository.findByFileStore(fileStore).size());
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(fileStores);
        } catch (Exception e) {
            logger.error("Error getting all file stores", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving file stores", e);
        }
    }
    
    /**
     * Get active file stores
     * @return List of active file store DTOs
     */
    @GetMapping("/active")
    public ResponseEntity<List<FileStoreDTO>> getActiveFileStores() {
        try {
            List<FileStoreDTO> fileStores = fileStoreService.findAllActive().stream()
                    .map(fileStore -> {
                        FileStoreDTO dto = FileStoreDTO.fromEntity(fileStore);
                        dto.setContentCount((long) contentRepository.findByFileStore(fileStore).size());
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(fileStores);
        } catch (Exception e) {
            logger.error("Error getting active file stores", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving active file stores", e);
        }
    }
    
    /**
     * Get file store by ID
     * @param id The file store ID
     * @return The file store DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileStoreDTO> getFileStoreById(@PathVariable Long id) {
        try {
            FileStore fileStore = fileStoreService.findById(id);
            FileStoreDTO dto = FileStoreDTO.fromEntity(fileStore);
            dto.setContentCount((long) contentRepository.findByFileStore(fileStore).size());
            
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving file store", e);
        }
    }
    
    /**
     * Create a new file store
     * @param fileStoreDTO The file store DTO
     * @return The created file store DTO
     */
    @PostMapping
    public ResponseEntity<FileStoreDTO> createFileStore(@Valid @RequestBody FileStoreDTO fileStoreDTO) {
        try {
            // Ensure ID is null for creation
            fileStoreDTO.setId(null);
            
            FileStore fileStore = fileStoreService.save(fileStoreDTO.toEntity());
            return ResponseEntity.status(HttpStatus.CREATED).body(FileStoreDTO.fromEntity(fileStore));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error creating file store", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating file store", e);
        }
    }
    
    /**
     * Update a file store
     * @param id The file store ID
     * @param fileStoreDTO The file store DTO
     * @return The updated file store DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<FileStoreDTO> updateFileStore(@PathVariable Long id, @Valid @RequestBody FileStoreDTO fileStoreDTO) {
        try {
            // Check if file store exists
            fileStoreService.findById(id);
            
            // Set ID from path
            fileStoreDTO.setId(id);
            
            FileStore fileStore = fileStoreService.save(fileStoreDTO.toEntity());
            return ResponseEntity.ok(FileStoreDTO.fromEntity(fileStore));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error updating file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating file store", e);
        }
    }
    
    /**
     * Delete a file store
     * @param id The file store ID
     * @return Empty response with NO_CONTENT status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFileStore(@PathVariable Long id) {
        try {
            fileStoreService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error deleting file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting file store", e);
        }
    }
    
    /**
     * Activate a file store
     * @param id The file store ID
     * @return The updated file store DTO
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<FileStoreDTO> activateFileStore(@PathVariable Long id) {
        try {
            FileStore fileStore = fileStoreService.activate(id);
            return ResponseEntity.ok(FileStoreDTO.fromEntity(fileStore));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error activating file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error activating file store", e);
        }
    }
    
    /**
     * Deactivate a file store
     * @param id The file store ID
     * @return The updated file store DTO
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<FileStoreDTO> deactivateFileStore(@PathVariable Long id) {
        try {
            FileStore fileStore = fileStoreService.deactivate(id);
            return ResponseEntity.ok(FileStoreDTO.fromEntity(fileStore));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error deactivating file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deactivating file store", e);
        }
    }
    
    /**
     * Get available space in a file store
     * @param id The file store ID
     * @return Space information DTO
     */
    @GetMapping("/{id}/space")
    public ResponseEntity<SpaceInfoDTO> getAvailableSpace(@PathVariable Long id) {
        try {
            FileStore fileStore = fileStoreService.findById(id);
            long availableSpace = fileStoreService.getAvailableSpace(id);
            
            SpaceInfoDTO spaceInfo = SpaceInfoDTO.builder()
                    .fileStoreId(fileStore.getId())
                    .fileStoreName(fileStore.getName())
                    .usableSpace(availableSpace)
                    .build();
            
            return ResponseEntity.ok(spaceInfo);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing file system", e);
        } catch (Exception e) {
            logger.error("Error getting available space for file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting available space", e);
        }
    }
    
    /**
     * Check if a file store has enough space
     * @param id The file store ID
     * @param requiredBytes The required space in bytes
     * @return Boolean indicating if enough space is available
     */
    @GetMapping("/{id}/space/check")
    public ResponseEntity<Boolean> hasEnoughSpace(@PathVariable Long id, @RequestParam long requiredBytes) {
        try {
            boolean hasEnoughSpace = fileStoreService.hasEnoughSpace(id, requiredBytes);
            return ResponseEntity.ok(hasEnoughSpace);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing file system", e);
        } catch (Exception e) {
            logger.error("Error checking available space for file store with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error checking available space", e);
        }
    }
}

