# Plugin System - Complete API & UI Guide

## ✅ System Status

**All components implemented and compiled successfully!**

- ✅ 10+ Plugins implemented
- ✅ Plugin metadata system
- ✅ Dynamic UI generation
- ✅ REST API with full metadata support
- ✅ Categorized plugin menu
- ✅ Dynamic parameter input forms
- ✅ Flexible result dialogs

---

## REST API Reference

### 1. Get Available Plugins (Simple)
```
GET /api/documents/{documentId}/plugins
```

**Response:**
```json
{
  "translate": "Detects the language of document content and translates it to a target language",
  "summarize": "Generate executive summary with key points and action items",
  "extract-keywords": "Extract relevant keywords and tags for searchability",
  ...
}
```

### 2. Get Detailed Plugin Information
```
GET /api/documents/{documentId}/plugins/detailed
```

**Response:**
```json
[
  {
    "taskName": "translate",
    "description": "Detects the language of document content and translates it to a target language",
    "category": "Content Analysis",
    "icon": "GLOBE",
    "parameters": [
      {
        "name": "targetLanguage",
        "label": "Target Language",
        "description": "Language to translate to",
        "type": "SELECT",
        "required": false,
        "defaultValue": "English",
        "options": ["English", "Spanish", "French", ...]
      }
    ]
  },
  ...
]
```

### 3. Get Specific Plugin Info
```
GET /api/documents/{documentId}/plugins/info/{taskName}
```

**Example:**
```bash
curl http://localhost:8080/api/documents/1/plugins/info/translate
```

### 4. Execute Plugin
```
POST /api/documents/{documentId}/plugins/{taskName}
Content-Type: application/json

{
  "parameter1": "value1",
  "parameter2": "value2"
}
```

**Examples:**

#### Translate Document
```bash
curl -X POST http://localhost:8080/api/documents/1/plugins/translate \
  -H "Content-Type: application/json" \
  -d '{"targetLanguage": "Spanish"}'
```

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "originalLanguage": "en",
    "originalLanguageName": "English",
    "targetLanguage": "es",
    "targetLanguageName": "Spanish",
    "originalContent": "Hello world",
    "translatedContent": "Hola mundo",
    "truncated": false
  },
  "error": null
}
```

#### Summarize Document
```bash
curl -X POST http://localhost:8080/api/documents/1/plugins/summarize \
  -H "Content-Type: application/json" \
  -d '{"length": "brief"}'
```

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "summary": "This document discusses...",
    "keyPoints": "- Point 1\n- Point 2\n- Point 3",
    "actionItems": "- Action 1\n- Action 2",
    "length": "brief",
    "truncated": false
  }
}
```

#### Extract Keywords
```bash
curl -X POST http://localhost:8080/api/documents/1/plugins/extract-keywords \
  -H "Content-Type: application/json" \
  -d '{"maxKeywords": 10}'
```

#### Ask Question
```bash
curl -X POST http://localhost:8080/api/documents/1/plugins/answer-question \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the main topic of this document?"}'
```

#### Classify Document
```bash
curl -X POST http://localhost:8080/api/documents/1/plugins/classify \
  -H "Content-Type: application/json" \
  -d '{}'
```

#### Check Compliance
```bash
curl -X POST http://localhost:8080/api/documents/1/plugins/check-compliance \
  -H "Content-Type: application/json" \
  -d '{
    "requirements": "Document must include: executive summary, risk assessment, budget breakdown"
  }'
```

---

## UI Usage Guide

### Accessing Plugins

1. **Navigate to Folders view** (`/folders`)
2. **Select a folder** from the folder tree
3. **Double-click a document** to open detail dialog
4. **Click "AI Plugins" menu** in the content toolbar

### Plugin Menu Structure

The AI Plugins menu is organized by category:

```
AI Plugins
├── Content Analysis
│   ├── 🌐 Translate
│   ├── 📊 Sentiment Analysis
│   ├── 🏷️ Topic Modeling
│   └── ...
├── Classification
│   ├── 📋 Classify Document
│   ├── 🔍 Find Duplicates  
│   └── ✅ Check Compliance
└── ...
```

### Plugin Execution Flow

1. **Select Plugin** from categorized menu
2. **Plugin Dialog Opens** with dynamic form
   - Parameters adapt based on plugin requirements
   - Dropdowns, text fields, numbers, checkboxes
   - Default values pre-populated
3. **Fill Parameters** (if required)
4. **Click "Execute"**
5. **Loading Indicator** shows progress
6. **Results Dialog** displays formatted output
   - Structured sections
   - Text areas for long content
   - Lists for multiple items
   - Key-value pairs for metadata

### Parameter Types

The system supports various parameter types:

#### TEXT
Single-line text input
```
Field: [                    ]
```

#### TEXTAREA
Multi-line text input (150px height)
```
Field: [                    ]
       [                    ]
       [                    ]
```

#### NUMBER
Integer input with min/max validation
```
Count: [  5  ] (1-100)
```

#### SELECT
Dropdown with predefined options
```
Language: [English         ▼]
```

#### BOOLEAN
Checkbox with description
```
☐ Include details
  Show additional information
```

### Result Dialog

Results are displayed in an organized, scrollable dialog:

```
┌─────────────────────────────────────┐
│ Results: Translate Document         │
├─────────────────────────────────────┤
│                                     │
│ ╔═══ Original Language ═══╗        │
│ ║ en                        ║        │
│ ╚═══════════════════════════╝        │
│                                     │
│ ╔═══ Target Language ═══╗          │
│ ║ es                      ║          │
│ ╚═══════════════════════════╝        │
│                                     │
│ ╔═══ Translated Content ═══╗       │
│ ║ [text area with content] ║       │
│ ║                          ║       │
│ ╚═══════════════════════════╝       │
│                                     │
│                         [Close]    │
└─────────────────────────────────────┘
```

