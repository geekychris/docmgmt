# Document Management CLI Tool

A Python command-line interface for interacting with the Document Management API.

## Overview

This tool provides a comprehensive set of commands for managing document resources, including:

- **Filestores**: Container objects that hold documents and content
- **Documents**: Logical entities with metadata and versioning support
- **Content**: Actual binary data/files associated with documents
- **Versions**: Links between documents and their content at specific points in time

The CLI supports all CRUD operations (Create, Read, Update, Delete) for these resources and includes a special command for creating a document with content in a single step.

## Requirements

- Python 3.6 or higher
- Required Python packages:
  - `requests`

Install the required packages:

```bash
pip install requests
```

## Basic Usage

Make the script executable:

```bash
chmod +x docmgmt.py
```

View available commands:

```bash
./docmgmt.py --help
```

## Common Operations

### Working with Filestores

**List all filestores:**

```bash
./docmgmt.py filestore list
```

**Create a new filestore:**

```bash
./docmgmt.py filestore create --name "project-docs" --description "Project documentation"
```

**Get filestore details:**

```bash
./docmgmt.py filestore get --id "filestore-id-123"
```

### Working with Documents

**List documents in a filestore:**

```bash
./docmgmt.py document list --filestore-id "filestore-id-123"
```

**Create a new document:**

```bash
./docmgmt.py document create \
  --filestore-id "filestore-id-123" \
  --name "Requirements Document" \
  --doc-type "SPECIFICATION" \
  --metadata '{"author":"Jane Smith","department":"Engineering"}'
```

**Update a document:**

```bash
./docmgmt.py document update \
  --id "document-id-456" \
  --name "Updated Requirements Document" \
  --metadata '{"author":"Jane Smith","department":"Engineering","status":"Final"}'
```

### Working with Content

**Create a content object:**

```bash
./docmgmt.py content create \
  --filestore-id "filestore-id-123" \
  --name "requirements-content" \
  --content-type "text/markdown"
```

**Upload content data:**

```bash
./docmgmt.py content upload \
  --id "content-id-789" \
  --file "./requirements.md" \
  --content-type "text/markdown"
```

**Download content:**

```bash
./docmgmt.py content download \
  --id "content-id-789" \
  --output "./downloaded-requirements.md"
```

### Working with Versions

**List versions of a document:**

```bash
./docmgmt.py version list --document-id "document-id-456"
```

**Create a new version:**

```bash
./docmgmt.py version create \
  --document-id "document-id-456" \
  --content-id "content-id-789" \
  --version-number 2 \
  --metadata '{"comment":"Updated requirements with stakeholder feedback"}'
```

## Complete Workflows

### Creating a Document with Content in One Step

This is a special command that combines document creation, content creation, and versioning in a single operation:

```bash
./docmgmt.py document create-with-content \
  --filestore-id "filestore-id-123" \
  --name "API Documentation" \
  --doc-type "DOCUMENTATION" \
  --content-file "./api-docs.md" \
  --content-type "text/markdown" \
  --comment "Initial version of API documentation"
```

### Document Versioning Workflow

1. First, create a document:

```bash
./docmgmt.py document create \
  --filestore-id "filestore-id-123" \
  --name "Project Plan" \
  --doc-type "PLAN"
```

2. Create and upload content for the first version:

```bash
./docmgmt.py content create \
  --filestore-id "filestore-id-123" \
  --name "project-plan-v1" \
  --content-type "application/pdf"

./docmgmt.py content upload \
  --id "content-id-v1" \
  --file "./project-plan-v1.pdf"
```

3. Link the document and content with version 1:

```bash
./docmgmt.py version create \
  --document-id "document-id-plan" \
  --content-id "content-id-v1" \
  --version-number 1 \
  --metadata '{"comment":"Initial project plan"}'
```

4. Later, create and upload content for the second version:

```bash
./docmgmt.py content create \
  --filestore-id "filestore-id-123" \
  --name "project-plan-v2" \
  --content-type "application/pdf"

./docmgmt.py content upload \
  --id "content-id-v2" \
  --file "./project-plan-v2.pdf"
```

5. Link the document and new content with version 2:

```bash
./docmgmt.py version create \
  --document-id "document-id-plan" \
  --content-id "content-id-v2" \
  --version-number 2 \
  --metadata '{"comment":"Updated timeline and resource allocation"}'
```

## Tips and Advanced Usage

### Output Formatting

Control the output format with the `--format` option:

```bash
# Pretty-printed JSON (default)
./docmgmt.py filestore list

# Compact JSON (useful for scripting)
./docmgmt.py filestore list --format json
```

### Custom API Endpoint

If your Document Management API is running on a different server or port, use the `--base-url` option:

```bash
./docmgmt.py filestore list --base-url "http://api.example.com/docmgmt/api"
```

### Scripting with the CLI

The JSON output format makes it easy to use the CLI in scripts:

```bash
# Save a filestore ID to a variable
FILESTORE_ID=$(./docmgmt.py filestore create --name "scripted-filestore" --format json | jq -r '.id')

# Use the filestore ID in subsequent commands
./docmgmt.py document create \
  --filestore-id "$FILESTORE_ID" \
  --name "Generated Document" \
  --doc-type "REPORT"
```

## Troubleshooting

- If you receive a connection error, ensure the Document Management API server is running and accessible at the correct URL.
- For "Not Found" errors when retrieving resources, verify that the IDs are correct.
- If upload operations fail, check that the file exists and you have read permissions.
- For authentication issues, consult the API documentation for authentication requirements.

