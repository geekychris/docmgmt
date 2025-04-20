package com.docmgmt.service;

import com.docmgmt.model.Content;
import com.docmgmt.model.FileStore;
import com.docmgmt.model.SysObject;
import com.docmgmt.repository.ContentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Service for Content management
 */
@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final FileStoreService fileStoreService;

    @Autowired
    public ContentService(ContentRepository contentRepository, FileStoreService fileStoreService) {
        this.contentRepository = contentRepository;
        this.fileStoreService = fileStoreService;
    }

    /**
     * Find content by ID
     * @param id The content ID
     * @return The found content
     * @throws EntityNotFoundException if content is not found
     */
    @Transactional(readOnly = true)
    public Content findById(Long id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + id));
        
        // Initialize associations to prevent lazy loading issues
        if (content.getFileStore() != null) {
            content.getFileStore().getName(); // Touch to initialize
        }
        if (content.getSysObject() != null) {
            content.getSysObject().getName(); // Touch to initialize
        }
        
        return content;
    }

    /**
     * Find all content for a SysObject
     * @param sysObject The SysObject
     * @return List of content objects
     */
    @Transactional(readOnly = true)
    public List<Content> findBySysObject(SysObject sysObject) {
        return contentRepository.findBySysObject(sysObject);
    }


    @Transactional(readOnly = true)
    public List<Content> findAll() {
        List<Content> contents = contentRepository.findAll();
        
        // The repository uses JOIN FETCH, but let's ensure everything is initialized
        for (Content content : contents) {
            if (content.getFileStore() != null) {
                content.getFileStore().getName(); // Touch to initialize
            }
            if (content.getSysObject() != null) {
                content.getSysObject().getName(); // Touch to initialize
            }
        }
        
        return contents;
    }

    /**
     * Find content by SysObject and name
     * @param sysObject The SysObject
     * @param name The content name
     * @return The found content
     * @throws EntityNotFoundException if content is not found
     */
    @Transactional(readOnly = true)
    public Content findBySysObjectAndName(SysObject sysObject, String name) {
        return contentRepository.findBySysObjectAndName(sysObject, name)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Content not found with name: " + name + " for SysObject ID: " + sysObject.getId()));
    }

    /**
     * Save content to database
     * @param content The content to save
     * @return The saved content
     */
    @Transactional
    public Content save(Content content) {
        return contentRepository.save(content);
    }

    /**
     * Delete content
     * @param id The content ID
     * @throws EntityNotFoundException if content is not found
     */
    @Transactional
    public void delete(Long id) {
        Content content = findById(id);
        
        try {
            // If content is stored in file system, delete the file
            if (content.isStoredInFileStore()) {
                Path filePath = Paths.get(content.getFileStore().getFullPath(content.getStoragePath()));
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete content file", e);
        }
        
        contentRepository.delete(content);
    }

    /**
     * Create content from a MultipartFile and store in database
     * @param file The uploaded file
     * @param sysObject The parent SysObject
     * @return The created content
     */
    @Transactional
    public Content createContentInDatabase(MultipartFile file, SysObject sysObject) throws IOException {
        Content content = new Content();
        content.setName(file.getOriginalFilename());
        content.setContentType(file.getContentType());
        content.setSysObject(sysObject);
        content.setContent(file.getBytes());
        
        return contentRepository.save(content);
    }

    /**
     * Create content from a MultipartFile and store in the specified FileStore
     * @param file The uploaded file
     * @param sysObject The parent SysObject
     * @param fileStoreId The ID of the FileStore to use
     * @return The created content
     * @throws IOException if file storage fails
     */
    @Transactional
    public Content createContentInFileStore(MultipartFile file, SysObject sysObject, Long fileStoreId) throws IOException {
        FileStore fileStore = fileStoreService.findById(fileStoreId);
        
        if (!fileStore.isActive()) {
            throw new IllegalStateException("FileStore is not active: " + fileStore.getName());
        }
        
        // Generate a unique path for the file to avoid collisions
        String storagePath = generateStoragePath(file.getOriginalFilename());
        
        Content content = new Content();
        content.setName(file.getOriginalFilename());
        content.setContentType(file.getContentType());
        content.setSysObject(sysObject);
        content.setFileStore(fileStore);
        content.setStoragePath(storagePath);
        
        // Create the content entity first
        content = contentRepository.save(content);
        
        // Then store the file
        content.setContentBytes(file.getBytes());
        
        return content;
    }

    /**
     * Get content bytes
     * @param id The content ID
     * @return The content bytes
     * @throws IOException if file access fails
     */
    @Transactional(readOnly = true)
    public byte[] getContentBytes(Long id) throws IOException {
        Content content = findById(id);
        return content.getContentBytes();
    }

    /**
     * Generate a unique storage path for a file
     * Uses a UUID to ensure uniqueness
     * @param originalFilename The original filename
     * @return A unique storage path
     */
    private String generateStoragePath(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        
        return uuid + extension;
    }

    /**
     * Move content from database to file store
     * @param contentId The content ID
     * @param fileStoreId The target FileStore ID
     * @return The updated content
     * @throws IOException if file operation fails
     */
    @Transactional
    public Content moveToFileStore(Long contentId, Long fileStoreId) throws IOException {
        Content content = findById(contentId);
        
        if (content.isStoredInFileStore()) {
            throw new IllegalStateException("Content is already stored in a file store");
        }
        
        FileStore fileStore = fileStoreService.findById(fileStoreId);
        
        if (!fileStore.isActive()) {
            throw new IllegalStateException("FileStore is not active: " + fileStore.getName());
        }
        
        // Get content bytes from database
        byte[] bytes = content.getContent();
        
        if (bytes == null) {
            throw new IllegalStateException("No content bytes found in database");
        }
        
        // Set up file store storage
        String storagePath = generateStoragePath(content.getName());
        content.setFileStore(fileStore);
        content.setStoragePath(storagePath);
        
        // Save the content entity with the new file store reference
        content = contentRepository.save(content);
        
        // Write the bytes to file store
        content.setContentBytes(bytes);
        
        return content;
    }

    /**
     * Move content from file store to database
     * @param contentId The content ID
     * @return The updated content
     * @throws IOException if file operation fails
     */
    @Transactional
    public Content moveToDatabase(Long contentId) throws IOException {
        Content content = findById(contentId);
        
        if (!content.isStoredInFileStore()) {
            throw new IllegalStateException("Content is already stored in the database");
        }
        
        // Read bytes from file store
        byte[] bytes = content.getContentBytes();
        
        // Update content entity to use database storage
        content.setContent(bytes);
        content.setFileStore(null);
        content.setStoragePath(null);
        
        return contentRepository.save(content);
    }
}

