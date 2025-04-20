package com.docmgmt.repository;

import com.docmgmt.model.FileStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FileStore entity
 */
@Repository
public interface FileStoreRepository extends JpaRepository<FileStore, Long> {

    /**
     * Find FileStore by name
     * @param name The FileStore name
     * @return Optional containing the matching FileStore if found
     */
    Optional<FileStore> findByName(String name);
    
    /**
     * Find all active FileStore objects
     * @return List of active FileStore objects
     */
    List<FileStore> findByStatus(FileStore.Status status);
    
    /**
     * Convenience method to find all active FileStore objects
     * @return List of active FileStore objects
     */
    default List<FileStore> findAllActive() {
        return findByStatus(FileStore.Status.ACTIVE);
    }
    
    /**
     * Convenience method to find all inactive FileStore objects
     * @return List of inactive FileStore objects
     */
    default List<FileStore> findAllInactive() {
        return findByStatus(FileStore.Status.INACTIVE);
    }
    
    /**
     * Check if a FileStore with the given name exists
     * @param name The FileStore name
     * @return True if a FileStore with the name exists, false otherwise
     */
    boolean existsByName(String name);
}

