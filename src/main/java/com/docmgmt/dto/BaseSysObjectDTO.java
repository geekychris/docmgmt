package com.docmgmt.dto;

import com.docmgmt.model.SysObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base DTO for SysObject entities
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseSysObjectDTO {
    
    private Long id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Major version is required")
    private Integer majorVersion;
    
    @NotNull(message = "Minor version is required")
    private Integer minorVersion;
    
    private Long parentVersionId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime modifiedAt;
    
    private List<ContentDTO> contents;
    
    /**
     * Convert from SysObject entity to DTO
     * This method should be implemented by child classes to handle specific attributes
     * @param sysObject the entity to convert
     * @return the DTO
     */
    public static BaseSysObjectDTO fromEntity(SysObject sysObject) {
        BaseSysObjectDTO dto = BaseSysObjectDTO.builder()
                .id(sysObject.getId())
                .name(sysObject.getName())
                .majorVersion(sysObject.getMajorVersion())
                .minorVersion(sysObject.getMinorVersion())
                .createdAt(sysObject.getCreatedAt())
                .modifiedAt(sysObject.getModifiedAt())
                .build();
        
        if (sysObject.getParentVersion() != null) {
            dto.setParentVersionId(sysObject.getParentVersion().getId());
        }
        
        return dto;
    }
    
    /**
     * Update an entity with values from this DTO
     * This method should be extended by child classes to handle specific attributes
     * @param sysObject the entity to update
     */
    public void updateEntity(SysObject sysObject) {
        sysObject.setName(this.name);
        // Don't update version information or parent through normal update
    }
    
    /**
     * Get the version string representation
     * @return formatted version string like "1.0"
     */
    public String getVersionString() {
        return majorVersion + "." + minorVersion;
    }
}

