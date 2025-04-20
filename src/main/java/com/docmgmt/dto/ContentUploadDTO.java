package com.docmgmt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for content upload requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentUploadDTO {
    
    @NotNull(message = "SysObject ID is required")
    private Long sysObjectId;
    
    private Long fileStoreId;
    
    private Boolean storeInDatabase;
    
    /**
     * Determines where the content should be stored
     * @return true if content should be stored in database, false if in file store
     */
    public boolean shouldStoreInDatabase() {
        return storeInDatabase == null || storeInDatabase || fileStoreId == null;
    }
}

