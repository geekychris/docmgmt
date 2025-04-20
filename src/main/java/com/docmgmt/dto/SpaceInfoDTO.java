package com.docmgmt.dto;

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
    
    private Long fileStoreId;
    private String fileStoreName;
    private long totalSpace;
    private long usableSpace;
    private long usedSpace;
    
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

