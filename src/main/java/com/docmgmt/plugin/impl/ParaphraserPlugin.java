package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ParaphraserPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ParaphraserPlugin.class);
    private final ChatModel chatModel;
    public ParaphraserPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "paraphrase"; }
    @Override public String getDescription() { return "Rephrase content while maintaining meaning"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "REFRESH"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Paraphrase this content while maintaining the original meaning and intent.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing ParaphraserPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("original", contentToAnalyze);
            data.put("paraphrased", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Paraphrasing failed", e);
            throw new PluginException("Paraphrasing failed: " + e.getMessage(), e);
        }
    }
}
