package com.docmgmt.controller;

import com.docmgmt.dto.ContentDTO;
import com.docmgmt.dto.ContentUploadDTO;
import com.docmgmt.model.Content;
import com.docmgmt.model.SysObject;
import com.docmgmt.service.AbstractSysObjectService;
import com.docmgmt.service.ContentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Content operations
 */
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    
    private final ContentService contentService;
    private final AbstractSysObjectService<SysObject, ?> sysObjectService;
    
    @Autowired
    public ContentController(ContentService contentService, 
                            AbstractSysObjectService<SysObject, ?> sysObjectService) {
        this.contentService = contentService;
        this.sysObjectService = sysObjectService;
    }
    
    /**
     * Get content by ID
     * @param id The content ID
     * @return Content metadata DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContentDTO> getContentById(@PathVariable Long id) {
        try {
            Content content = contentService.findById(id);
            return ResponseEntity.ok(ContentDTO.fromEntity(content));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting content with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving content", e);
        }
    }
    
    /**
     * List all content for a SysObject
     * @param sysObjectId The ID of the SysObject to find content for
     * @return List of content metadata DTOs
     */
    @GetMapping("/by-sysobject/{sysObjectId}")
    public ResponseEntity<List<ContentDTO>> getContentBySysObject(@PathVariable Long sysObjectId) {
        try {
            // Find the SysObject by ID
            SysObject sysObject = findSysObjectById(sysObjectId);
            
            List<Content> contents = contentService.findBySysObject(sysObject);
            List<ContentDTO> contentDTOs = contents.stream()
                    .map(ContentDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(contentDTOs);
        } catch (Exception e) {
            logger.error("Error getting content for SysObject with ID: {}", sysObjectId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving content", e);
        }
    }
    
    /**
     * Upload content
     * @param file The file to upload
     * @param uploadDTO The upload parameters
     * @return The uploaded content metadata DTO
     */
    @PostMapping("/upload")
    @Transactional
    public ResponseEntity<ContentDTO> uploadContent(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute ContentUploadDTO uploadDTO) {
        
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }
            
            // Find the SysObject if ID is provided
            SysObject sysObject = null;
            if (uploadDTO.getSysObjectId() != null) {
                sysObject = findSysObjectById(uploadDTO.getSysObjectId());
            }
            
            Content content;
            if (uploadDTO.shouldStoreInDatabase()) {
                content = contentService.createContentInDatabase(file, sysObject);
            } else {
                content = contentService.createContentInFileStore(file, sysObject, uploadDTO.getFileStoreId());
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(ContentDTO.fromEntity(content));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error storing file", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error storing file", e);
        } catch (Exception e) {
            logger.error("Error uploading content", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading content", e);
        }
    }
    
    /**
     * Download content
     * @param id The content ID
     * @return The content as a downloadable resource
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadContent(@PathVariable Long id) {
        try {
            Content content = contentService.findById(id);
            byte[] data = contentService.getContentBytes(id);
            
            ByteArrayResource resource = new ByteArrayResource(data);
            
            // Set content disposition header to suggest filename for download
            String encodedFilename = URLEncoder.encode(content.getName(), StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.builder("attachment")
                            .filename(encodedFilename)
                            .build());
            
            // Set content type if available
            if (content.getContentType() != null && !content.getContentType().isEmpty()) {
                headers.setContentType(MediaType.parseMediaType(content.getContentType()));
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            
            headers.setContentLength(data.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error reading content data for ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading content data", e);
        } catch (Exception e) {
            logger.error("Error downloading content with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading content", e);
        }
    }
    
    /**
     * Delete content
     * @param id The content ID
     * @return Empty response with NO_CONTENT status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        try {
            contentService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error deleting content with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting content", e);
        }
    }
    
    /**
     * Move content from database to file store
     * @param id The content ID
     * @param fileStoreId The target file store ID
     * @return The updated content metadata DTO
     */
    @PutMapping("/{id}/move-to-filestore")
    @Transactional
    public ResponseEntity<ContentDTO> moveToFileStore(
            @PathVariable Long id,
            @RequestParam Long fileStoreId) {
        
        try {
            Content content = contentService.moveToFileStore(id, fileStoreId);
            return ResponseEntity.ok(ContentDTO.fromEntity(content));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error accessing file system", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing file system", e);
        } catch (Exception e) {
            logger.error("Error moving content with ID: {} to file store", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error moving content to file store", e);
        }
    }
    
    /**
     * Move content from file store to database
     * @param id The content ID
     * @return The updated content metadata DTO
     */
    @PutMapping("/{id}/move-to-database")
    @Transactional
    public ResponseEntity<ContentDTO> moveToDatabase(@PathVariable Long id) {
        try {
            Content content = contentService.moveToDatabase(id);
            return ResponseEntity.ok(ContentDTO.fromEntity(content));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error accessing file system", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing file system", e);
        } catch (Exception e) {
            logger.error("Error moving content with ID: {} to database", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error moving content to database", e);
        }
    }
    
    /**
     * Helper method to find a SysObject by ID
     * @param id the SysObject ID
     * @return the SysObject
     * @throws ResponseStatusException if the SysObject is not found
     */
    private SysObject findSysObjectById(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("SysObject ID cannot be null");
            }
            return sysObjectService.findById(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "SysObject not found with ID: " + id, e);
        } catch (Exception e) {
            logger.error("Error finding SysObject with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error finding SysObject", e);
        }
    }
}
