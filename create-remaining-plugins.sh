#!/bin/bash

# Create all remaining plugins
cd /Users/chris/code/warp_experiments/docmgmt

PLUGIN_DIR="src/main/java/com/docmgmt/plugin/impl"

# Plugin 1: ReadabilityScorerPlugin
cat > "$PLUGIN_DIR/ReadabilityScorerPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;

import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ReadabilityScorerPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ReadabilityScorerPlugin.class);
    private final ChatModel chatModel;
    
    public ReadabilityScorerPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    
    @Override public String getTaskName() { return "readability-score"; }
    @Override public String getDescription() { return "Analyze document readability and complexity level"; }
    @Override public String getCategory() { return "Quality"; }
    @Override public String getIcon() { return "EYE"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Analyze the readability of this document. Provide: reading level, readability score (1-10), sentence complexity, vocabulary assessment, and improvement suggestions.\\n\\nDocument:\\n%s",
                contentToAnalyze
            );
            
            logger.info("Executing ReadabilityScorerPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("analysis", response.trim());
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Readability analysis failed", e);
            throw new PluginException("Readability analysis failed: " + e.getMessage(), e);
        }
    }
}
EOF

# Plugin 2: GrammarCheckerPlugin
cat > "$PLUGIN_DIR/GrammarCheckerPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;

import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GrammarCheckerPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(GrammarCheckerPlugin.class);
    private final ChatModel chatModel;
    
    public GrammarCheckerPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    
    @Override public String getTaskName() { return "grammar-check"; }
    @Override public String getDescription() { return "Check grammar, spelling, and style errors"; }
    @Override public String getCategory() { return "Quality"; }
    @Override public String getIcon() { return "SPELL_CHECK"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Review this document for grammar, spelling, punctuation, and style issues. List all errors with suggested corrections.\\n\\nDocument:\\n%s",
                contentToAnalyze
            );
            
            logger.info("Executing GrammarCheckerPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("analysis", response.trim());
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Grammar check failed", e);
            throw new PluginException("Grammar check failed: " + e.getMessage(), e);
        }
    }
}
EOF

# Plugin 3: FAQGeneratorPlugin
cat > "$PLUGIN_DIR/FAQGeneratorPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;

import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FAQGeneratorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FAQGeneratorPlugin.class);
    private final ChatModel chatModel;
    
    public FAQGeneratorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    
    @Override public String getTaskName() { return "generate-faq"; }
    @Override public String getDescription() { return "Generate FAQs from document content"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "QUESTION_CIRCLE"; }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("count")
                .label("Number of FAQs")
                .description("How many FAQ pairs to generate")
                .type(PluginParameter.ParameterType.NUMBER)
                .required(false)
                .defaultValue("5")
                .minValue(1)
                .maxValue(20)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            Integer count = request.getParameter("count", 5);
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            
            String prompt = String.format(
                "Generate %d frequently asked questions and answers based on this document.\\n\\nDocument:\\n%s",
                count, contentToAnalyze
            );
            
            logger.info("Executing FAQGeneratorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            
            if (response == null || response.trim().isEmpty()) {
                throw new PluginException("LLM returned empty response");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("faqs", response.trim());
            data.put("count", count);
            data.put("truncated", content.length() > 4000);
            
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("FAQ generation failed", e);
            throw new PluginException("FAQ generation failed: " + e.getMessage(), e);
        }
    }
}
EOF

echo "Created 3 plugins successfully!"
echo "Run the script to create all 13 plugins..."
