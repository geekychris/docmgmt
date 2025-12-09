package com.docmgmt.plugin.impl;

import com.docmgmt.plugin.DocumentPlugin;
import com.docmgmt.plugin.PluginException;
import com.docmgmt.plugin.PluginMetadata;
import com.docmgmt.plugin.PluginParameter;
import com.docmgmt.plugin.PluginRequest;
import com.docmgmt.plugin.PluginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;

@Component
public class ComplianceCheckerPlugin implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(ComplianceCheckerPlugin.class);
    private final ChatModel chatModel;
    
    public ComplianceCheckerPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "check-compliance";
    }
    
    @Override
    public String getDescription() {
        return "Verify document meets specific requirements, check for required sections and information";
    }
    
    @Override
    public String getCategory() {
        return "Classification";
    }
    
    @Override
    public String getIcon() {
        return "CHECK_SQUARE";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("requirements")
                .label("Requirements")
                .description("List the requirements this document must meet")
                .type(PluginParameter.ParameterType.TEXTAREA)
                .required(true)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String requirements = request.getParameter("requirements", (String) null);
            
            if (requirements == null || requirements.trim().isEmpty()) {
                throw new PluginException("Requirements parameter is required");
            }
            
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Check if the following document meets these requirements:\\n%s\\n\\n" +
                "Provide your analysis in this format:\\n" +
                "COMPLIANT:\\n[yes/no/partial]\\n\\n" +
                "MISSING_REQUIREMENTS:\\n- [list any missing requirements]\\n\\n" +
                "PRESENT_REQUIREMENTS:\\n- [list requirements that are met]\\n\\n" +
                "ISSUES:\\n- [list any compliance issues or concerns]\\n\\n" +
                "RECOMMENDATIONS:\\n- [suggestions for improvement]\\n\\n" +
                "Document:\\n%s",
                requirements,
                contentToAnalyze
            );
            
            logger.debug("Checking compliance");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            // Parse response
            Map<String, Object> parsed = parseComplianceResponse(response);
            
            Map<String, Object> data = new HashMap<>();
            data.put("compliant", parsed.get("COMPLIANT"));
            data.put("missingRequirements", parsed.get("MISSING_REQUIREMENTS"));
            data.put("presentRequirements", parsed.get("PRESENT_REQUIREMENTS"));
            data.put("issues", parsed.get("ISSUES"));
            data.put("recommendations", parsed.get("RECOMMENDATIONS"));
            data.put("requirements", requirements);
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Compliance check failed", e);
            throw new PluginException("Compliance check failed: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> parseComplianceResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        String currentSection = null;
        List<String> currentList = new ArrayList<>();
        String currentValue = null;
        
        for (String line : response.split("\\n")) {
            String trimmed = line.trim();
            
            if (trimmed.matches("^[A-Z_]+:$")) {
                // Save previous section
                if (currentSection != null) {
                    if (currentSection.equals("COMPLIANT")) {
                        result.put(currentSection, currentValue != null ? currentValue : "unknown");
                    } else {
                        result.put(currentSection, new ArrayList<>(currentList));
                    }
                }
                
                currentSection = trimmed.replace(":", "");
                currentList = new ArrayList<>();
                currentValue = null;
            } else if (currentSection != null && !trimmed.isEmpty()) {
                if (currentSection.equals("COMPLIANT")) {
                    currentValue = trimmed.toLowerCase();
                } else if (trimmed.startsWith("-")) {
                    String item = trimmed.substring(1).trim();
                    if (!item.equalsIgnoreCase("none") && !item.isEmpty()) {
                        currentList.add(item);
                    }
                } else {
                    currentList.add(trimmed);
                }
            }
        }
        
        // Save last section
        if (currentSection != null) {
            if (currentSection.equals("COMPLIANT")) {
                result.put(currentSection, currentValue != null ? currentValue : "unknown");
            } else {
                result.put(currentSection, currentList);
            }
        }
        
        // Set defaults for missing sections
        result.putIfAbsent("COMPLIANT", "unknown");
        result.putIfAbsent("MISSING_REQUIREMENTS", new ArrayList<>());
        result.putIfAbsent("PRESENT_REQUIREMENTS", new ArrayList<>());
        result.putIfAbsent("ISSUES", new ArrayList<>());
        result.putIfAbsent("RECOMMENDATIONS", new ArrayList<>());
        
        return result;
    }
}
