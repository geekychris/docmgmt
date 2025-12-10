package com.docmgmt.service;

import com.docmgmt.dto.TileConfigurationDTO;
import com.docmgmt.dto.TileDTO;
import com.docmgmt.model.*;
import com.docmgmt.repository.TileConfigurationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TileService {
    
    private static final Logger logger = LoggerFactory.getLogger(TileService.class);
    
    private final TileConfigurationRepository configRepository;
    private final FolderService folderService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TileService(TileConfigurationRepository configRepository, 
                      FolderService folderService,
                      ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.folderService = folderService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get tile configuration for a folder
     */
    public TileConfiguration getConfiguration(Long folderId) {
        return configRepository.findByFolderId(folderId)
            .orElse(createDefaultConfiguration(folderId));
    }
    
    /**
     * Get tile configuration by folder name
     */
    public TileConfiguration getConfigurationByFolderName(String folderName) {
        List<Folder> folders = folderService.findAllVersionsByName(folderName);
        if (folders.isEmpty()) {
            throw new EntityNotFoundException("Folder not found: " + folderName);
        }
        
        Folder folder = folders.get(0);
        return configRepository.findByFolderId(folder.getId())
            .orElse(createDefaultConfiguration(folder.getId()));
    }
    
    /**
     * Save or update tile configuration
     */
    public TileConfiguration saveConfiguration(TileConfigurationDTO dto) {
        TileConfiguration config;
        
        if (dto.getId() != null) {
            config = configRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Configuration not found"));
        } else {
            config = new TileConfiguration();
        }
        
        // Set folder if specified
        if (dto.getFolderId() != null) {
            Folder folder = folderService.findById(dto.getFolderId());
            config.setFolder(folder);
        }
        
        // Update configuration
        config.setGroupBySubfolder(dto.getGroupBySubfolder());
        config.setVisibleFields(dto.getVisibleFields());
        config.setColorStrategy(dto.getColorStrategy() != null ? 
            TileConfiguration.ColorStrategy.valueOf(dto.getColorStrategy()) : null);
        config.setColorMappings(dto.getColorMappings());
        config.setTileSize(dto.getTileSize() != null ? 
            TileConfiguration.TileSize.valueOf(dto.getTileSize()) : null);
        config.setShowDetailLink(dto.getShowDetailLink());
        config.setShowUrlLink(dto.getShowUrlLink());
        config.setSortOrder(dto.getSortOrder() != null ? 
            TileConfiguration.SortOrder.valueOf(dto.getSortOrder()) : null);
        config.setHideNavigation(dto.getHideNavigation());
        config.setHideEditButtons(dto.getHideEditButtons());
        config.setBackgroundColorOpacity(dto.getBackgroundColorOpacity());
        
        return configRepository.save(config);
    }
    
    /**
     * Get tiles for a folder by name
     */
    @Transactional(readOnly = true)
    public List<TileDTO> getTilesByFolderName(String folderName) {
        List<Folder> folders = folderService.findByNameForTileDisplay(folderName);
        if (folders.isEmpty()) {
            throw new EntityNotFoundException("Folder not found: " + folderName);
        }
        
        Folder folder = folders.get(0);
        TileConfiguration config = getConfiguration(folder.getId());
        
        return getTiles(folder, config);
    }
    
    /**
     * Get tiles for a folder
     */
    public List<TileDTO> getTiles(Folder folder, TileConfiguration config) {
        List<TileDTO> tiles = new ArrayList<>();
        Map<String, String> colorMap = parseColorMappings(config.getColorMappings());
        
        if (config.getGroupBySubfolder()) {
            // Group by subfolders
            for (Folder subfolder : folder.getChildFolders()) {
                String groupName = subfolder.getName();
                String color = determineColor(config, subfolder.getName(), null, colorMap);
                
                for (SysObject item : subfolder.getItems()) {
                    tiles.add(TileDTO.fromSysObject(item, color, groupName));
                }
            }
            
            // Add items directly in this folder (ungrouped)
            String color = determineColor(config, null, null, colorMap);
            for (SysObject item : folder.getItems()) {
                tiles.add(TileDTO.fromSysObject(item, color, null));
            }
        } else {
            // No grouping - just list all items
            String color = determineColor(config, null, null, colorMap);
            for (SysObject item : folder.getItems()) {
                tiles.add(TileDTO.fromSysObject(item, color, null));
            }
        }
        
        // Sort tiles
        sortTiles(tiles, config.getSortOrder());
        
        return tiles;
    }
    
    /**
     * Create default configuration for a folder
     */
    private TileConfiguration createDefaultConfiguration(Long folderId) {
        Folder folder = folderService.findById(folderId);
        
        return TileConfiguration.builder()
            .folder(folder)
            .groupBySubfolder(false)
            .visibleFields("name,description,url")
            .colorStrategy(TileConfiguration.ColorStrategy.NONE)
            .tileSize(TileConfiguration.TileSize.MEDIUM)
            .showDetailLink(true)
            .showUrlLink(true)
            .sortOrder(TileConfiguration.SortOrder.NAME)
            .build();
    }
    
    /**
     * Determine color for a tile based on configuration
     */
    private String determineColor(TileConfiguration config, String folderName, 
                                  Document.DocumentType docType, Map<String, String> colorMap) {
        switch (config.getColorStrategy()) {
            case BY_FOLDER:
                if (folderName != null && colorMap.containsKey(folderName)) {
                    return colorMap.get(folderName);
                }
                return generateColorFromString(folderName);
                
            case BY_TYPE:
                if (docType != null && colorMap.containsKey(docType.name())) {
                    return colorMap.get(docType.name());
                }
                return generateColorFromString(docType != null ? docType.name() : null);
                
            case CUSTOM:
                // Use custom mappings
                return null; // Will be set by caller based on specific key
                
            case NONE:
            default:
                return null;
        }
    }
    
    /**
     * Parse color mappings JSON
     */
    private Map<String, String> parseColorMappings(String colorMappings) {
        if (colorMappings == null || colorMappings.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(colorMappings, 
                new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse color mappings: {}", colorMappings, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Generate a consistent color from a string
     */
    private String generateColorFromString(String input) {
        if (input == null) {
            return "#6c757d"; // Default gray
        }
        
        int hash = input.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        
        // Ensure colors aren't too dark
        r = Math.max(r, 100);
        g = Math.max(g, 100);
        b = Math.max(b, 100);
        
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    /**
     * Sort tiles based on configuration
     */
    private void sortTiles(List<TileDTO> tiles, TileConfiguration.SortOrder sortOrder) {
        if (sortOrder == null) {
            return;
        }
        
        switch (sortOrder) {
            case NAME:
                tiles.sort(Comparator.comparing(TileDTO::getName, 
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                break;
            case TYPE:
                tiles.sort(Comparator.comparing(TileDTO::getDocumentType, 
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                break;
            // Note: CREATED_DATE and MODIFIED_DATE would require additional fields in TileDTO
            default:
                break;
        }
    }
    
    /**
     * Delete configuration
     */
    public void deleteConfiguration(Long id) {
        configRepository.deleteById(id);
    }
}
