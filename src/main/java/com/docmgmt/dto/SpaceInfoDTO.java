package com.docmgmt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * DTO for file store space information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceInfoDTO {
    
    @JsonProperty("id")
    private Long fileStoreId;
    
    @JsonProperty("name")
    private String fileStoreName;
    
    private long totalSpace;
    
    @JsonProperty("availableSpace")
    private long usableSpace;
    
    private long usedSpace;
    
    /**
     * Get formatted available space
     * @return human-readable string of available space
     */
    @JsonProperty("formattedAvailableSpace")
    public String getFormattedAvailableSpace() {
        return formatBytes(usableSpace);
    }
    
    /**
     * Convert bytes to a human-readable format
     * @param bytes the number of bytes
     * @return a human-readable string
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}

