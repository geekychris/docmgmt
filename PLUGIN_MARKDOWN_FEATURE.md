# Plugin Result Markdown Export & Target Language

## Overview

The plugin system now supports:
1. **Saving plugin results as markdown files** - Results can be automatically saved as content on the document
2. **Target language selection** - The QuestionAnswerer plugin supports answering in multiple languages

## Features

### 1. Save Results as Markdown

When executing plugins, you can now save the results as a markdown file that gets attached to the document as content.

#### UI Usage

1. Open a document in the Folder View
2. Click **AI Plugins** menu and select a plugin
3. Fill in the required parameters
4. **Check** the "Save result as markdown file" checkbox at the bottom
5. Click **Execute**

The result will be:
- Displayed in a dialog as usual
- **Also saved** as a `.md` file attached to the document
- Filename format: `{plugin-task-name}_result_{timestamp}.md`
- Content type: `text/markdown`

#### REST API Usage

Add the `saveAsMarkdown` query parameter to your plugin execution request:

```bash
# Execute plugin and save result as markdown
curl -X POST "http://localhost:8082/api/documents/1/plugins/answer-question?saveAsMarkdown=true" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is this document about?",
    "targetLanguage": "Spanish"
  }'
```

**Response includes:**
```json
{
  "status": "SUCCESS",
  "data": { ... },
  "error": null,
  "savedAsMarkdown": true
}
```

### 2. Target Language for Question Answerer

The Question Answerer plugin now supports answering in multiple languages.

#### Supported Languages
- English (default)
- Spanish
- French
- German
- Italian
- Portuguese
- Dutch
- Russian
- Japanese
- Korean
- Chinese
- Arabic

#### UI Usage

1. Select the **"Answer specific questions about document"** plugin
2. Enter your question in the text area
3. Select your desired **Answer Language** from the dropdown
4. Optionally check "Save result as markdown file"
5. Click Execute

The LLM will provide the answer in the selected language.

#### REST API Usage

```bash
curl -X POST "http://localhost:8082/api/documents/1/plugins/answer-question" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the main topic?",
    "targetLanguage": "French"
  }'
```

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "question": "What is the main topic?",
    "answer": "Le sujet principal est...",
    "language": "French"
  },
  "error": null
}
```

### 3. Markdown Format

The saved markdown files follow this structure:

```markdown
# {Plugin Description}

**Generated**: 2025-12-09 03:45:22

---

## Question

What is this document about?

## Answer

This document discusses...

## Language

Spanish
```

## Implementation Details

### Backend Changes

1. **PluginService.java**
   - New method: `executePlugin(documentId, taskName, parameters, saveAsMarkdown)`
   - New private method: `saveResultAsMarkdown()` - Creates markdown content
   - New private method: `formatResultAsMarkdown()` - Formats response data as markdown

2. **DocumentPluginController.java**
   - Added `@RequestParam saveAsMarkdown` to POST endpoint
   - Returns `savedAsMarkdown` flag in response

3. **QuestionAnswererPlugin.java**
   - Added `targetLanguage` parameter (SELECT type)
   - Updated prompt to include language instruction
   - Returns `language` in response data

### Frontend Changes

1. **PluginExecutionDialog.java**
   - Added `saveAsMarkdownCheckbox` component
   - Checkbox appears at the bottom of all plugin execution dialogs
   - Value is passed to `pluginService.executePlugin()`

## Examples

### Example 1: Simple Question in English, Save as Markdown

**UI:** Execute "Answer Question" plugin, check "Save as markdown", click Execute

**API:**
```bash
curl -X POST "http://localhost:8082/api/documents/5/plugins/answer-question?saveAsMarkdown=true" \
  -H "Content-Type: application/json" \
  -d '{"question": "Summarize this in one sentence"}'
```

**Result:** Answer displayed in dialog + file `answer-question_result_20251209_034522.md` created

### Example 2: Question in Spanish, Don't Save

**API:**
```bash
curl -X POST "http://localhost:8082/api/documents/5/plugins/answer-question" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "¿Cuál es el tema principal?",
    "targetLanguage": "Spanish"
  }'
```

**Result:** Answer in Spanish displayed, no file created

### Example 3: Any Plugin with Markdown Save

**Works with ALL plugins:**
```bash
# Translate and save
curl -X POST "http://localhost:8082/api/documents/5/plugins/translate?saveAsMarkdown=true" \
  -H "Content-Type: application/json" \
  -d '{"targetLanguage": "fr"}'

# Extract keywords and save
curl -X POST "http://localhost:8082/api/documents/5/plugins/extract-keywords?saveAsMarkdown=true" \
  -H "Content-Type: application/json" \
  -d '{"maxKeywords": 10}'
```

## Technical Notes

- Markdown files are stored as `Content` entities with `contentType = "text/markdown"`
- Files are stored directly in the database (not in FileStore)
- Saving is optional and non-blocking - failures don't prevent plugin execution
- Timestamp format: `yyyyMMdd_HHmmss`
- All plugin responses can be saved as markdown (not just QuestionAnswerer)
- Language parameter only affects QuestionAnswerer plugin currently

## Future Enhancements

Potential improvements:
- Add target language to other plugins (translate, summarize, etc.)
- Support custom filename for saved markdown
- Option to save to FileStore instead of database
- Export multiple results as a single combined markdown
- Markdown template customization