---

## Available Plugins

### Content Analysis
1. **translate** - Translate documents
2. **summarize** - Generate summaries
3. **extract-keywords** - Extract keywords/tags
4. **extract-entities** - Extract people, orgs, dates, amounts
5. **answer-question** - Q&A on content
6. **analyze-sentiment** - Sentiment & tone analysis
7. **identify-topics** - Identify themes

### Classification
8. **classify** - Auto-determine document type
9. **find-duplicates** - Find similar documents
10. **check-compliance** - Verify requirements

---

## Programmatic Usage (Java)

### Execute Plugin Directly
```java
@Autowired
private PluginService pluginService;

// Simple execution
Map<String, Object> params = Map.of("targetLanguage", "Spanish");
PluginResponse response = pluginService.executePlugin(documentId, "translate", params);

// Check results
if (response.getStatus() == PluginResponse.PluginStatus.SUCCESS) {
    String translation = response.getData("translatedContent");
    String targetLang = response.getData("targetLanguage");
}
```

### Get Plugin Metadata
```java
// Get all plugins with metadata
List<PluginInfoDTO> plugins = pluginService.getDetailedPluginInfo();

// Get specific plugin
PluginInfoDTO pluginInfo = pluginService.getPluginInfo("translate");
List<PluginParameter> params = pluginInfo.getParameters();
```

### Create Custom Plugin
```java
@Component
public class MyPlugin implements DocumentPlugin, PluginMetadata {
    
    private final ChatModel chatModel;
    
    @Override
    public String getTaskName() { return "my-task"; }
    
    @Override
    public String getDescription() { return "My custom plugin"; }
    
    @Override
    public String getCategory() { return "Custom"; }
    
    @Override
    public String getIcon() { return "STAR"; }
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            PluginParameter.builder()
                .name("myParam")
                .label("My Parameter")
                .type(ParameterType.TEXT)
                .required(true)
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        String content = request.getContent();
        String param = request.getParameter("myParam", "default");
        
        // Process with LLM
        String result = chatModel.call(new Prompt("Process: " + content))
            .getResult().getOutput().getContent();
        
        return PluginResponse.builder()
            .status(PluginResponse.PluginStatus.SUCCESS)
            .data(Map.of("result", result))
            .build();
    }
}
```

Plugin will automatically appear in UI menu and REST API!

---

## Testing

### Test REST API
```bash
# Get plugins
curl http://localhost:8080/api/documents/1/plugins/detailed | jq

# Execute translation
curl -X POST http://localhost:8080/api/documents/1/plugins/translate \
  -H "Content-Type: application/json" \
  -d '{"targetLanguage": "French"}' | jq
```

### Test UI
1. Start application: `mvn spring-boot:run`
2. Navigate to http://localhost:8080/folders
3. Create a folder and add a document with text content
4. Open document detail dialog
5. Click "AI Plugins" → Select any plugin
6. Fill parameters and execute
7. View results in formatted dialog

---

## Plugin Development Guide

### Parameter Types Reference

```java
PluginParameter.builder()
    .name("paramName")           // Internal key
    .label("Display Name")       // UI label
    .description("Help text")    // Tooltip/helper
    .type(ParameterType.SELECT)  // TEXT, TEXTAREA, NUMBER, SELECT, BOOLEAN
    .required(false)             // Optional field
    .defaultValue("default")     // Pre-filled value
    .options(List.of(...))       // For SELECT type
    .minValue(1)                 // For NUMBER type
    .maxValue(100)               // For NUMBER type
    .build()
```

### Best Practices

1. **Keep prompts focused** - Specific instructions work better
2. **Limit content length** - 4000 chars recommended for LLM
3. **Provide defaults** - Make plugins easy to use
4. **Structure output** - Use consistent format for parsing
5. **Handle errors** - Wrap in try-catch, throw PluginException
6. **Add metadata** - Implement PluginMetadata for UI integration
7. **Test thoroughly** - Verify with various document types

---

## Architecture Overview

```
┌──────────────┐
│   UI Layer   │ ← FolderView with AI Plugins Menu
│              │ ← PluginExecutionDialog (dynamic forms)
│              │ ← PluginResultDialog (formatted output)
└──────┬───────┘
       │
┌──────▼────────┐
│  REST Layer   │ ← DocumentPluginController
│               │   - GET /plugins (list)
│               │   - GET /plugins/detailed (metadata)
│               │   - POST /plugins/{task} (execute)
└──────┬────────┘
       │
┌──────▼────────┐
│ Service Layer │ ← PluginService
│               │   - executePlugin()
│               │   - getDetailedPluginInfo()
└──────┬────────┘
       │
┌──────▼────────┐
│Plugin Registry│ ← Auto-discovers @Component plugins
│               │ ← Manages plugin lifecycle
└──────┬────────┘
       │
┌──────▼────────┐
│   Plugins     │ ← TranslatorPlugin, SummarizerPlugin, etc.
│               │ ← Implement DocumentPlugin + PluginMetadata
│               │ ← Use ChatModel (Spring AI)
└───────────────┘
```

## Success! 🎉

The plugin system is fully operational with:
- ✅ Dynamic UI generation from plugin metadata
- ✅ REST API with comprehensive metadata support
- ✅ Categorized plugin menu
- ✅ Flexible parameter handling
- ✅ Professional result display
- ✅ Easy plugin development

Add new plugins by simply creating a @Component class!
