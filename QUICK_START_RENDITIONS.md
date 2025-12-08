# Quick Start: Working with Content Renditions

## What Are Renditions?

Renditions are multiple representations of the same document content:
- **Primary Rendition**: The original uploaded file (e.g., PDF)
- **Secondary Renditions**: Derived versions (e.g., extracted text from PDF)

Secondary renditions enable:
- Full-text search of PDF documents
- Preview text without loading heavy PDF
- Alternative formats for different use cases

## Quick Actions

### 1. Transform a Single PDF to Text

After importing or uploading a PDF:

```bash
# Replace {content_id} with your PDF's content ID
curl -X POST http://localhost:8082/docmgmt/api/content/{content_id}/transform
```

Example with content ID 5:
```bash
curl -X POST http://localhost:8082/docmgmt/api/content/5/transform
```

### 2. Transform All PDFs (Batch)

Run the provided script:

```bash
cd /Users/chris/code/warp_experiments/docmgmt
./transform_pdfs.sh http://localhost:8082/docmgmt
```

This will:
- Find all PDFs in your system
- Create text renditions for those that don't have them
- Show progress and summary

### 3. View Renditions in UI

1. Navigate to a document detail page
2. Scroll to "Associated Content" section
3. Look for entries with:
   - **Rendition**: "Primary" (PDF) or "Secondary" (text)
   - **Indexable**: ✓ for searchable text
4. Click eye icon to view content

### 4. Check Renditions via API

```bash
# List all renditions for a content item
curl http://localhost:8082/docmgmt/api/content/{primary_content_id}/renditions

# List all content for a document
curl http://localhost:8082/docmgmt/api/content/by-sysobject/{document_id}
```

## Common Workflows

### After Importing Documents via CLI

Your PDFs are uploaded but don't have text renditions yet.

**Solution**:
```bash
# Option 1: Batch transform all PDFs
./transform_pdfs.sh http://localhost:8082/docmgmt

# Option 2: Transform specific document's content
curl -X POST http://localhost:8082/docmgmt/api/content/{pdf_id}/transform
```

### Checking If a PDF Has Text Rendition

```bash
# Get all renditions for the PDF
curl http://localhost:8082/docmgmt/api/content/{pdf_id}/renditions | jq '.[] | select(.contentType == "text/plain")'
```

If output is empty, no text rendition exists.

### Viewing Text Content

```bash
# Download the text rendition
curl http://localhost:8082/docmgmt/api/content/{text_rendition_id}/download
```

Or view in the UI by clicking the eye icon next to the text rendition.

### Deleting Renditions

Deleting primary content automatically deletes all secondary renditions:

```bash
curl -X DELETE http://localhost:8082/docmgmt/api/content/{primary_content_id}
```

To delete only a secondary rendition:
```bash
curl -X DELETE http://localhost:8082/docmgmt/api/content/{secondary_rendition_id}
```

## Understanding the API Response

When you query content, you'll see these new fields:

```json
{
  "id": 5,
  "name": "report.pdf",
  "contentType": "application/pdf",
  "isPrimary": true,              // ← Is this the original?
  "isIndexable": false,           // ← Should this be searched?
  "parentRenditionId": null,      // ← Link to primary (for secondary)
  "sysObjectId": 3,
  "storageType": "DATABASE",
  "size": 524288,
  "formattedSize": "512.0 KB"
}
```

After transformation, the text rendition looks like:
```json
{
  "id": 6,
  "name": "report.pdf.txt",
  "contentType": "text/plain",
  "isPrimary": false,             // ← This is a secondary rendition
  "isIndexable": true,            // ← This can be searched
  "parentRenditionId": 5,         // ← Points to the PDF
  "sysObjectId": 3,
  "storageType": "DATABASE",
  "size": 15360,
  "formattedSize": "15.0 KB"
}
```

## Troubleshooting

### "No transformer found for content type"

**Problem**: Trying to transform a non-PDF file.

**Solution**: Currently only PDF→text transformation is supported. Check:
```bash
curl http://localhost:8082/docmgmt/api/content/{id} | jq '.contentType'
```

Should return `"application/pdf"`.

### "Content not found"

**Problem**: Invalid content ID.

**Solution**: List content for a document first:
```bash
curl http://localhost:8082/docmgmt/api/content/by-sysobject/{document_id}
```

### Script Says "jq not installed"

