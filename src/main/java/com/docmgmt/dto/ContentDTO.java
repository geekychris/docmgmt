package com.docmgmt.dto;

import com.docmgmt.model.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * DTO for Content metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDTO {
    
    private Long id;
    private String name;
    private String contentType;
    private Long sysObjectId;
    private Long fileStoreId;
    private String fileStoreName;
    private String storagePath;
    private Long size;
    private String formattedSize;
    private String storageType;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    
    // Rendition fields
    private boolean isPrimary;
    private boolean isIndexable;
    private Long parentRenditionId;
    private java.util.List<ContentDTO> secondaryRenditions;
    
    /**
     * Convert from entity to DTO
     * @param content the entity
     * @return the DTO
     */
    public static ContentDTO fromEntity(Content content) {
        return fromEntity(content, false);
    }
    
    /**
     * Convert from entity to DTO
     * @param content the entity
     * @param includeSecondaryRenditions whether to include secondary renditions recursively
     * @return the DTO
     */
    public static ContentDTO fromEntity(Content content, boolean includeSecondaryRenditions) {
        try {
            ContentDTO dto = ContentDTO.builder()
                    .id(content.getId())
                    .name(content.getName())
                    .contentType(content.getContentType())
                    .sysObjectId(content.getSysObject().getId())
                    .createdAt(content.getCreatedAt())
                    .modifiedAt(content.getModifiedAt())
                    .size(content.getSize())
                    .formattedSize(SpaceInfoDTO.formatBytes(content.getSize()))
                    .isPrimary(content.isPrimary())
                    .isIndexable(content.isIndexable())
                    .build();
            
            if (content.isStoredInDatabase()) {
                dto.setStorageType("DATABASE");
            } else if (content.isStoredInFileStore()) {
                dto.setStorageType("FILE_STORE");
                dto.setFileStoreId(content.getFileStore().getId());
                dto.setFileStoreName(content.getFileStore().getName());
                dto.setStoragePath(content.getStoragePath());
            }
            
            // Add parent rendition ID if this is a secondary rendition
            if (content.getParentRendition() != null) {
                dto.setParentRenditionId(content.getParentRendition().getId());
            }
            
            // Add secondary renditions if requested
            if (includeSecondaryRenditions && content.isPrimary() && 
                content.getSecondaryRenditions() != null && !content.getSecondaryRenditions().isEmpty()) {
                dto.setSecondaryRenditions(
                    content.getSecondaryRenditions().stream()
                        .map(sec -> ContentDTO.fromEntity(sec, false))
                        .collect(java.util.stream.Collectors.toList())
                );
            }
            
            return dto;
        } catch (IOException e) {
            // Handle error getting content size
            throw new RuntimeException("Error getting content size", e);
        }
    }
}

