package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class RiskAssessorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(RiskAssessorPlugin.class);
    private final ChatModel chatModel;
    public RiskAssessorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "assess-risks"; }
    @Override public String getDescription() { return "Identify potential risks and concerns"; }
    @Override public String getCategory() { return "Content Analysis"; }
    @Override public String getIcon() { return "WARNING"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Analyze this document for potential risks, concerns, or issues that should be addressed.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing RiskAssessorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("riskAssessment", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Risk assessment failed", e);
            throw new PluginException("Risk assessment failed: " + e.getMessage(), e);
        }
    }
}
