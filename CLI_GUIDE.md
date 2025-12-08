# Document Management CLI Guide

## Overview

The Document Management CLI (`docmgmt-cli.py`) provides a command-line interface to the Document Management System REST API.

## Requirements

- Python 3.6+
- `requests` library: `pip install requests`

## Quick Start

```bash
# Make executable (first time only)
chmod +x docmgmt-cli.py

# Get help
./docmgmt-cli.py --help

# List commands
./docmgmt-cli.py documents --help
./docmgmt-cli.py content --help
./docmgmt-cli.py search --help
```

## Document Operations

### List Documents

```bash
# List all latest document versions
./docmgmt-cli.py documents list
```

### Create Document

```bash
./docmgmt-cli.py documents create \
  --name "Spring Boot Guide" \
  --type MANUAL \
  --description "Comprehensive Spring Boot tutorial" \
  --keywords "spring boot java framework" \
  --tags spring java tutorial
```

**Document Types:**
- `ARTICLE` - Articles, blog posts
- `MANUAL` - Technical manuals, guides  
- `REPORT` - Business reports, analytics
- `SPREADSHEET` - Data sheets, calculations
- `PRESENTATION` - Slide decks
- `IMAGE` - Images, diagrams
- `VIDEO` - Video files
- `AUDIO` - Audio files
- `OTHER` - Other document types

### Get Document

```bash
./docmgmt-cli.py documents get 1
```

### Update Document

```bash
./docmgmt-cli.py documents update 1 \
  --name "Updated Name" \
  --description "New description" \
  --tags updated modified
```

### Delete Document

```bash
./docmgmt-cli.py documents delete 1
```

### Version Management

```bash
# Create major version (1.0 → 2.0)
./docmgmt-cli.py documents version-major 1

# Create minor version (1.0 → 1.1)
./docmgmt-cli.py documents version-minor 1

# Get version history
./docmgmt-cli.py documents version-history 1
```

## Content Operations

### List Content for Document

```bash
./docmgmt-cli.py content list 1
```

### Upload Content

```bash
# Upload to database (small files)
./docmgmt-cli.py content upload \
  --document-id 1 \
  --file document.pdf \
  --store-in-db

# Upload to file store (large files)
./docmgmt-cli.py content upload \
  --document-id 1 \
  --file largefile.mp4 \
  --filestore-id 1
```

### Download Content

```bash
./docmgmt-cli.py content download 1 --output downloaded.pdf
```

### Get Content Metadata

```bash
./docmgmt-cli.py content get 1
```

### Delete Content

```bash
./docmgmt-cli.py content delete 1
```

## Search Operations

### Simple Search

```bash
# Search across all fields
./docmgmt-cli.py search query "spring framework"

# Limit results
./docmgmt-cli.py search query "java" --limit 10
```

### Field-Specific Search

```bash
# Search in specific fields
./docmgmt-cli.py search fields \
  --name tutorial \
  --tags java

# Use OR operator
./docmgmt-cli.py search fields \
  --keywords spring \
  --keywords python \
  --operator OR

# Search in content
./docmgmt-cli.py search fields \
  --content "dependency injection"
```

**Searchable Fields:**
- `--name` - Document name
- `--description` - Document description  
- `--keywords` - Keywords field
- `--tags` - Tags
- `--content` - Indexable content text

### Search Index Management

```bash
# Rebuild search index
./docmgmt-cli.py search rebuild-index

# Get index statistics
./docmgmt-cli.py search stats
```

## File Store Operations

### List File Stores

```bash
# List all file stores
./docmgmt-cli.py filestores list

# List only active file stores
./docmgmt-cli.py filestores list --active-only
```

### Get File Store

```bash
./docmgmt-cli.py filestores get 1
```

### Create File Store

```bash
./docmgmt-cli.py filestores create \
  --name "Primary Storage" \
  --root-path "/data/docmgmt/files" \
  --status ACTIVE
```

## Complete Workflows

### 1. Create Document with Content

```bash
# Step 1: Create document
DOC_ID=$(./docmgmt-cli.py documents create \
  --name "API Documentation" \
  --type MANUAL \
  --description "REST API documentation" \
  --keywords "api rest documentation" \
  --tags api documentation | jq -r '.id')

echo "Created document ID: $DOC_ID"

# Step 2: Upload content
./docmgmt-cli.py content upload \
  --document-id $DOC_ID \
  --file api-docs.pdf \
  --store-in-db

# Step 3: Verify
./docmgmt-cli.py documents get $DOC_ID
```

### 2. Document Versioning Workflow

