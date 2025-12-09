# Quick Start: AI Field Extraction

## 1. Install and Start Ollama

```bash
# Install Ollama (macOS)
brew install ollama

# Pull the AI model
ollama pull llama3.2

# Start the Ollama service
ollama serve
```

Keep the `ollama serve` terminal window open while using the feature.

## 2. Start the Application

```bash
mvn spring-boot:run
```

## 3. Use Field Extraction

### Option A: Via Web UI

1. Open browser: http://localhost:8082/docmgmt
2. Navigate to **Folders** view
3. Create a folder and add a document with text content
4. Upload a PDF or text file to the document
5. If PDF, click "Transform to Text" first
6. Double-click the document to open details
7. Click **"Extract Fields (AI)"** button
8. Wait for AI analysis (~10-30 seconds)
9. Review suggestions and select which fields to apply
10. Click **"Apply Selected"**

### Option B: Via REST API

```bash
# First, create a document and upload content via the UI or API

# Extract field suggestions
curl http://localhost:8082/docmgmt/api/documents/1/extract-fields

# Apply suggestions (example)
curl -X POST http://localhost:8082/docmgmt/api/documents/1/apply-fields \
  -H "Content-Type: application/json" \
  -d '{
    "fieldsToApply": {
      "description": true,
      "keywords": true,
      "tags": true,
      "documentType": true
    },
    "suggestedFields": {
      "description": "AI-suggested description",
      "keywords": "keyword1 keyword2",
      "tags": ["tag1", "tag2"],
      "documentType": "ARTICLE"
    }
  }'
```

## Troubleshooting

**"Connection refused" error:**
- Make sure `ollama serve` is running

**"Model not found" error:**
```bash
ollama pull llama3.2
```

**"No text content available" error:**
- Document needs text-based content
- For PDFs, use "Transform to Text" button first

## Next Steps

See [AI_FIELD_EXTRACTION.md](AI_FIELD_EXTRACTION.md) for:
- Detailed configuration options
- Different AI models
- Performance tuning
- Advanced usage
