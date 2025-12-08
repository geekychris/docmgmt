package com.docmgmt.dto;

import com.docmgmt.model.Folder;
import com.docmgmt.model.SysObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO for Folder entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FolderDTO extends BaseSysObjectDTO {
    
    private String path;
    private String description;
    private Boolean isPublic;
    private Set<String> permissions;
    private Long parentFolderId;
    private Set<Long> childFolderIds;
    private Set<Long> itemIds;
    
    /**
     * Convert from Folder entity to DTO
     * @param folder the entity
     * @return the DTO
     */
    public static FolderDTO fromEntity(Folder folder) {
        FolderDTO dto = FolderDTO.builder()
                .id(folder.getId())
                .name(folder.getName())
                .majorVersion(folder.getMajorVersion())
                .minorVersion(folder.getMinorVersion())
                .path(folder.getPath())
                .description(folder.getDescription())
                .isPublic(folder.getIsPublic())
                .createdAt(folder.getCreatedAt())
                .modifiedAt(folder.getModifiedAt())
                .build();
        
        if (folder.getParentVersion() != null) {
            dto.setParentVersionId(folder.getParentVersion().getId());
        }
        
        if (folder.getPermissions() != null && !folder.getPermissions().isEmpty()) {
            dto.setPermissions(new HashSet<>(folder.getPermissions()));
        }
        
        // Map parent folder
        if (folder.getParentFolder() != null) {
            dto.setParentFolderId(folder.getParentFolder().getId());
        }
        
        // Map child folders - safely handle lazy-loaded collections
        try {
            if (folder.getChildFolders() != null && !folder.getChildFolders().isEmpty()) {
                dto.setChildFolderIds(folder.getChildFolders().stream()
                        .map(Folder::getId)
                        .collect(Collectors.toSet()));
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Child folders not loaded - skip it
            dto.setChildFolderIds(null);
        }
        
        // Map items - safely handle lazy-loaded collections
        try {
            if (folder.getItems() != null && !folder.getItems().isEmpty()) {
                dto.setItemIds(folder.getItems().stream()
                        .map(SysObject::getId)
                        .collect(Collectors.toSet()));
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Items not loaded - skip it
            dto.setItemIds(null);
        }
        
        return dto;
    }
    
    /**
     * Convert from DTO to entity
     * Note: This creates a basic Folder entity. Parent folder and child relationships
     * must be managed separately through the service layer.
     * @return the entity
     */
    public Folder toEntity() {
        Folder folder = Folder.builder()
                .id(this.getId())
                .name(this.getName())
                .majorVersion(this.getMajorVersion())
                .minorVersion(this.getMinorVersion())
                .path(this.getPath())
                .description(this.getDescription())
                .isPublic(this.getIsPublic())
                .build();
        
        if (this.getPermissions() != null && !this.getPermissions().isEmpty()) {
            folder.setPermissions(new HashSet<>(this.getPermissions()));
        }
        
        return folder;
    }
    
    /**
     * Update an entity with values from this DTO
     * @param sysObject the entity to update
     */
    @Override
    public void updateEntity(SysObject sysObject) {
        super.updateEntity(sysObject);
        
        if (sysObject instanceof Folder) {
            Folder folder = (Folder) sysObject;
            folder.setPath(this.getPath());
            folder.setDescription(this.getDescription());
            folder.setIsPublic(this.getIsPublic());
            
            if (this.getPermissions() != null) {
                folder.setPermissions(new HashSet<>(this.getPermissions()));
            }
        }
    }
}
