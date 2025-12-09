# AI-Powered Document Field Extraction

This document describes the AI field extraction feature that uses Spring AI with Ollama to automatically suggest document metadata fields based on content.

## Overview

The field extraction feature analyzes document text content using a local AI model (via Ollama) to suggest appropriate values for:
- **Description**: A concise summary of the document
- **Keywords**: Space-separated keywords relevant to the content
- **Tags**: Categorization terms for the document
- **Document Type**: The type of document (ARTICLE, REPORT, CONTRACT, MANUAL, PRESENTATION, TRIP_REPORT, OTHER)

## Prerequisites

### 1. Ollama Installation

Install Ollama on your system:
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh
```

### 2. Download AI Model

Pull the Llama 3.2 model (or your preferred model):
```bash
ollama pull llama3.2
```

### 3. Start Ollama Service

```bash
ollama serve
```

This starts Ollama on `http://localhost:11434` by default.

## Configuration

### Application Properties

The following configuration in `application.properties` controls the AI integration:

```properties
# Spring AI Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.chat.options.temperature=0.7
```

You can change:
- `base-url`: If Ollama is running on a different host/port
- `model`: To use a different Ollama model (e.g., `llama2`, `mistral`, etc.)
- `temperature`: Controls randomness (0.0 = deterministic, 1.0 = more creative)

## Usage

### Via Web UI (Folder View)

1. Navigate to the **Folders** view
2. Select a folder containing documents
3. Double-click on a document to open its detail view
4. Click the **"Extract Fields (AI)"** button
5. Wait for AI analysis (usually 5-30 seconds depending on document size)
6. Review suggested fields vs current values
7. Check which fields you want to apply
8. Click **"Apply Selected"** to update the document

### Via REST API

#### Extract Field Suggestions

```bash
GET /api/documents/{id}/extract-fields
```

**Response:**
```json
{
  "currentFields": {
    "description": "Current description or null",
    "keywords": "current keywords",
    "tags": ["tag1", "tag2"],
    "documentType": "REPORT"
  },
  "suggestedFields": {
    "description": "AI-generated description",
    "keywords": "ai suggested keywords",
    "tags": ["ai-tag1", "ai-tag2", "ai-tag3"],
    "documentType": "ARTICLE"
  }
}
```

#### Apply Field Suggestions

```bash
POST /api/documents/{id}/apply-fields
Content-Type: application/json

{
  "fieldsToApply": {
    "description": true,
    "keywords": false,
    "tags": true,
    "documentType": true
  },
  "suggestedFields": {
    "description": "New description",
    "keywords": "keywords here",
    "tags": ["tag1", "tag2"],
    "documentType": "ARTICLE"
  }
}
```

## Technical Architecture

### Components

1. **DocumentFieldExtractionService** (`com.docmgmt.service`)
   - Core service that orchestrates field extraction
   - Extracts text content from documents
   - Sends content to AI model with structured prompt
   - Parses AI response into structured fields

2. **FieldSuggestionDTO** (`com.docmgmt.dto`)
   - Data transfer object for field suggestions
   - Contains both current and suggested field values

3. **DocumentController** (REST API endpoints)
   - `GET /api/documents/{id}/extract-fields` - Get suggestions
   - `POST /api/documents/{id}/apply-fields` - Apply suggestions

4. **FolderView UI** (`com.docmgmt.ui.views`)
   - "Extract Fields (AI)" button in document detail dialog
   - Field comparison dialog with checkboxes
   - Asynchronous processing with loading indicator

### Content Extraction

The service extracts text from documents using this priority:

1. **Text renditions**: Looks for `text/plain` secondary renditions (created by PDF-to-text transformers)
2. **Primary text content**: If primary content has a `text/*` content type
3. **Error**: If no text content is available

Text content is truncated to 4000 characters to avoid exceeding model token limits.

### AI Prompt

The service uses a structured prompt that:
- Provides the document text
- Specifies the exact JSON format for the response
- Includes guidelines for each field
- Requests valid DocumentType enum values

### Response Parsing

The service handles:
- JSON responses wrapped in markdown code blocks
- Invalid JSON gracefully (returns error message)
- Invalid document type values (defaults to OTHER)
- Missing or empty field values

## Error Handling

Common errors and solutions:

### "Document has no text content available"

**Cause**: Document has no text-based content or text rendition.

**Solution**: 
1. Upload a text file or PDF
2. If PDF, run "Transform to Text" first to create a text rendition

### "Failed to extract fields: Connection refused"

**Cause**: Ollama service is not running.

**Solution**:
```bash
ollama serve
```

### "Failed to extract fields: model not found"

**Cause**: Configured model is not pulled.

**Solution**:
```bash
ollama pull llama3.2
```

### Slow Performance

**Cause**: Large documents or limited compute resources.

**Solution**:
- Use smaller/faster models (e.g., `llama3.2` vs `llama3.2:70b`)
- Ensure adequate RAM (8GB+ recommended)
- Consider GPU acceleration if available

## Advanced Usage

### Using Different Models

Edit `application.properties`:
```properties
# Use Mistral instead
spring.ai.ollama.chat.options.model=mistral

# Or use a larger Llama model
spring.ai.ollama.chat.options.model=llama3.2:70b
```

Then pull the model:
```bash
ollama pull mistral
```

### Adjusting AI Behavior

**More deterministic (consistent) responses:**
```properties
spring.ai.ollama.chat.options.temperature=0.3
```

**More creative responses:**
```properties
spring.ai.ollama.chat.options.temperature=1.0
```

### Remote Ollama Instance

If running Ollama on a different machine:
```properties
spring.ai.ollama.base-url=http://192.168.1.100:11434
```

## Dependencies

Added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>
```

Plus Spring milestone repository for accessing Spring AI artifacts.

## Performance Considerations

- **First request**: May take longer (20-60 seconds) as model loads into memory
- **Subsequent requests**: Faster (5-20 seconds) while model remains in memory
- **Memory usage**: Ollama keeps models in RAM; plan for 4-8GB per model
- **Concurrent requests**: Ollama handles queueing; UI shows loading indicator

## Future Enhancements

Potential improvements:
- Batch processing for multiple documents
- Field extraction during document upload
- Confidence scores for suggestions
- Custom prompts per document type
- Integration with other AI providers (OpenAI, Anthropic, etc.)
- Caching of suggestions to avoid re-processing

## Troubleshooting

Enable debug logging:
```properties
logging.level.com.docmgmt.service.DocumentFieldExtractionService=DEBUG
```

This will log:
- AI responses (for debugging parsing issues)
- Content extraction details
- Error messages with stack traces
