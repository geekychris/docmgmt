package com.docmgmt.repository;

import com.docmgmt.model.TileConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TileConfigurationRepository extends JpaRepository<TileConfiguration, Long> {
    
    /**
     * Find tile configuration by folder ID
     */
    Optional<TileConfiguration> findByFolderId(Long folderId);
    
    /**
     * Find tile configuration by folder name
     */
    Optional<TileConfiguration> findByFolderName(String folderName);
}
