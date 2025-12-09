package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ToneAdjusterPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ToneAdjusterPlugin.class);
    private final ChatModel chatModel;
    public ToneAdjusterPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "adjust-tone"; }
    @Override public String getDescription() { return "Rewrite content in different tone"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "EDIT"; }
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("targetTone")
                .label("Target Tone")
                .description("Desired tone for the content")
                .type(PluginParameter.ParameterType.SELECT)
                .required(false)
                .defaultValue("Professional")
                .options(List.of("Professional", "Casual", "Formal", "Friendly", "Technical", "Simple"))
                .build()
        );
    }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String targetTone = request.getParameter("targetTone", "Professional");
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Rewrite this content in a %s tone while maintaining the key information.\\n\\nDocument:\\n%s", targetTone.toLowerCase(), contentToAnalyze);
            logger.info("Executing ToneAdjusterPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("original", contentToAnalyze);
            data.put("adjusted", response.trim());
            data.put("tone", targetTone);
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Tone adjustment failed", e);
            throw new PluginException("Tone adjustment failed: " + e.getMessage(), e);
        }
    }
}
