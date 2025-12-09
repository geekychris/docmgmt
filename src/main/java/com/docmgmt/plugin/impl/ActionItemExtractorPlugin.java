package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ActionItemExtractorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ActionItemExtractorPlugin.class);
    private final ChatModel chatModel;
    public ActionItemExtractorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "extract-actions"; }
    @Override public String getDescription() { return "Extract action items and tasks from document"; }
    @Override public String getCategory() { return "Extraction"; }
    @Override public String getIcon() { return "TASKS"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Extract all action items, tasks, and to-dos from this document with assignees if mentioned.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing ActionItemExtractorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("actionItems", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Action item extraction failed", e);
            throw new PluginException("Action item extraction failed: " + e.getMessage(), e);
        }
    }
}
