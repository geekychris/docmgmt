package com.docmgmt.dto;

import com.docmgmt.model.FileStore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for FileStore entity
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileStoreDTO {
    
    private Long id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Root path is required")
    private String rootPath;
    
    private FileStore.Status status;
    
    private Long contentCount;
    
    /**
     * Convert from entity to DTO
     * @param fileStore the entity
     * @return the DTO
     */
    public static FileStoreDTO fromEntity(FileStore fileStore) {
        FileStoreDTO dto = FileStoreDTO.builder()
                .id(fileStore.getId())
                .name(fileStore.getName())
                .rootPath(fileStore.getRootPath())
                .status(fileStore.getStatus())
                .build();
        
        // Initialize content count if the contents collection is loaded
        if (fileStore.getContents() != null) {
            dto.setContentCount((long) fileStore.getContents().size());
        }
        
        return dto;
    }
    
    /**
     * Convert from DTO to entity
     * @return the entity
     */
    public FileStore toEntity() {
        return FileStore.builder()
                .id(this.id)
                .name(this.name)
                .rootPath(this.rootPath)
                .status(this.status != null ? this.status : FileStore.Status.ACTIVE)
                .build();
    }
}

