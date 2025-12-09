# Plugin Parameters Reference

All plugins now properly declare their parameters through the `PluginMetadata` interface. The UI dynamically generates appropriate input fields based on parameter types.

## Plugin Parameters by Category

### Content Analysis

#### 1. Translate
- **targetLanguage** (SELECT) - Language to translate to
  - Options: English, Spanish, French, German, Italian, Portuguese, Russian, Chinese, Japanese, Korean, Arabic, Hindi
  - Default: English
  - Required: No

#### 2. Summarize
- **length** (SELECT) - Summary detail level
  - Options: brief, standard, detailed
  - Default: standard
  - Required: No

#### 3. Extract Keywords
- **maxKeywords** (NUMBER) - Maximum number of keywords to extract
  - Range: 1-50
  - Default: 10
  - Required: No

#### 4. Extract Entities
- **No parameters required** - Automatically extracts all entity types

#### 5. Answer Question
- **question** (TEXTAREA) - Ask a specific question about this document
  - Required: Yes
  - Multi-line input

#### 6. Analyze Sentiment
- **No parameters required** - Automatically analyzes sentiment and tone

#### 7. Identify Topics
- **maxTopics** (NUMBER) - Maximum number of topics to identify
  - Range: 1-10
  - Default: 5
  - Required: No

### Classification

#### 8. Classify Document
- **No parameters required** - Automatically classifies based on content

#### 9. Find Duplicates
- **maxResults** (NUMBER) - Maximum number of duplicates to find
  - Range: 1-20
  - Default: 5
  - Required: No

#### 10. Check Compliance
- **requirements** (TEXTAREA) - List the requirements this document must meet
  - Required: Yes
  - Multi-line input
  - Example: "Document must include: executive summary, risk assessment, budget breakdown"

---

## Parameter Types

### TEXT
Single-line text input for short strings.
```
Field: [________________________]
```

### TEXTAREA
Multi-line text input (150px height) for longer content like questions or requirements.
```
Field: [________________________]
       [________________________]
       [________________________]
```

### NUMBER
Integer input with optional min/max validation.
```
Count: [  5  ] (1-50)
```

### SELECT
Dropdown with predefined options.
```
Language: [English            ▼]
          [Spanish             ]
          [French              ]
```

### BOOLEAN
Checkbox with description.
```
☐ Option name
  Helper text description
```

---

## UI Behavior

### Dynamic Form Generation
The UI automatically creates the appropriate input field based on the parameter type:

1. **Plugin selected from menu** → `PluginExecutionDialog` opens
2. **Dialog reads plugin metadata** → `getParameters()`
3. **For each parameter:**
   - Creates appropriate input component (TextField, TextArea, IntegerField, ComboBox, Checkbox)
   - Sets label from `label` property
   - Sets helper text/placeholder from `description` property
   - Pre-fills `defaultValue` if provided
   - Applies validation (required, min/max, options)
4. **User fills values** → Stored in parameter map
5. **"Execute" clicked** → Parameters passed to plugin

### Default Values
All plugins with optional parameters provide sensible defaults:
- Users can execute immediately without changing defaults
- Or customize parameters for specific needs

### Validation
- **Required fields**: Cannot execute without filling
- **Number ranges**: Min/max validation enforced
- **Select options**: Only valid options selectable

---

## REST API Usage

### Get Plugin Parameters
```bash
curl http://localhost:8080/api/documents/1/plugins/info/translate | jq
```

Response includes parameter definitions:
```json
{
  "taskName": "translate",
  "parameters": [
    {
      "name": "targetLanguage",
      "label": "Target Language",
      "description": "Language to translate to",
      "type": "SELECT",
      "required": false,
      "defaultValue": "English",
      "options": ["English", "Spanish", ...]
    }
  ]
}
```

### Execute with Parameters
```bash
# With parameter
curl -X POST http://localhost:8080/api/documents/1/plugins/translate \
  -H "Content-Type: application/json" \
  -d '{"targetLanguage": "Spanish"}'

# Without parameters (uses defaults)
curl -X POST http://localhost:8080/api/documents/1/plugins/translate \
  -H "Content-Type: application/json" \
  -d '{}'

# Required parameter
curl -X POST http://localhost:8080/api/documents/1/plugins/answer-question \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the main topic?"}'

# Multiple parameters
curl -X POST http://localhost:8080/api/documents/1/plugins/extract-keywords \
  -H "Content-Type: application/json" \
  -d '{"maxKeywords": 15}'
```

---

## Adding Parameters to New Plugins

When creating a new plugin, implement `PluginMetadata`:

```java
@Component
public class MyPlugin implements DocumentPlugin, PluginMetadata {
    
    @Override
    public List<PluginParameter> getParameters() {
        return List.of(
            // Text input
            PluginParameter.builder()
                .name("textParam")
                .label("Text Field")
                .description("Enter text here")
                .type(ParameterType.TEXT)
                .required(false)
                .defaultValue("default")
                .build(),
            
            // Number input
            PluginParameter.builder()
                .name("count")
                .label("Count")
                .description("How many items")
                .type(ParameterType.NUMBER)
                .required(false)
                .defaultValue("10")
                .minValue(1)
                .maxValue(100)
                .build(),
            
            // Dropdown
            PluginParameter.builder()
                .name("option")
                .label("Choose Option")
                .description("Select one")
                .type(ParameterType.SELECT)
                .required(true)
                .options(List.of("Option A", "Option B", "Option C"))
                .build(),
            
            // Long text
            PluginParameter.builder()
                .name("longText")
                .label("Description")
                .description("Enter detailed description")
                .type(ParameterType.TEXTAREA)
                .required(true)
                .build(),
            
            // Boolean
            PluginParameter.builder()
                .name("flag")
                .label("Enable Feature")
                .description("Check to enable")
                .type(ParameterType.BOOLEAN)
                .required(false)
                .defaultValue("false")
                .build()
        );
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) {
        // Access parameters
        String text = request.getParameter("textParam", "default");
        Integer count = request.getParameter("count", 10);
        String option = request.getParameter("option", "Option A");
        String longText = request.getParameter("longText", "");
        Boolean flag = request.getParameter("flag", false);
        
        // ... process
    }
}
```

The UI will automatically generate the appropriate form!

---

## Summary

✅ **All plugins now declare their parameters**
✅ **UI dynamically generates appropriate input fields**
✅ **REST API exposes parameter metadata**
✅ **Validation is automatic**
✅ **No more "This plugin requires no additional parameters" for plugins that DO need parameters!**

The system is fully generalized - plugins tell the UI what they need, and the UI adapts accordingly!
