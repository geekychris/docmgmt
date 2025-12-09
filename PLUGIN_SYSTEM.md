# Document Management Plugin System

## Overview

The plugin system allows you to execute LLM-powered operations on documents. Plugins receive document content and parameters, process them using AI, and return structured results.

## Architecture

### Core Components

1. **DocumentPlugin Interface** - Base interface for all plugins
   - `getTaskName()` - Returns the unique task identifier (e.g., "translate")
   - `execute(PluginRequest)` - Processes the document and returns results
   - `getDescription()` - Returns human-readable description

2. **PluginRegistry** - Auto-discovers and manages all plugins
   - Automatically registers all Spring beans implementing `DocumentPlugin`
   - Provides plugin lookup by task name

3. **PluginService** - Executes plugins on documents
   - Extracts text content from documents
   - Manages plugin lifecycle
   - Handles errors and transactions

4. **PluginRequest/PluginResponse** - Data transfer objects
   - Request contains: document, content text, and parameters
   - Response contains: status, result data, and error messages

## Available Plugins

### TranslatorPlugin

**Task Name:** `translate`

**Description:** Detects the language of document content and translates it to a target language

**Parameters:**
- `targetLanguage` (String) - The target language name (e.g., "English", "Spanish", "French")
  - Default: "English"

**Response Data:**
- `originalLanguage` (String) - ISO 639-1 language code of source (e.g., "en")
- `originalLanguageName` (String) - Human-readable source language name
- `targetLanguage` (String) - ISO 639-1 language code of target
- `targetLanguageName` (String) - Human-readable target language name
- `originalContent` (String) - The original text content
- `translatedContent` (String) - The translated text
- `truncated` (Boolean) - Whether content was truncated (max 4000 chars)

**Example Usage (REST API):**
```bash
POST /api/documents/{documentId}/plugins/translate
Content-Type: application/json

{
  "targetLanguage": "Spanish"
}
```

**Example Response:**
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
  }
}
```

## REST API

### Execute Plugin
```
POST /api/documents/{documentId}/plugins/{taskName}
```

Execute a plugin on a document.

**Path Parameters:**
- `documentId` - The document ID
- `taskName` - The plugin task name (e.g., "translate")

**Request Body:**
```json
{
  "parameter1": "value1",
  "parameter2": "value2"
}
```

**Response:**
```json
{
  "status": "SUCCESS|PARTIAL_SUCCESS|FAILURE",
  "data": {
    // Plugin-specific result data
  },
  "error": "Error message if failed"
}
```

### List Available Plugins
```
GET /api/documents/{documentId}/plugins
```

Returns a map of available plugins and their descriptions.

**Response:**
```json
{
  "translate": "Detects the language of document content and translates it to a target language"
}
```

## UI Integration

The plugin system is integrated into the Folder View in the Vaadin UI:

1. **Document Detail Dialog** - Each document detail view has a "Translate" button
2. **Translation Dialog** - Shows:
   - Target language selector
   - Loading indicator during translation
   - Source and target language codes (ISO 639-1)
   - Original and translated content side-by-side

**Requirements:**
- Document must have text content (text/plain or other text/* MIME type)
- For PDFs, first transform to text using "Transform PDF" button

## Creating New Plugins

### Step 1: Implement DocumentPlugin

```java
@Component
public class MyPlugin implements DocumentPlugin {
    
    private final ChatModel chatModel;
    
    public MyPlugin(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Override
    public String getTaskName() {
        return "my-task";
    }
    
    @Override
    public String getDescription() {
        return "Description of what this plugin does";
    }
    
    @Override
    public PluginResponse execute(PluginRequest request) throws PluginException {
        try {
            String content = request.getContent();
            Document document = request.getDocument();
            
            // Get parameters
            String param = request.getParameter("myParam", "defaultValue");
            
            // Process with LLM
            String prompt = "Process this content: " + content;
            String result = chatModel.call(new Prompt(prompt))
                .getResult()
                .getOutput()
                .getContent();
            
            // Build response
            Map<String, Object> data = new HashMap<>();
            data.put("result", result);
            
            return PluginResponse.builder()
                .status(PluginResponse.PluginStatus.SUCCESS)
                .data(data)
                .build();
                
        } catch (Exception e) {
            throw new PluginException("Processing failed: " + e.getMessage(), e);
        }
    }
}
```

### Step 2: Register as Spring Component

Add the `@Component` annotation to automatically register the plugin.

### Step 3: Use the Plugin

**Via REST API:**
```bash
POST /api/documents/123/plugins/my-task
```

**Via Service:**
```java
PluginResponse response = pluginService.executePlugin(
    documentId, 
    "my-task", 
    Map.of("myParam", "value")
);
```

## Requirements

- Document must have text content (text/plain or text/*)
- Spring AI with ChatModel configured (Ollama by default)
- Valid LLM endpoint configured in application properties

## Configuration

Configure Spring AI in `application.properties`:

```properties
# Ollama configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
```

## Best Practices

1. **Content Limits** - Limit content length for LLM processing (4000 chars recommended)
2. **Error Handling** - Always wrap plugin code in try-catch and throw PluginException
3. **Async Execution** - UI should execute plugins asynchronously to avoid blocking
4. **Status Codes** - Use appropriate PluginStatus (SUCCESS, PARTIAL_SUCCESS, FAILURE)
5. **Documentation** - Provide clear descriptions and parameter documentation
6. **Testing** - Write tests extending BaseTest to verify plugin functionality

## Future Enhancements

Potential plugin ideas:
- **Summarizer** - Generate document summaries
- **Classifier** - Automatically classify document type
- **Extractor** - Extract specific information (dates, names, etc.)
- **Analyzer** - Analyze sentiment or tone
- **Comparator** - Compare two documents for similarity
- **Generator** - Generate content based on document context
