#!/bin/bash

# Plugin Generator Tool for Document Management System
# This script generates a new Spring AI-powered plugin from a template
#
# Usage: ./plugin-generator.sh <PluginName> "<Description>" "<Category>" "<Icon>"
#
# Examples:
#   ./plugin-generator.sh ReadabilityScorer "Analyze document readability level" "Content Analysis" "EYE"
#   ./plugin-generator.sh NamedEntityRecognizer "Extract named entities" "Content Analysis" "USERS"
#
# Available Icons (VaadinIcon): 
#   FILE_TEXT, TAGS, QUESTION_CIRCLE, LIGHTBULB, CHART, EYE, USERS, CALENDAR, 
#   CHECK_SQUARE, GLOBE, COG, EDIT, SEARCH, MAGIC, TROPHY, etc.
#
# Available Categories:
#   Content Analysis, Classification, Translation, Extraction, Quality

set -e

# Check arguments
if [ "$#" -lt 4 ]; then
    echo "Usage: $0 <PluginName> \"<Description>\" \"<Category>\" \"<Icon>\""
    echo ""
    echo "Example:"
    echo "  $0 ReadabilityScorer \"Analyze document readability level\" \"Content Analysis\" \"EYE\""
    exit 1
fi

PLUGIN_NAME=$1
DESCRIPTION=$2
CATEGORY=$3
ICON=$4
TASK_NAME=$(echo "$PLUGIN_NAME" | sed 's/Plugin$//' | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//')

# Generate the plugin file
PLUGIN_FILE="src/main/java/com/docmgmt/plugin/impl/${PLUGIN_NAME}.java"

if [ -f "$PLUGIN_FILE" ]; then
    echo "Error: Plugin already exists: $PLUGIN_FILE"
    exit 1
fi

echo "Generating plugin: $PLUGIN_NAME"
echo "  Task name: $TASK_NAME"
echo "  Description: $DESCRIPTION"
echo "  Category: $CATEGORY"
echo "  Icon: $ICON"
echo ""

cat > "$PLUGIN_FILE" << 'TEMPLATE_EOF'
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PLUGIN_NAME_PLACEHOLDER implements DocumentPlugin, PluginMetadata {
    
    private static final Logger logger = LoggerFactory.getLogger(PLUGIN_NAME_PLACEHOLDER.class);
    private final ChatModel chatModel;
    
    public PLUGIN_NAME_PLACEHOLDER(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "TASK_NAME_PLACEHOLDER";
    }
    
    @Override
    public String getDescription() {
        return "DESCRIPTION_PLACEHOLDER";
    }
    
    @Override
    public String getCategory() {
        return "CATEGORY_PLACEHOLDER";
    }
    
    @Override
    public String getIcon() {
        return "ICON_PLACEHOLDER";
    }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            // Add parameters here if needed
            // Example:
            // PluginParameter.builder()
            //     .name("paramName")
            //     .label("Parameter Label")
            //     .description("Parameter description")
            //     .type(PluginParameter.ParameterType.TEXT)
            //     .required(false)
            //     .defaultValue("default")
            //     .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            
            // Truncate content if too long
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            // Build the prompt - customize this based on your plugin's purpose
            String prompt = String.format(
                "Analyze the following document and provide YOUR_ANALYSIS_HERE.\\n\\n" +
                "Document:\\n%s",
                contentToAnalyze
            );
            
            logger.info("Executing PLUGIN_NAME_PLACEHOLDER");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            logger.info("Analysis complete, response length: {} characters", response != null ? response.length() : 0);
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned an empty response");
            }
            
            // Build response data
            Map<String, Object> data = new HashMap<>();
            data.put("analysis", response.trim());
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build()
                
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Analysis failed", e);
            throw new PluginException("Analysis failed: " + e.getMessage(), e);
        }
    }
}
TEMPLATE_EOF

# Replace placeholders
sed -i '' "s/PLUGIN_NAME_PLACEHOLDER/$PLUGIN_NAME/g" "$PLUGIN_FILE"
sed -i '' "s/TASK_NAME_PLACEHOLDER/$TASK_NAME/g" "$PLUGIN_FILE"
sed -i '' "s/DESCRIPTION_PLACEHOLDER/$DESCRIPTION/g" "$PLUGIN_FILE"
sed -i '' "s/CATEGORY_PLACEHOLDER/$CATEGORY/g" "$PLUGIN_FILE"
sed -i '' "s/ICON_PLACEHOLDER/$ICON/g" "$PLUGIN_FILE"

echo "✓ Plugin generated: $PLUGIN_FILE"
echo ""
echo "Next steps:"
echo "  1. Edit the plugin to customize the prompt and response data"
echo "  2. Add any parameters in getParameters() if needed"
echo "  3. Compile: mvn compile -DskipTests"
echo "  4. Restart the application"
echo ""
echo "The plugin will be automatically discovered and available in the UI!"
