package com.docmgmt.service;

import com.docmgmt.model.FileStore;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.repository.FileStoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service for FileStore management
 */
@Service
public class FileStoreService {

    private final FileStoreRepository fileStoreRepository;
    private final ContentRepository contentRepository;

    @Autowired
    public FileStoreService(FileStoreRepository fileStoreRepository, ContentRepository contentRepository) {
        this.fileStoreRepository = fileStoreRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * Find all FileStore entities
     * @return List of all FileStore entities
     */
    @Transactional(readOnly = true)
    public List<FileStore> findAll() {
        List<FileStore> stores = fileStoreRepository.findAll();
        // Initialize the contents collection to prevent lazy loading issues
        stores.forEach(store -> store.getContents().size());
        return stores;
    }

    /**
     * Find all active FileStore entities
     * @return List of active FileStore entities
     */
    @Transactional(readOnly = true)
    public List<FileStore> findAllActive() {
        List<FileStore> stores = fileStoreRepository.findAllActive();
        // Initialize the contents collection to prevent lazy loading issues
        stores.forEach(store -> store.getContents().size());
        return stores;
    }

    /**
     * Find a FileStore by ID
     * @param id The FileStore ID
     * @return The found FileStore
     * @throws EntityNotFoundException if FileStore is not found
     */
    @Transactional(readOnly = true)
    public FileStore findById(Long id) {
        FileStore store = fileStoreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FileStore not found with ID: " + id));
        // Initialize the contents collection to prevent lazy loading issues
        store.getContents().size();
        return store;
    }

    /**
     * Find a FileStore by name
     * @param name The FileStore name
     * @return The found FileStore
     * @throws EntityNotFoundException if FileStore is not found
     */
    @Transactional(readOnly = true)
    public FileStore findByName(String name) {
        return fileStoreRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("FileStore not found with name: " + name));
    }

    /**
     * Save a FileStore
     * @param fileStore The FileStore to save
     * @return The saved FileStore
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public FileStore save(FileStore fileStore) {
        // Check if we're trying to save a FileStore with a name that already exists
        // Do this BEFORE validation to give better error messages
        if (fileStore.getId() == null && fileStoreRepository.existsByName(fileStore.getName())) {
            throw new IllegalArgumentException("FileStore with name '" + fileStore.getName() + "' already exists");
        }
        
        validateFileStore(fileStore);
        
        return fileStoreRepository.save(fileStore);
    }

    /**
     * Delete a FileStore
     * @param id The FileStore ID
     * @throws EntityNotFoundException if FileStore is not found
     * @throws IllegalStateException if FileStore has associated content
     */
    @Transactional
    public void delete(Long id) {
        FileStore fileStore = findById(id);
        
        // Check if there are any content objects using this file store
        long contentCount = contentRepository.findByFileStore(fileStore).size();
        if (contentCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete FileStore with ID " + id + " because it has " + contentCount + " content objects");
        }
        
        fileStoreRepository.delete(fileStore);
    }

    /**
     * Set the status of a FileStore
     * @param id The FileStore ID
     * @param status The new status
     * @return The updated FileStore
     * @throws EntityNotFoundException if FileStore is not found
     */
    @Transactional
    public FileStore setStatus(Long id, FileStore.Status status) {
        FileStore fileStore = findById(id);
        fileStore.setStatus(status);
        return fileStoreRepository.save(fileStore);
    }

    /**
     * Activate a FileStore
     * @param id The FileStore ID
     * @return The activated FileStore
     * @throws EntityNotFoundException if FileStore is not found
     * @throws IllegalStateException if the FileStore path is invalid
     */
    @Transactional
    public FileStore activate(Long id) {
        FileStore fileStore = findById(id);
        validateFileStore(fileStore);
        fileStore.setStatus(FileStore.Status.ACTIVE);
        return fileStoreRepository.save(fileStore);
    }

    /**
     * Deactivate a FileStore
     * @param id The FileStore ID
     * @return The deactivated FileStore
     * @throws EntityNotFoundException if FileStore is not found
     */
    @Transactional
    public FileStore deactivate(Long id) {
        FileStore fileStore = findById(id);
        fileStore.setStatus(FileStore.Status.INACTIVE);
        return fileStoreRepository.save(fileStore);
    }

    /**
     * Get available space in a FileStore
     * @param id The FileStore ID
     * @return Available space in bytes
     * @throws EntityNotFoundException if FileStore is not found
     * @throws IOException if there's an error accessing the file system
     */
    @Transactional(readOnly = true)
    public long getAvailableSpace(Long id) throws IOException {
        FileStore fileStore = findById(id);
        Path path = Paths.get(fileStore.getRootPath());
        
        if (!Files.exists(path)) {
            throw new IllegalStateException("FileStore path does not exist: " + path);
        }
        
        return Files.getFileStore(path).getUsableSpace();
    }

    /**
     * Check if a FileStore has enough space for a specific size
     * @param id The FileStore ID
     * @param requiredBytes The required space in bytes
     * @return true if the FileStore has enough space, false otherwise
     * @throws EntityNotFoundException if FileStore is not found
     * @throws IOException if there's an error accessing the file system
     */
    @Transactional(readOnly = true)
    public boolean hasEnoughSpace(Long id, long requiredBytes) throws IOException {
        return getAvailableSpace(id) >= requiredBytes;
    }

    /**
     * Validate a FileStore
     * Checks if the root path exists and is writable
     * @param fileStore The FileStore to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFileStore(FileStore fileStore) {
        if (fileStore.getName() == null || fileStore.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("FileStore name cannot be empty");
        }

        if (fileStore.getRootPath() == null || fileStore.getRootPath().trim().isEmpty()) {
            throw new IllegalArgumentException("FileStore root path cannot be empty");
        }

        // Check if path exists or can be created
        File directory = new File(fileStore.getRootPath());
        
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalArgumentException("Cannot create directory: " + fileStore.getRootPath());
            }
        }
        
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + fileStore.getRootPath());
        }
        
        if (!directory.canWrite()) {
            throw new IllegalArgumentException("Cannot write to directory: " + fileStore.getRootPath());
        }
    }
}

