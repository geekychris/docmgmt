# Complete Plugin Catalog (23 Plugins)

## Overview
The document management system now includes **23 AI-powered plugins** for comprehensive document analysis and generation.

---

## Plugin Categories

### 📊 **Content Analysis** (7 plugins)
1. **Summarizer** - Generate executive summaries
2. **Keyword Extractor** - Extract keywords and tags
3. **Entity Extractor** - Extract people, organizations, locations
4. **Question Answerer** - Answer specific questions about documents
5. **Sentiment Analyzer** - Analyze tone and sentiment
6. **Topic Modeler** - Identify main themes
7. **Risk Assessor** - Identify potential risks and concerns

### 🏷️ **Classification** (3 plugins)
8. **Classifier** - Auto-classify document types
9. **Duplicate Detector** - Find similar documents
10. **Comparison Analyzer** - Analyze comparative content

### 🌐 **Translation** (1 plugin)
11. **Translator** - Detect language and translate content

### 📝 **Content Generation** (8 plugins)
12. **FAQ Generator** - Generate frequently asked questions
13. **Outline Generator** - Create structured outlines
14. **Meeting Notes Generator** - Format as professional meeting notes
15. **Citation Generator** - Generate citations (APA, MLA, Chicago, Harvard)
16. **Paraphraser** - Rephrase content while maintaining meaning
17. **Tone Adjuster** - Rewrite content in different tones
18. **Content Expander** - Elaborate and add details

### 🔍 **Extraction** (2 plugins)
19. **Action Item Extractor** - Extract tasks and to-dos
20. **Compliance Checker** - Verify requirements

### ✅ **Quality** (3 plugins)
21. **Readability Scorer** - Analyze readability and complexity
22. **Grammar Checker** - Check grammar, spelling, style
23. **Fact Checker** - Identify claims that need verification

---

## Plugin Details

### 1. Summarizer
- **Task**: `summarize`
- **Category**: Content Analysis
- **Icon**: FILE_TEXT
- **Parameters**: `length` (brief/standard/detailed)
- **Returns**: Summary text

### 2. Keyword Extractor
- **Task**: `extract-keywords`
- **Category**: Content Analysis
- **Icon**: TAGS
- **Parameters**: `maxKeywords` (1-50, default: 10)
- **Returns**: List of keywords

### 3. Entity Extractor
- **Task**: `extract-entities`
- **Category**: Content Analysis
- **Icon**: USERS
- **Parameters**: None
- **Returns**: People, organizations, locations, dates, amounts

### 4. Question Answerer
- **Task**: `answer-question`
- **Category**: Content Analysis
- **Icon**: QUESTION_CIRCLE
- **Parameters**: `question` (required), `targetLanguage` (12 options)
- **Returns**: Answer in specified language

### 5. Sentiment Analyzer
- **Task**: `analyze-sentiment`
- **Category**: Content Analysis
- **Icon**: HEART
- **Parameters**: None
- **Returns**: Sentiment analysis

### 6. Topic Modeler
- **Task**: `model-topics`
- **Category**: Content Analysis
- **Icon**: CHART
- **Parameters**: `maxTopics` (1-10, default: 5)
- **Returns**: Identified themes

### 7. Risk Assessor
- **Task**: `assess-risks`
- **Category**: Content Analysis
- **Icon**: WARNING
- **Parameters**: None
- **Returns**: Risk assessment

### 8. Classifier
- **Task**: `classify`
- **Category**: Classification
- **Icon**: TAGS
- **Parameters**: None
- **Returns**: Document classification

### 9. Duplicate Detector
- **Task**: `detect-duplicates`
- **Category**: Classification
- **Icon**: COPY
- **Parameters**: `maxResults` (1-20, default: 5)
- **Returns**: Similar documents

### 10. Comparison Analyzer
- **Task**: `analyze-comparison`
- **Category**: Content Analysis
- **Icon**: SPLIT
- **Parameters**: None
- **Returns**: Comparison analysis

### 11. Translator
- **Task**: `translate`
- **Category**: Translation
- **Icon**: GLOBE
- **Parameters**: `targetLanguage` (12 options)
- **Returns**: Original and translated content with language codes

### 12. FAQ Generator
- **Task**: `generate-faq`
- **Category**: Content Generation
- **Icon**: QUESTION_CIRCLE
- **Parameters**: `count` (1-20, default: 5)
- **Returns**: FAQ pairs

