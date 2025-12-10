package com.docmgmt.dto;

import com.docmgmt.model.TileConfiguration;
import lombok.*;

/**
 * DTO for TileConfiguration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TileConfigurationDTO {
    
    private Long id;
    private Long folderId;
    private String folderName;
    private Boolean groupBySubfolder;
    private String visibleFields;
    private String colorStrategy;
    private String colorMappings;
    private String tileSize;
    private Boolean showDetailLink;
    private Boolean showUrlLink;
    private String sortOrder;
    private Boolean hideNavigation;
    private Boolean hideEditButtons;
    private Double backgroundColorOpacity;
    
    /**
     * Convert entity to DTO
     */
    public static TileConfigurationDTO fromEntity(TileConfiguration config) {
        if (config == null) {
            return null;
        }
        
        return TileConfigurationDTO.builder()
            .id(config.getId())
            .folderId(config.getFolder() != null ? config.getFolder().getId() : null)
            .folderName(config.getFolder() != null ? config.getFolder().getName() : null)
            .groupBySubfolder(config.getGroupBySubfolder())
            .visibleFields(config.getVisibleFields())
            .colorStrategy(config.getColorStrategy() != null ? config.getColorStrategy().name() : null)
            .colorMappings(config.getColorMappings())
            .tileSize(config.getTileSize() != null ? config.getTileSize().name() : null)
            .showDetailLink(config.getShowDetailLink())
            .showUrlLink(config.getShowUrlLink())
            .sortOrder(config.getSortOrder() != null ? config.getSortOrder().name() : null)
            .hideNavigation(config.getHideNavigation())
            .hideEditButtons(config.getHideEditButtons())
            .backgroundColorOpacity(config.getBackgroundColorOpacity())
            .build();
    }
    
    /**
     * Convert DTO to entity
     */
    public TileConfiguration toEntity() {
        return TileConfiguration.builder()
            .id(this.id)
            .groupBySubfolder(this.groupBySubfolder)
            .visibleFields(this.visibleFields)
            .colorStrategy(this.colorStrategy != null ? TileConfiguration.ColorStrategy.valueOf(this.colorStrategy) : null)
            .colorMappings(this.colorMappings)
            .tileSize(this.tileSize != null ? TileConfiguration.TileSize.valueOf(this.tileSize) : null)
            .showDetailLink(this.showDetailLink)
            .showUrlLink(this.showUrlLink)
            .sortOrder(this.sortOrder != null ? TileConfiguration.SortOrder.valueOf(this.sortOrder) : null)
            .hideNavigation(this.hideNavigation)
            .hideEditButtons(this.hideEditButtons)
            .backgroundColorOpacity(this.backgroundColorOpacity)
            .build();
    }
}
