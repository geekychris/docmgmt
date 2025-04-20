package com.docmgmt.repository;

import com.docmgmt.model.Content;
import com.docmgmt.model.FileStore;
import com.docmgmt.model.SysObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Content entity
 */
@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
     * Find all content objects for a specific SysObject
     * @param sysObject The SysObject
     * @return List of Content objects
     */
    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.fileStore WHERE c.sysObject = :sysObject")
    List<Content> findBySysObject(@Param("sysObject") SysObject sysObject);

    /**
     * Find all content objects for a specific SysObject
     * @param sysObject The SysObject
     * @return List of Content objects
     */
    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.fileStore LEFT JOIN FETCH c.sysObject")
    List<Content> findAll();
    
    /**
     * Find content by SysObject and name
     * @param sysObject The SysObject
     * @param name The content name
     * @return Optional containing the matching Content if found
     */
    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.fileStore WHERE c.sysObject = :sysObject AND c.name = :name")
    Optional<Content> findBySysObjectAndName(@Param("sysObject") SysObject sysObject, @Param("name") String name);
    
    /**
     * Find all content objects stored in a specific FileStore
     * @param fileStore The FileStore
     * @return List of Content objects
     */
    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.sysObject WHERE c.fileStore = :fileStore")
    List<Content> findByFileStore(@Param("fileStore") FileStore fileStore);
    
    /**
     * Find content objects with database storage (no FileStore)
     * @return List of Content objects stored in the database
     */
    List<Content> findByFileStoreIsNull();
    
    /**
     * Find content by content type
     * @param contentType The MIME type of the content
     * @return List of matching Content objects
     */
    List<Content> findByContentType(String contentType);
    
    /**
     * Find content by SysObject ID
     * @param sysObjectId The ID of the SysObject
     * @return List of Content objects
     */
    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.fileStore WHERE c.sysObject.id = :sysObjectId")
    List<Content> findBySysObjectId(@Param("sysObjectId") Long sysObjectId);
    
    /**
     * Count content objects by SysObject
     * @param sysObject The SysObject
     * @return Number of content objects
     */
    long countBySysObject(SysObject sysObject);
    
    /**
     * Find content objects using a path pattern in FileStore
     * @param pathPattern The pattern to match against the storage path
     * @return List of matching Content objects
     */
    @Query("SELECT c FROM Content c WHERE c.fileStore IS NOT NULL AND c.storagePath LIKE %:pathPattern%")
    List<Content> findByStoragePathPattern(@Param("pathPattern") String pathPattern);
}