### 13. Outline Generator
- **Task**: `generate-outline`
- **Category**: Content Generation
- **Icon**: LIST
- **Parameters**: None
- **Returns**: Structured outline

### 14. Meeting Notes Generator
- **Task**: `format-meeting-notes`
- **Category**: Content Generation
- **Icon**: CALENDAR
- **Parameters**: None
- **Returns**: Structured meeting notes

### 15. Citation Generator
- **Task**: `generate-citation`
- **Category**: Content Generation
- **Icon**: QUOTE_LEFT
- **Parameters**: `format` (APA/MLA/Chicago/Harvard)
- **Returns**: Formatted citation

### 16. Paraphraser
- **Task**: `paraphrase`
- **Category**: Content Generation
- **Icon**: REFRESH
- **Parameters**: None
- **Returns**: Original and paraphrased versions

### 17. Tone Adjuster
- **Task**: `adjust-tone`
- **Category**: Content Generation
- **Icon**: EDIT
- **Parameters**: `targetTone` (Professional/Casual/Formal/Friendly/Technical/Simple)
- **Returns**: Original and adjusted versions

### 18. Content Expander
- **Task**: `expand-content`
- **Category**: Content Generation
- **Icon**: EXPAND_SQUARE
- **Parameters**: None
- **Returns**: Original and expanded versions

### 19. Action Item Extractor
- **Task**: `extract-actions`
- **Category**: Extraction
- **Icon**: TASKS
- **Parameters**: None
- **Returns**: Action items with assignees

### 20. Compliance Checker
- **Task**: `check-compliance`
- **Category**: Extraction
- **Icon**: CHECK_SQUARE
- **Parameters**: `requirements` (required)
- **Returns**: Compliance verification

### 21. Readability Scorer
- **Task**: `readability-score`
- **Category**: Quality
- **Icon**: EYE
- **Parameters**: None
- **Returns**: Reading level, score, complexity assessment

### 22. Grammar Checker
- **Task**: `grammar-check`
- **Category**: Quality
- **Icon**: PENCIL
- **Parameters**: None
- **Returns**: Grammar, spelling, punctuation errors

### 23. Fact Checker
- **Task**: `fact-check`
- **Category**: Quality
- **Icon**: CHECK_CIRCLE
- **Parameters**: None
- **Returns**: Claims requiring verification

---

## Using the Plugin Generator Tool

### Quick Start

```bash
cd /path/to/docmgmt
./plugin-generator.sh <PluginName> "<Description>" "<Category>" "<Icon>"
```

### Examples

```bash
# Create a sentiment analysis plugin
./plugin-generator.sh SentimentAnalyzer "Analyze emotional tone" "Content Analysis" "HEART"

# Create a privacy checker
./plugin-generator.sh PrivacyChecker "Check for PII and sensitive data" "Quality" "LOCK"

# Create a formatting plugin
./plugin-generator.sh CodeFormatter "Format code snippets" "Content Generation" "CODE"
```

### Available Icons

Common VaadinIcon values:
- FILE_TEXT, TAGS, QUESTION_CIRCLE, LIGHTBULB, CHART, EYE
- USERS, CALENDAR, CHECK_SQUARE, GLOBE, COG, EDIT, SEARCH
- MAGIC, TROPHY, WARNING, HEART, COPY, SPLIT, LIST, TASKS
- QUOTE_LEFT, REFRESH, EXPAND_SQUARE, PENCIL, CHECK_CIRCLE
- LOCK, CODE, DATABASE, ENVELOPE, PHONE, CLOCK, etc.

**Note**: If you get an "No enum constant" error, the icon name is invalid. 
Common valid icons: FILE, FILE_TEXT, FOLDER, EDIT, PENCIL, TRASH, SEARCH, 
PLUS, MINUS, CHECK, CLOSE, ARROW_RIGHT, ARROW_LEFT, COG, USER, USERS, etc.

Full list: https://vaadin.com/docs/latest/components/icons

### Categories

- **Content Analysis** - For analysis and insights
- **Classification** - For categorization and matching
- **Translation** - For language operations
- **Content Generation** - For creating new content
- **Extraction** - For pulling specific information
- **Quality** - For checking and validation

### Plugin Template Structure

