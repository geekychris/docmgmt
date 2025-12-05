# Document Versioning & Copy-on-Write API Guide

This guide demonstrates how to use the Document Management System's REST API for versioning documents and managing content with copy-on-write semantics.

## Table of Contents
- [Overview](#overview)
- [Base URL](#base-url)
- [Authentication](#authentication)
- [Document Operations](#document-operations)
- [Content Operations](#content-operations)
- [Versioning Operations](#versioning-operations)
- [Copy-on-Write Workflow](#copy-on-write-workflow)
- [Complete Examples](#complete-examples)

## Overview

The Document Management System supports versioning with copy-on-write semantics for content:
- When a document is versioned, content entities are cloned
- Content initially shares the same underlying data (database blob or file)
- When content is modified in one version, it becomes independent
- Other versions retain their original content

## Base URL

```
http://localhost:8080/api
```

## Authentication

Currently, the API does not require authentication. This should be added for production use.

## Document Operations

### Create a Document

**Request:**
```http
POST /documents
Content-Type: application/json

{
  "name": "Technical Specification",
  "documentType": "REPORT",
  "description": "System architecture documentation",
  "author": "John Doe",
  "keywords": "architecture system design",
  "tags": ["technical", "architecture"]
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "name": "Technical Specification",
  "documentType": "REPORT",
  "description": "System architecture documentation",
  "author": "John Doe",
  "keywords": "architecture system design",
  "majorVersion": 1,
  "minorVersion": 0,
  "version": "1.0",
  "tags": ["technical", "architecture"],
  "createdAt": "2025-12-05T07:30:00",
  "modifiedAt": "2025-12-05T07:30:00"
}
```

### Get Document by ID

**Request:**
```http
GET /documents/{id}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Technical Specification",
  "documentType": "REPORT",
  "majorVersion": 1,
  "minorVersion": 0,
  "version": "1.0",
  ...
}
```

### Update Document

**Request:**
```http
PUT /documents/{id}
Content-Type: application/json

{
  "id": 1,
  "name": "Technical Specification - Updated",
  "documentType": "REPORT",
  "description": "Updated system architecture documentation",
  ...
}
```

**Response:** `200 OK` - Returns updated document

## Content Operations

### Upload Content to Database

**Request:**
```http
POST /content/upload
Content-Type: multipart/form-data

file: [binary file data]
sysObjectId: 1
storeInDatabase: true
```

**Response:** `201 Created`
```json
{
  "id": 10,
  "name": "architecture-diagram.png",
  "contentType": "image/png",
  "sysObjectId": 1,
  "storageType": "DATABASE",
  "createdAt": "2025-12-05T07:31:00",
  "modifiedAt": "2025-12-05T07:31:00"
}
```

### Upload Content to File Store

**Request:**
```http
POST /content/upload
Content-Type: multipart/form-data

file: [binary file data]
sysObjectId: 1
storeInDatabase: false
fileStoreId: 5
```

**Response:** `201 Created`
```json
{
  "id": 11,
  "name": "specification.pdf",
  "contentType": "application/pdf",
  "sysObjectId": 1,
  "storageType": "FILE_STORE",
  "fileStoreId": 5,
  "createdAt": "2025-12-05T07:32:00",
  "modifiedAt": "2025-12-05T07:32:00"
}
```

### Download Content

**Request:**
```http
GET /content/{id}/download
```

**Response:** `200 OK`
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="specification.pdf"

[binary file data]
```

### Get Content Metadata

**Request:**
```http
GET /content/{id}
```

**Response:** `200 OK`
```json
{
  "id": 11,
  "name": "specification.pdf",
  "contentType": "application/pdf",
  "sysObjectId": 1,
  "storageType": "FILE_STORE",
  "fileStoreId": 5
}
```

### Get All Content for a Document

**Request:**
```http
GET /content/by-sysobject/{sysObjectId}
```

**Response:** `200 OK`
```json
[
  {
    "id": 10,
    "name": "architecture-diagram.png",
    "contentType": "image/png",
    "storageType": "DATABASE"
  },
  {
    "id": 11,
    "name": "specification.pdf",
    "contentType": "application/pdf",
    "storageType": "FILE_STORE"
  }
]
```

## Versioning Operations

### Create Major Version

Creates a new major version (e.g., 1.0 → 2.0)

**Request:**
```http
POST /documents/{id}/versions/major
```

**Response:** `201 Created`
```json
{
  "id": 2,
  "name": "Technical Specification",
  "documentType": "REPORT",
  "majorVersion": 2,
  "minorVersion": 0,
  "version": "2.0",
  "parentVersionId": 1,
  ...
}
```

### Create Minor Version

Creates a new minor version (e.g., 1.0 → 1.1)

**Request:**
```http
POST /documents/{id}/versions/minor
```

**Response:** `201 Created`
```json
{
  "id": 3,
  "name": "Technical Specification",
  "documentType": "REPORT",
  "majorVersion": 1,
  "minorVersion": 1,
  "version": "1.1",
  "parentVersionId": 1,
  ...
}
```

### Get Version History

**Request:**
```http
GET /documents/{id}/versions/history
```

**Response:** `200 OK`
```json
[
  {
    "id": 3,
    "version": "2.0",
    "majorVersion": 2,
    "minorVersion": 0,
    "isLatestVersion": true,
    "createdAt": "2025-12-05T07:35:00"
  },
  {
    "id": 2,
    "version": "1.1",
    "majorVersion": 1,
    "minorVersion": 1,
    "isLatestVersion": false,
    "createdAt": "2025-12-05T07:34:00"
  },
  {
    "id": 1,
    "version": "1.0",
    "majorVersion": 1,
    "minorVersion": 0,
    "isLatestVersion": false,
    "createdAt": "2025-12-05T07:30:00"
  }
]
```

## Copy-on-Write Workflow

### Scenario: Update Content in New Version Only

This demonstrates the copy-on-write behavior where modifying content in a new version doesn't affect the original.

#### Step 1: Create Document v1.0
```http
POST /documents
Content-Type: application/json

{
  "name": "User Manual",
  "documentType": "MANUAL",
  "description": "Product user manual"
}
```
Response: Document ID = 1, version = "1.0"

#### Step 2: Add Content to v1.0
```http
POST /content/upload
Content-Type: multipart/form-data

file: manual-v1.pdf (content: "This is version 1.0")
sysObjectId: 1
storeInDatabase: true
```
Response: Content ID = 10

#### Step 3: Verify v1.0 Content
```http
GET /content/10/download
```
Response: Downloads "This is version 1.0"

#### Step 4: Create v2.0
```http
POST /documents/1/versions/major
```
Response: Document ID = 2, version = "2.0"

#### Step 5: Check v2.0 Content (Initially Shared)
```http
GET /content/by-sysobject/2
```
Response: Shows content with ID 11 (cloned from 10)

```http
GET /content/11/download
```
Response: Downloads "This is version 1.0" (same content!)

#### Step 6: Upload New Content to v2.0
```http
POST /content/upload
Content-Type: multipart/form-data

file: manual-v2.pdf (content: "This is version 2.0 with updates")
sysObjectId: 2
storeInDatabase: true
```
Response: Content ID = 12 (new content)

You could also update the existing content (ID 11):
```http
DELETE /content/11
POST /content/upload
...
```

#### Step 7: Verify Copy-on-Write Worked
```http
GET /content/10/download
```
Response: Still downloads "This is version 1.0" ✓

```http
GET /content/12/download
```
Response: Downloads "This is version 2.0 with updates" ✓

**Result:** v1.0 and v2.0 now have independent content!

## Complete Examples

### Example 1: Version Document with Multiple Content Files

```bash
# 1. Create document
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "API Documentation",
    "documentType": "MANUAL",
    "author": "Tech Team"
  }'
# Response: {"id": 1, "version": "1.0", ...}

# 2. Upload multiple files
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@overview.md" \
  -F "sysObjectId=1" \
  -F "storeInDatabase=true"

curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@api-reference.pdf" \
  -F "sysObjectId=1" \
  -F "fileStoreId=1" \
  -F "storeInDatabase=false"

# 3. Create new version
curl -X POST http://localhost:8080/api/documents/1/versions/major
# Response: {"id": 2, "version": "2.0", ...}

# 4. Check both versions have content
curl http://localhost:8080/api/content/by-sysobject/1
curl http://localhost:8080/api/content/by-sysobject/2
# Both return 2 content items (cloned)

# 5. Update one file in v2.0
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@overview-v2.md" \
  -F "sysObjectId=2" \
  -F "storeInDatabase=true"

# 6. Verify v1.0 unchanged
curl http://localhost:8080/api/content/by-sysobject/1
# Still shows original content
```

### Example 2: File Store Copy-on-Write

```bash
# 1. Create document with file store content
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"name": "Large Media", "documentType": "OTHER"}'

curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@large-video.mp4" \
  -F "sysObjectId=1" \
  -F "fileStoreId=1" \
  -F "storeInDatabase=false"
# Response: {"id": 10, "storageType": "FILE_STORE", "storagePath": "uuid1.mp4"}

# 2. Create new version
curl -X POST http://localhost:8080/api/documents/1/versions/major
# Response: {"id": 2, "version": "2.0"}

# 3. Check content cloned with same storage path
curl http://localhost:8080/api/content/by-sysobject/2
# Response: {"id": 11, "storagePath": "uuid1.mp4"} - SAME FILE!

# 4. To update v2.0's video independently, upload new file
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@updated-video.mp4" \
  -F "sysObjectId=2" \
  -F "fileStoreId=1" \
  -F "storeInDatabase=false"
# Response: {"id": 12, "storagePath": "uuid2.mp4"} - NEW FILE!

# 5. Delete old v2.0 content if needed
curl -X DELETE http://localhost:8080/api/content/11

# Result: v1.0 still references uuid1.mp4, v2.0 references uuid2.mp4
```

### Example 3: Mixed Storage Types

```bash
# Create document with both database and file store content
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"name": "Mixed Content Doc", "documentType": "REPORT"}'

# Database content (small text)
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@notes.txt" \
  -F "sysObjectId=1" \
  -F "storeInDatabase=true"

# File store content (large binary)
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@dataset.csv" \
  -F "sysObjectId=1" \
  -F "fileStoreId=1" \
  -F "storeInDatabase=false"

# Version and modify
curl -X POST http://localhost:8080/api/documents/1/versions/major
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@notes-updated.txt" \
  -F "sysObjectId=2" \
  -F "storeInDatabase=true"

# Both storage types support copy-on-write independently
```

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-12-05T07:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": {
    "name": "Name is required",
    "documentType": "Document type must be specified"
  }
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-12-05T07:40:00",
  "status": 404,
  "error": "Not Found",
  "message": "Document not found with ID: 999"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-12-05T07:40:00",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot delete file store: it contains content"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-12-05T07:40:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error processing request"
}
```

## Best Practices

1. **Version Before Major Changes**: Always create a new version before making significant content changes
2. **Use Appropriate Storage**: Use database storage for small files (<1MB), file store for large files
3. **Check Version History**: Use `/versions/history` to track changes over time
4. **Content Cleanup**: Delete unused content from old versions to save space
5. **Atomic Updates**: When updating multiple content files in a version, do it in a single transaction if possible

## Notes on Copy-on-Write

- **Database Content**: Automatically independent when modified (byte array is copied in memory)
- **File Store Content**: Shares physical file initially. Manual intervention required:
  - Upload new content with different storage path, OR
  - Use move operations to change storage location
- **Efficiency**: Shared storage means versions take minimal extra space until content diverges
- **Safety**: Original versions are never affected by changes to new versions
