package com.docmgmt.dto;
import com.docmgmt.model.SysObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for SysObject version information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysObjectVersionDTO {
    
    private Long id;
    private String name;
    private String version;
    private Integer majorVersion;
    private Integer minorVersion;
    private Long parentVersionId;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Boolean isLatestVersion;
    private Boolean hasChildVersions;
    
    /**
     * Convert entity to DTO for version information
     * @param sysObject the entity
     * @param isLatestVersion flag indicating if this is the latest version
     * @param hasChildVersions flag indicating if this object has child versions
     * @return the version DTO
     */
    public static SysObjectVersionDTO fromEntity(SysObject sysObject, Boolean isLatestVersion, Boolean hasChildVersions) {
        return SysObjectVersionDTO.builder()
                .id(sysObject.getId())
                .name(sysObject.getName())
                .version(sysObject.getMajorVersion() + "." + sysObject.getMinorVersion())
                .majorVersion(sysObject.getMajorVersion())
                .minorVersion(sysObject.getMinorVersion())
                .parentVersionId(sysObject.getParentVersion() != null ? sysObject.getParentVersion().getId() : null)
                .createdAt(sysObject.getCreatedAt())
                .modifiedAt(sysObject.getModifiedAt())
                .isLatestVersion(isLatestVersion)
                .hasChildVersions(hasChildVersions)
                .build();
    }
    
    /**
     * Convert a list of SysObject entities to version DTOs
     * @param sysObjects the list of entities
     * @return list of version DTOs
     */
    public static List<SysObjectVersionDTO> fromEntities(List<SysObject> sysObjects) {
        if (sysObjects == null || sysObjects.isEmpty()) {
            return new ArrayList<>();
        }
        
        return sysObjects.stream()
                .map(obj -> fromEntity(obj, false, false))
                .collect(Collectors.toList());
    }
}