**Problem**: The `transform_pdfs.sh` script requires `jq` for JSON parsing.

**Solution**: Install jq:
```bash
# macOS
brew install jq

# Linux
sudo apt-get install jq  # Debian/Ubuntu
sudo yum install jq      # RedHat/CentOS
```

### Renditions Not Showing in UI

**Problem**: Text renditions exist but don't appear in the document detail view.

**Checklist**:
1. Restart the application: `mvn spring-boot:run`
2. Clear browser cache
3. Check API response includes renditions:
   ```bash
   curl http://localhost:8082/docmgmt/api/content/by-sysobject/{doc_id}
   ```
4. Verify both primary and secondary appear in JSON

### PDF Has No Text

**Problem**: PDF transformation completes but text rendition is empty or very small.

**Possible Causes**:
- PDF is image-based (scanned document) with no extractable text
- PDF is encrypted
- PDF has text in unusual encoding

**Verification**:
```bash
# Download and check the text rendition
curl http://localhost:8082/docmgmt/api/content/{text_id}/download

# Check its size
curl http://localhost:8082/docmgmt/api/content/{text_id} | jq '.size'
```

## Advanced Usage

### Transform to Specific Format

By default, PDFs transform to text. To specify a target format:

```bash
curl -X POST "http://localhost:8082/docmgmt/api/content/{id}/transform?targetContentType=text/plain"
```

### Query Only Indexable Content

To find all searchable content for a document:

```bash
curl http://localhost:8082/docmgmt/api/content/by-sysobject/{doc_id} | jq '.[] | select(.isIndexable == true)'
```

### Find PDFs Without Text Renditions

```bash
# Get all content for a document
curl http://localhost:8082/docmgmt/api/content/by-sysobject/{doc_id} > content.json

# Check which PDFs are missing text renditions
jq -r '.[] | select(.contentType == "application/pdf" and .isPrimary == true) | .id' content.json | while read pdf_id; do
  count=$(curl -s "http://localhost:8082/docmgmt/api/content/${pdf_id}/renditions" | jq '[.[] | select(.contentType == "text/plain")] | length')
  if [ "$count" -eq 0 ]; then
    echo "PDF ID $pdf_id needs transformation"
  fi
done
```

## Integration Examples

### Shell Script to Auto-Transform New PDFs

```bash
#!/bin/bash
# watch_and_transform.sh - Monitor for new PDFs and transform them

LAST_ID=0

while true; do
  # Get latest PDFs
  PDFS=$(curl -s http://localhost:8082/docmgmt/api/documents | \
         jq -r '.[].id' | \
         while read doc_id; do
           curl -s "http://localhost:8082/docmgmt/api/content/by-sysobject/${doc_id}"
         done | \
         jq -r 'select(.contentType == "application/pdf" and .isPrimary == true and .id > '$LAST_ID') | .id')
  
  for pdf_id in $PDFS; do
    echo "Transforming PDF ID: $pdf_id"
    curl -X POST "http://localhost:8082/docmgmt/api/content/${pdf_id}/transform"
    LAST_ID=$pdf_id
  done
  
  sleep 60  # Check every minute
done
```

### Python Example

```python
import requests

BASE_URL = "http://localhost:8082/docmgmt/api"

def transform_pdf(content_id):
    """Transform a PDF to text rendition"""
    url = f"{BASE_URL}/content/{content_id}/transform"
    response = requests.post(url)
    
    if response.status_code == 201:
        rendition = response.json()
        print(f"Created text rendition: {rendition['name']}")
        return rendition
    else:
        print(f"Failed: {response.text}")
        return None

def get_renditions(content_id):
    """Get all renditions for a content item"""
    url = f"{BASE_URL}/content/{content_id}/renditions"
    response = requests.get(url)
    return response.json() if response.ok else []

# Transform a PDF
transform_pdf(5)

# Check renditions
renditions = get_renditions(5)
for r in renditions:
    print(f"{r['name']}: {r['contentType']} (Primary: {r['isPrimary']})")
```

## More Information

- **Full Documentation**: See `docs/TROUBLESHOOTING_CONTENT_RENDITIONS.md`
- **Changes Summary**: See `CHANGES_SUMMARY.md`
- **Content Rendition Design**: See `docs/CONTENT_RENDITIONS.md`
- **API Documentation**: http://localhost:8082/docmgmt/api/swagger-ui.html