Generated plugins include:
1. **Imports** - All necessary Spring AI and plugin interfaces
2. **Metadata** - Task name, description, category, icon
3. **Parameters** - Optional parameter definitions with types
4. **Execute method** - LLM prompt and response handling
5. **Error handling** - Proper exception management
6. **Logging** - SLF4J logger for tracking

### After Generation

1. **Edit the prompt** in the `execute()` method to customize behavior
2. **Add parameters** in `getParameters()` if needed:
   - TEXT - Single-line text input
   - TEXTAREA - Multi-line text input
   - NUMBER - Integer with min/max
   - SELECT - Dropdown with options
   - BOOLEAN - Checkbox
   - DOCUMENT_ID - Reference to another document

3. **Customize response data** in the `execute()` method
4. **Compile**: `mvn compile -DskipTests`
5. **Restart** the application
6. Plugin appears automatically in the UI!

### Advanced: Adding Complex Parameters

```java
@Override
public List<PluginParameter> getParameters() {
    return List.of(
        // Text input
        PluginParameter.builder()
            .name("title")
            .label("Report Title")
            .type(PluginParameter.ParameterType.TEXT)
            .required(true)
            .build(),
        
        // Number with range
        PluginParameter.builder()
            .name("maxItems")
            .label("Maximum Items")
            .type(PluginParameter.ParameterType.NUMBER)
            .defaultValue("10")
            .minValue(1)
            .maxValue(100)
            .build(),
        
        // Dropdown selection
        PluginParameter.builder()
            .name("format")
            .label("Output Format")
            .type(PluginParameter.ParameterType.SELECT)
            .defaultValue("JSON")
            .options(List.of("JSON", "XML", "CSV"))
            .build()
    );
}
```

### Accessing Parameters in Execute Method

```java
// Get parameter with default
String title = request.getParameter("title", "Untitled");
Integer maxItems = request.getParameter("maxItems", 10);
String format = request.getParameter("format", "JSON");

// Use in prompt
String prompt = String.format(
    "Generate a %s report titled '%s' with up to %d items...",
    format, title, maxItems
);
```

---

## REST API Usage

Execute any plugin via REST:

```bash
# Execute with parameters
curl -X POST "http://localhost:8082/api/documents/1/plugins/summarize?saveAsMarkdown=true" \
  -H "Content-Type: application/json" \
  -d '{"length": "detailed"}'

# Execute without parameters
curl -X POST "http://localhost:8082/api/documents/1/plugins/grammar-check" \
  -H "Content-Type: application/json"

# Get plugin list with metadata
curl http://localhost:8082/api/documents/1/plugins/detailed

# Get specific plugin info
curl http://localhost:8082/api/documents/1/plugins/info/readability-score
```

---

## Tips & Best Practices

1. **Keep prompts clear and specific** - Better prompts = better results
2. **Use truncation** - Content is limited to 4000 characters for performance
3. **Add logging** - Use INFO level for execution tracking
4. **Handle empty responses** - Always check for null/empty LLM output
5. **Provide good defaults** - Make parameters optional when possible
6. **Test with various content** - Different document types may need different prompts
7. **Use markdown for output** - Leverage the markdown viewer for rich results
8. **Save important results** - Use `saveAsMarkdown=true` to preserve outputs

---

## Troubleshooting

### Plugin not appearing in UI
- Check compilation: `mvn compile -DskipTests`
- Verify `@Component` annotation exists
- Check logs for Spring Boot errors
- Restart application

### Empty or poor results
- Adjust prompt for clarity
- Check content truncation (4000 char limit)
- Test with different document types
- Review LLM logs for actual input/output

### Parameter not working
- Verify parameter name spelling
- Check parameter type matches usage
- Ensure default values are provided
- Test via REST API first

---

## Future Enhancements

Potential additions:
- **Image analyzer** - Extract text and analyze images
- **Audio transcriber** - Convert audio to text
- **Chart generator** - Create visualizations from data
- **Template filler** - Fill document templates with extracted data
- **Cross-document analyzer** - Compare multiple documents
- **Version differ** - Show changes between versions
- **Privacy scanner** - Detect PII and sensitive data
- **SEO optimizer** - Optimize content for search engines

---

**Build Status**: ✅ All 23 plugins compiled successfully!
**Auto-discovery**: ✅ All plugins automatically available in UI
**REST API**: ✅ All plugins accessible via API
**Markdown export**: ✅ All results can be saved as markdown
