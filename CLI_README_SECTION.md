# Command Line Interface

## Overview

The Document Management System includes a modern Python CLI (`docmgmt-cli.py`) for interacting with the REST API from the command line.

## Quick Start

```bash
# Install requirements
pip install requests

# Make CLI executable
chmod +x docmgmt-cli.py

# Get help
./docmgmt-cli.py --help
```

## Basic Usage

```bash
# List documents
./docmgmt-cli.py documents list

# Create a document
./docmgmt-cli.py documents create \
  --name "My Document" \
  --type MANUAL \
  --description "A sample document" \
  --keywords "test example" \
  --tags test sample

# Upload content
./docmgmt-cli.py content upload \
  --document-id 1 \
  --file document.pdf \
  --store-in-db

# Search documents  
./docmgmt-cli.py search query "spring framework"

# Create a new version
./docmgmt-cli.py documents version-major 1
```

## Available Commands

- `documents` - Create, read, update, delete documents and manage versions
- `content` - Upload, download, and manage content files
- `search` - Full-text search with Lucene (simple and field-specific queries)
- `filestores` - Manage file storage locations

## Complete Documentation

For comprehensive CLI documentation including:
- All commands and options
- Complete workflows
- Scripting examples
- Troubleshooting

See: **[CLI_GUIDE.md](CLI_GUIDE.md)**

## Python Client Library

For programmatic access in Python applications, use the client library:

```python
from clients.python.docmgmt_client import DocumentManagementClient

client = DocumentManagementClient()

# Create document
doc = client.create_document(
    name="My Document",
    document_type="MANUAL",
    description="Sample document",
    keywords="test example",
    tags=["test", "sample"]
)

# Upload content
content = client.upload_content_file(
    file_path="document.pdf",
    sysobject_id=doc['id'],
    store_in_database=True
)

# Search
results = client.search("spring framework", limit=10)
```

See: **[clients/python/README.md](clients/python/README.md)**

---

**Note:** The old `docmgmt.py` CLI in the root directory is deprecated. Use `docmgmt-cli.py` instead.
