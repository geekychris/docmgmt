#!/bin/bash
cd /Users/chris/code/warp_experiments/docmgmt
PLUGIN_DIR="src/main/java/com/docmgmt/plugin/impl"

# Already created: Readability, Grammar, FAQ

# 4. OutlineGeneratorPlugin
cat > "$PLUGIN_DIR/OutlineGeneratorPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class OutlineGeneratorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(OutlineGeneratorPlugin.class);
    private final ChatModel chatModel;
    public OutlineGeneratorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "generate-outline"; }
    @Override public String getDescription() { return "Create structured outline from document"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "LIST"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Create a structured outline with main topics and subtopics for this document.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing OutlineGeneratorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("outline", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Outline generation failed", e);
            throw new PluginException("Outline generation failed: " + e.getMessage(), e);
        }
    }
}
EOF

# 5. Action ItemExtractorPlugin
cat > "$PLUGIN_DIR/ActionItemExtractorPlugin.java" << 'EOF'
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
EOF

# 6. MeetingNotesGeneratorPlugin
cat > "$PLUGIN_DIR/MeetingNotesGeneratorPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class MeetingNotesGeneratorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(MeetingNotesGeneratorPlugin.class);
    private final ChatModel chatModel;
    public MeetingNotesGeneratorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "format-meeting-notes"; }
    @Override public String getDescription() { return "Format as structured meeting notes"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "CALENDAR"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Format this as professional meeting notes with: attendees, agenda, discussion points, decisions, and action items.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing MeetingNotesGeneratorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("meetingNotes", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Meeting notes generation failed", e);
            throw new PluginException("Meeting notes generation failed: " + e.getMessage(), e);
        }
    }
}
EOF

# 7. RiskAssessorPlugin
cat > "$PLUGIN_DIR/RiskAssessorPlugin.java" << 'EOF'
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
EOF

# 8. CitationGeneratorPlugin
cat > "$PLUGIN_DIR/CitationGeneratorPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class CitationGeneratorPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(CitationGeneratorPlugin.class);
    private final ChatModel chatModel;
    public CitationGeneratorPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "generate-citation"; }
    @Override public String getDescription() { return "Generate citation in various formats"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "QUOTE_LEFT"; }
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("format")
                .label("Citation Format")
                .description("Format style for citation")
                .type(PluginParameter.ParameterType.SELECT)
                .required(false)
                .defaultValue("APA")
                .options(List.of("APA", "MLA", "Chicago", "Harvard"))
                .build()
        );
    }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String format = request.getParameter("format", "APA");
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Generate a %s format citation for this document. Extract title, author, date if available.\\n\\nDocument:\\n%s", format, contentToAnalyze);
            logger.info("Executing CitationGeneratorPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("citation", response.trim());
            data.put("format", format);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Citation generation failed", e);
            throw new PluginException("Citation generation failed: " + e.getMessage(), e);
        }
    }
}
EOF

# 9. ParaphraserPlugin
cat > "$PLUGIN_DIR/ParaphraserPlugin.java" << 'EOF'
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
EOF

# 10. ToneAdjusterPlugin
cat > "$PLUGIN_DIR/ToneAdjusterPlugin.java" << 'EOF'
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
EOF

# 11. FactCheckerPlugin
cat > "$PLUGIN_DIR/FactCheckerPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class FactCheckerPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FactCheckerPlugin.class);
    private final ChatModel chatModel;
    public FactCheckerPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "fact-check"; }
    @Override public String getDescription() { return "Identify claims that need verification"; }
    @Override public String getCategory() { return "Quality"; }
    @Override public String getIcon() { return "CHECK_CIRCLE"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Identify factual claims in this document that should be verified. Note which seem accurate vs questionable.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing FactCheckerPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("analysis", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Fact checking failed", e);
            throw new PluginException("Fact checking failed: " + e.getMessage(), e);
        }
    }
}
EOF

# 12. ContentExpanderPlugin
cat > "$PLUGIN_DIR/ContentExpanderPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ContentExpanderPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ContentExpanderPlugin.class);
    private final ChatModel chatModel;
    public ContentExpanderPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "expand-content"; }
    @Override public String getDescription() { return "Expand and elaborate on content"; }
    @Override public String getCategory() { return "Content Generation"; }
    @Override public String getIcon() { return "EXPAND_SQUARE"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Expand on this content with more details, examples, and explanations.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing ContentExpanderPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("original", contentToAnalyze);
            data.put("expanded", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Content expansion failed", e);
            throw new PluginException("Content expansion failed: " + e.getMessage(), e);
        }
    }
}
EOF

# 13. ComparisonAnalyzerPlugin
cat > "$PLUGIN_DIR/ComparisonAnalyzerPlugin.java" << 'EOF'
package com.docmgmt.plugin.impl;
import com.docmgmt.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ComparisonAnalyzerPlugin implements DocumentPlugin, PluginMetadata {
    private static final Logger logger = LoggerFactory.getLogger(ComparisonAnalyzerPlugin.class);
    private final ChatModel chatModel;
    public ComparisonAnalyzerPlugin(ChatModel chatModel) { this.chatModel = chatModel; }
    @Override public String getTaskName() { return "analyze-comparison"; }
    @Override public String getDescription() { return "Analyze comparative content and differences"; }
    @Override public String getCategory() { return "Content Analysis"; }
    @Override public String getIcon() { return "SPLIT"; }
    @Override public List<PluginParameter> getParameters() { return List.of(); }
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            String contentToAnalyze = content.length() > 4000 ? content.substring(0, 4000) : content;
            String prompt = String.format("Identify and analyze any comparisons, alternatives, or options discussed in this document.\\n\\nDocument:\\n%s", contentToAnalyze);
            logger.info("Executing ComparisonAnalyzerPlugin");
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
            if (response == null || response.trim().isEmpty()) throw new PluginException("LLM returned empty response");
            Map<String, Object> data = new HashMap<>();
            data.put("analysis", response.trim());
            data.put("truncated", content.length() > 4000);
            return PluginResponse.builder().status(PluginResponse.PluginStatus.SUCCESS).data(data).build();
        } catch (Exception e) {
            logger.error("Comparison analysis failed", e);
            throw new PluginException("Comparison analysis failed: " + e.getMessage(), e);
        }
    }
}
EOF

echo "✅ Created 13 additional plugins successfully!"
echo "Total plugins: 23 (10 existing + 13 new)"
echo ""
echo "New plugins created:"
echo "  1. ReadabilityScorerPlugin"
echo "  2. GrammarCheckerPlugin"
echo "  3. FAQGeneratorPlugin"
echo "  4. OutlineGeneratorPlugin"
echo "  5. ActionItemExtractorPlugin"
echo "  6. MeetingNotesGeneratorPlugin"
echo "  7. RiskAssessorPlugin"
echo "  8. CitationGeneratorPlugin"
echo "  9. ParaphraserPlugin"
echo "  10. ToneAdjusterPlugin"
echo "  11. FactCheckerPlugin"
echo "  12. ContentExpanderPlugin"
echo "  13. ComparisonAnalyzerPlugin"
echo ""
echo "Now run: mvn compile -DskipTests"
