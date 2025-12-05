package com.docmgmt.service;

import com.docmgmt.model.Content;
import com.docmgmt.model.SysObject;
import com.docmgmt.repository.BaseSysObjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base service for SysObject entities
 * @param <T> The concrete SysObject type
 * @param <R> The repository type for T
 */
public abstract class AbstractSysObjectService<T extends SysObject, R extends BaseSysObjectRepository<T>> {

    protected final R repository;

    protected AbstractSysObjectService(R repository) {
        this.repository = repository;
    }

    /**
     * Save a SysObject entity
     * @param entity The entity to save
     * @return The saved entity
     */
    @Transactional
    public T save(T entity) {
        // If this is a new entity, set initial version
        if (entity.getId() == null && entity.getMajorVersion() == null) {
            entity.setMajorVersion(1);
            entity.setMinorVersion(0);
        }
        return repository.save(entity);
    }

    /**
     * Find a SysObject by ID
     * @param id The entity ID
     * @return The found entity
     * @throws EntityNotFoundException if the entity is not found
     */
    @Transactional(readOnly = true)
    public T findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));
    }

    /**
     * Find a SysObject by ID, returning Optional
     * @param id The entity ID
     * @return Optional containing the entity if found
     */
    @Transactional(readOnly = true)
    public Optional<T> findByIdOptional(Long id) {
        return repository.findById(id);
    }

    /**
     * Find all SysObjects
     * @return List of all entities
     */
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    /**
     * Find all latest versions of SysObjects
     * @return List of latest versions
     */
    @Transactional(readOnly = true)
    public List<T> findAllLatestVersions() {
        return repository.findLatestVersions();
    }

    /**
     * Find a SysObject by name and version
     * @param name The object name
     * @param majorVersion The major version
     * @param minorVersion The minor version
     * @return The found entity
     * @throws EntityNotFoundException if the entity is not found
     */
    @Transactional(readOnly = true)
    public T findByNameAndVersion(String name, Integer majorVersion, Integer minorVersion) {
        return repository.findByNameAndMajorVersionAndMinorVersion(name, majorVersion, minorVersion)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Entity not found with name: " + name + " and version: " + majorVersion + "." + minorVersion));
    }

    /**
     * Find latest version of a SysObject by name
     * @param name The object name
     * @return The found entity
     * @throws EntityNotFoundException if the entity is not found
     */
    @Transactional(readOnly = true)
    public T findLatestVersionByName(String name) {
        return repository.findLatestVersionByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with name: " + name));
    }

    /**
     * Find all versions of a SysObject by name
     * @param name The object name
     * @return List of all versions
     */
    @Transactional(readOnly = true)
    public List<T> findAllVersionsByName(String name) {
        return repository.findByNameOrderByMajorVersionDescMinorVersionDesc(name);
    }

    /**
     * Create a new major version of a SysObject
     * @param id The ID of the SysObject to version
     * @return The new version
     * @throws EntityNotFoundException if the entity is not found
     */
    @Transactional
    public T createMajorVersion(Long id) {
        T entity = findById(id);
        
        // Find the highest existing major version for this document name
        List<T> allVersions = repository.findByNameOrderByMajorVersionDescMinorVersionDesc(entity.getName());
        int nextMajorVersion = entity.getMajorVersion() + 1;
        
        if (!allVersions.isEmpty()) {
            int highestMajor = allVersions.get(0).getMajorVersion();
            if (highestMajor >= nextMajorVersion) {
                nextMajorVersion = highestMajor + 1;
            }
        }
        
        T newVersion = (T) entity.createMajorVersion();
        newVersion.setMajorVersion(nextMajorVersion);
        newVersion.setMinorVersion(0);
        return repository.save(newVersion);
    }

    /**
     * Create a new minor version of a SysObject
     * @param id The ID of the SysObject to version
     * @return The new version
     * @throws EntityNotFoundException if the entity is not found
     */
    @Transactional
    public T createMinorVersion(Long id) {
        T entity = findById(id);
        
        // Find the highest existing minor version for this major version
        int targetMajorVersion = entity.getMajorVersion();
        List<T> allVersions = repository.findByNameOrderByMajorVersionDescMinorVersionDesc(entity.getName());
        
        int nextMinorVersion = entity.getMinorVersion() + 1;
        
        // Check if there are any versions with the same major version
        for (T version : allVersions) {
            if (version.getMajorVersion().equals(targetMajorVersion)) {
                if (version.getMinorVersion() >= nextMinorVersion) {
                    nextMinorVersion = version.getMinorVersion() + 1;
                }
            }
        }
        
        T newVersion = (T) entity.createMinorVersion();
        newVersion.setMinorVersion(nextMinorVersion);
        return repository.save(newVersion);
    }

    /**
     * Delete a SysObject
     * @param id The ID of the SysObject to delete
     * @throws EntityNotFoundException if the entity is not found
     */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Entity not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Find all child versions of a SysObject
     * @param parentId The ID of the parent SysObject
     * @return List of child versions
     */
    @Transactional(readOnly = true)
    public List<T> findChildVersions(Long parentId) {
        return repository.findByParentVersionId(parentId);
    }

    /**
     * Get the version history chain of a SysObject
     * This includes the object itself, its parent, parent's parent, etc.
     * @param id The ID of the SysObject
     * @return List of versions in the history chain
     */
    @Transactional(readOnly = true)
    public List<T> getVersionHistory(Long id) {
        T current = findById(id);
        List<T> history = new java.util.ArrayList<>();
        history.add(current);
        
        while (current.getParentVersion() != null) {
            @SuppressWarnings("unchecked")
            T parent = (T) current.getParentVersion();
            history.add(parent);
            current = parent;
        }
        
        return history;
    }
}

