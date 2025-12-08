package com.docmgmt.repository;

import com.docmgmt.model.SysObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface for SysObject entities
 * @param <T> The concrete SysObject type
 */
@NoRepositoryBean
public interface BaseSysObjectRepository<T extends SysObject> extends JpaRepository<T, Long> {

    /**
     * Find by specific version
     * @param majorVersion The major version
     * @param minorVersion The minor version
     * @return List of objects with the specified version
     */
    List<T> findByMajorVersionAndMinorVersion(Integer majorVersion, Integer minorVersion);
    
    /**
     * Find latest version of objects
     * @return List of objects with the latest version
     */
    @Query("SELECT o FROM #{#entityName} o " +
           "WHERE NOT EXISTS (SELECT 1 FROM #{#entityName} newer " +
           "WHERE newer.parentVersion = o)")
    List<T> findLatestVersions();
    
    /**
     * Find latest version of objects with pagination
     * @param pageable Pagination information
     * @return Page of objects with the latest version
     */
    @Query("SELECT o FROM #{#entityName} o " +
           "WHERE NOT EXISTS (SELECT 1 FROM #{#entityName} newer " +
           "WHERE newer.parentVersion = o)")
    Page<T> findLatestVersionsPaginated(Pageable pageable);
    
    /**
     * Count latest versions only
     * @return Count of latest versions
     */
    @Query("SELECT COUNT(o) FROM #{#entityName} o " +
           "WHERE NOT EXISTS (SELECT 1 FROM #{#entityName} newer " +
           "WHERE newer.parentVersion = o)")
    long countLatestVersions();
    
    /**
     * Find object by name and specific version
     * @param name The object name
     * @param majorVersion The major version
     * @param minorVersion The minor version
     * @return Optional containing the matching object if found
     */
    Optional<T> findByNameAndMajorVersionAndMinorVersion(String name, Integer majorVersion, Integer minorVersion);
    
    /**
     * Find latest version of an object by name
     * @param name The object name
     * @return Optional containing the latest version if found
     */
    @Query("SELECT o FROM #{#entityName} o " +
           "WHERE o.name = :name " +
           "AND NOT EXISTS (SELECT 1 FROM #{#entityName} newer " +
           "WHERE newer.parentVersion.id = o.id)")
    Optional<T> findLatestVersionByName(@Param("name") String name);
    
    /**
     * Find all versions of an object by name
     * @param name The object name
     * @return List of all versions of the object
     */
    List<T> findByNameOrderByMajorVersionDescMinorVersionDesc(String name);
    
    /**
     * Find all objects where the specified object is the parent version
     * @param parentId The ID of the parent version
     * @return List of child versions
     */
    List<T> findByParentVersionId(Long parentId);
}

