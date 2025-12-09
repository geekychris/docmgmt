package com.docmgmt.dto;

import com.docmgmt.plugin.PluginParameter;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for plugin information exposed via REST API
 */
@Data
@Builder
public class PluginInfoDTO {
    private String taskName;
    private String description;
    private String category;
    private String icon;
    private List<PluginParameter> parameters;
}