```bash
# Create initial document
DOC_ID=$(./docmgmt-cli.py documents create \
  --name "Project Plan" \
  --type REPORT \
  --description "Q1 Project Plan" | jq -r '.id')

# Upload v1.0 content
./docmgmt-cli.py content upload \
  --document-id $DOC_ID \
  --file plan-v1.pdf \
  --store-in-db

# Later: Create minor version for updates
DOC_V2=$(./docmgmt-cli.py documents version-minor $DOC_ID | jq -r '.id')

# Upload updated content to v1.1
./docmgmt-cli.py content upload \
  --document-id $DOC_V2 \
  --file plan-v1.1.pdf \
  --store-in-db

# View version history
./docmgmt-cli.py documents version-history $DOC_ID
```

### 3. Search and Download

```bash
# Search for documents
RESULTS=$(./docmgmt-cli.py search query "spring framework" --limit 5)

# Extract first document ID
DOC_ID=$(echo $RESULTS | jq -r '.[0].documentId')

# Get document details
./docmgmt-cli.py documents get $DOC_ID

# List content
CONTENT=$(./docmgmt-cli.py content list $DOC_ID)

# Download first content item
CONTENT_ID=$(echo $CONTENT | jq -r '.[0].id')
./docmgmt-cli.py content download $CONTENT_ID --output downloaded.pdf
```

## Configuration

### Custom API URL

```bash
# Use different server
./docmgmt-cli.py --base-url http://prod-server:8080/docmgmt/api documents list
```

### Environment Variable

```bash
export DOCMGMT_API_URL="http://localhost:8082/docmgmt/api"
# Note: CLI currently uses command-line arg, but could be enhanced to support env var
```

## Output Formatting

All commands output JSON which can be processed with `jq`:

```bash
# Pretty print
./docmgmt-cli.py documents list | jq '.'

# Extract specific field
./docmgmt-cli.py documents get 1 | jq '.name'

# Filter results
./docmgmt-cli.py documents list | jq '.[] | select(.documentType == "MANUAL")'

# Count results
./docmgmt-cli.py search query "java" | jq 'length'
```

## Scripting Examples

### Batch Upload

```bash
#!/bin/bash
# Upload multiple files

for file in documents/*.pdf; do
    echo "Uploading $file..."
    
    # Create document
    DOC_ID=$(./docmgmt-cli.py documents create \
        --name "$(basename $file .pdf)" \
        --type MANUAL \
        --keywords "batch upload" | jq -r '.id')
    
    # Upload content
    ./docmgmt-cli.py content upload \
        --document-id $DOC_ID \
        --file "$file" \
        --store-in-db
    
    echo "✓ Uploaded as document $DOC_ID"
done
```

### Automated Versioning

```bash
#!/bin/bash
# Create new version and upload updated content

DOC_ID=$1
NEW_FILE=$2

# Create minor version
NEW_DOC=$(./docmgmt-cli.py documents version-minor $DOC_ID | jq -r '.id')

# Upload new content
./docmgmt-cli.py content upload \
    --document-id $NEW_DOC \
    --file "$NEW_FILE" \
    --store-in-db

echo "✓ Created version $(./docmgmt-cli.py documents get $NEW_DOC | jq -r '.versionLabel')"
```

## Error Handling

The CLI exits with non-zero status on errors:

```bash
# Check for errors in scripts
if ! ./docmgmt-cli.py documents get 999; then
    echo "Document not found"
    exit 1
fi
```

## Comparison with Old CLI

The old `docmgmt.py` is deprecated. Key differences:

| Old CLI | New CLI | Notes |
|---------|---------|-------|
| `filestore` | `filestores` | Renamed for consistency |
| `document list --filestore-id` | `documents list` | No filestore filter needed |
| `content create` | `content upload` | Simplified workflow |
| `version create` | `documents version-major/minor` | Clearer commands |
| No search | `search query/fields` | New search capabilities |

## Troubleshooting

### Connection Errors

```bash
# Verify API is running
curl http://localhost:8082/docmgmt/api/documents

# Check base URL
./docmgmt-cli.py --base-url http://localhost:8082/docmgmt/api documents list
```

### Import Errors

```bash
# Install dependencies
pip install requests

# Verify Python client is present
ls clients/python/docmgmt_client.py
```

### Permission Errors

```bash
# Make CLI executable
chmod +x docmgmt-cli.py
```

## See Also

- [Python Client Library](clients/python/README.md) - For programmatic access
- [REST API Documentation](docs/OPENAPI_SWAGGER.md) - Full API reference
- [Swagger UI](http://localhost:8082/docmgmt/api/swagger-ui.html) - Interactive API testing
- [Content Renditions Guide](docs/CONTENT_RENDITIONS.md) - PDF transformations and indexing
