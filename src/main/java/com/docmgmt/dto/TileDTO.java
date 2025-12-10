package com.docmgmt.dto;

import com.docmgmt.model.Document;
import com.docmgmt.model.SysObject;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DTO representing a document as a tile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TileDTO {
    
    private Long id;
    private String name;
    private String description;
    private String url;
    private String detailUrl;
    private String documentType;
    private Set<String> tags;
    private String color;
    private String groupName;
    
    /**
     * Dynamic fields based on configuration
     */
    @Builder.Default
    private Map<String, Object> customFields = new HashMap<>();
    
    /**
     * Create TileDTO from SysObject
     */
    public static TileDTO fromSysObject(SysObject sysObject, String color, String groupName) {
        // Use the document's own color if set, otherwise use the provided color from strategy
        String effectiveColor = (sysObject.getColor() != null && !sysObject.getColor().isEmpty()) 
            ? sysObject.getColor() 
            : color;
        
        TileDTOBuilder builder = TileDTO.builder()
            .id(sysObject.getId())
            .name(sysObject.getName())
            .url(sysObject.getUrl())
            .detailUrl("/document-detail/" + sysObject.getId())
            .color(effectiveColor)
            .groupName(groupName);
        
        // Add document-specific fields if it's a document
        if (sysObject instanceof Document) {
            Document doc = (Document) sysObject;
            builder.description(doc.getDescription())
                   .documentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : null)
                   .tags(doc.getTags());
        }
        
        return builder.build();
    }
}
