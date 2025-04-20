# Document Management API Test Commands

This file contains a series of curl commands that demonstrate the document management workflow:
1. Creating a filestore
2. Creating a document
3. Uploading content to a content object
4. Linking a content object to a document
5. Versioning a document

## 1. Create a Filestore

```bash
# Create a new filestore called "test-filestore"
# This is the top-level container for documents and content
curl -X POST http://localhost:8080/api/filestores \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-filestore",
    "description": "A test filestore for document management"
  }'

# Save the filestore ID for later use
# FILESTORE_ID=$(curl -s http://localhost:8080/api/filestores?name=test-filestore | jq -r '.items[0].id')
```

## 2. Create a Document

```bash
# Create a new document in the filestore
# A document is a logical container that can have multiple versions
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "filestoreId": "'${FILESTORE_ID}'",
    "name": "sample-document",
    "documentType": "ARTICLE",
    "metadata": {
      "author": "John Doe",
      "department": "Engineering"
    }
  }'

# Save the document ID for later use
# DOCUMENT_ID=$(curl -s "http://localhost:8080/api/documents?filestoreId=${FILESTORE_ID}&name=sample-document" | jq -r '.items[0].id')
```

## 3. Create and Upload Content to a Content Object

```bash
# Create a content object
# A content object holds the actual binary data of a document
curl -X POST http://localhost:8080/api/contents \
  -H "Content-Type: application/json" \
  -d '{
    "filestoreId": "'${FILESTORE_ID}'",
    "name": "sample-content",
    "contentType": "text/markdown"
  }'

# Save the content ID for later use
# CONTENT_ID=$(curl -s "http://localhost:8080/api/contents?filestoreId=${FILESTORE_ID}&name=sample-content" | jq -r '.items[0].id')

# Upload binary data to the content object
curl -X PUT http://localhost:8080/api/contents/${CONTENT_ID}/data \
  -H "Content-Type: text/markdown" \
  --data-binary @- << EOF
# Sample Document

This is a sample document content.

## Introduction

This document demonstrates the document management workflow.

## Conclusion

This is just an example of document content.
EOF
```

## 4. Link Content Object to Document

```bash
# Link the content object to the document
# This creates the first version of the document
curl -X POST http://localhost:8080/api/document-versions \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'${DOCUMENT_ID}'",
    "contentId": "'${CONTENT_ID}'",
    "versionNumber": 1,
    "metadata": {
      "comment": "Initial version"
    }
  }'
```

## 5. Create a New Version of the Document

```bash
# First, create a new content object for the new version
curl -X POST http://localhost:8080/api/contents \
  -H "Content-Type: application/json" \
  -d '{
    "filestoreId": "'${FILESTORE_ID}'",
    "name": "sample-content-v2",
    "contentType": "text/markdown"
  }'

# Save the new content ID
# CONTENT_ID_V2=$(curl -s "http://localhost:8080/api/contents?filestoreId=${FILESTORE_ID}&name=sample-content-v2" | jq -r '.items[0].id')

# Upload data to the new content object
curl -X PUT http://localhost:8080/api/contents/${CONTENT_ID_V2}/data \
  -H "Content-Type: text/markdown" \
  --data-binary @- << EOF
# Sample Document (Updated)

This is a sample document content with updates.

## Introduction

This document demonstrates the document management workflow.

## New Section

This is a new section added in version 2.

## Conclusion

This is just an example of document content.
EOF

# Create a new version of the document using the new content
curl -X POST http://localhost:8080/api/document-versions \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'${DOCUMENT_ID}'",
    "contentId": "'${CONTENT_ID_V2}'",
    "versionNumber": 2,
    "metadata": {
      "comment": "Added new section",
      "reviewer": "Jane Smith"
    }
  }'
```

## Bonus: Retrieving Document Information

```bash
# Get document details
curl -X GET http://localhost:8080/api/documents/${DOCUMENT_ID}

# List all versions of a document
curl -X GET http://localhost:8080/api/document-versions?documentId=${DOCUMENT_ID}

# Download the content of a specific document version
curl -X GET http://localhost:8080/api/contents/${CONTENT_ID}/data -o document_content.md
```

## Complete Workflow Script

To run all these commands in sequence, you can use the script below:

```bash
#!/bin/bash

# Create filestore
echo "Creating filestore..."
FILESTORE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/filestores \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-filestore",
    "description": "A test filestore for document management"
  }')
FILESTORE_ID=$(echo $FILESTORE_RESPONSE | jq -r '.id')
echo "Filestore created with ID: $FILESTORE_ID"

# Create document
echo "Creating document..."
DOCUMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "filestoreId": "'$FILESTORE_ID'",
    "name": "sample-document",
    "documentType": "ARTICLE",
    "metadata": {
      "author": "John Doe",
      "department": "Engineering"
    }
  }')
DOCUMENT_ID=$(echo $DOCUMENT_RESPONSE | jq -r '.id')
echo "Document created with ID: $DOCUMENT_ID"

# Create content
echo "Creating content object..."
CONTENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/contents \
  -H "Content-Type: application/json" \
  -d '{
    "filestoreId": "'$FILESTORE_ID'",
    "name": "sample-content",
    "contentType": "text/markdown"
  }')
CONTENT_ID=$(echo $CONTENT_RESPONSE | jq -r '.id')
echo "Content created with ID: $CONTENT_ID"

# Upload content data
echo "Uploading content data..."
curl -s -X PUT http://localhost:8080/api/contents/$CONTENT_ID/data \
  -H "Content-Type: text/markdown" \
  --data-binary @- << EOF
# Sample Document

This is a sample document content.

## Introduction

This document demonstrates the document management workflow.

## Conclusion

This is just an example of document content.
EOF
echo "Content data uploaded"

# Create first document version
echo "Creating first document version..."
curl -s -X POST http://localhost:8080/api/document-versions \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'$DOCUMENT_ID'",
    "contentId": "'$CONTENT_ID'",
    "versionNumber": 1,
    "metadata": {
      "comment": "Initial version"
    }
  }'
echo "First document version created"

# Create new content for version 2
echo "Creating content for version 2..."
CONTENT_V2_RESPONSE=$(curl -s -X POST http://localhost:8080/api/contents \
  -H "Content-Type: application/json" \
  -d '{
    "filestoreId": "'$FILESTORE_ID'",
    "name": "sample-content-v2",
    "contentType": "text/markdown"
  }')
CONTENT_ID_V2=$(echo $CONTENT_V2_RESPONSE | jq -r '.id')
echo "Content for version 2 created with ID: $CONTENT_ID_V2"

# Upload content data for version 2
echo "Uploading content data for version 2..."
curl -s -X PUT http://localhost:8080/api/contents/$CONTENT_ID_V2/data \
  -H "Content-Type: text/markdown" \
  --data-binary @- << EOF
# Sample Document (Updated)

This is a sample document content with updates.

## Introduction

This document demonstrates the document management workflow.

## New Section

This is a new section added in version 2.

## Conclusion

This is just an example of document content.
EOF
echo "Content data for version 2 uploaded"

# Create second document version
echo "Creating second document version..."
curl -s -X POST http://localhost:8080/api/document-versions \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'$DOCUMENT_ID'",
    "contentId": "'$CONTENT_ID_V2'",
    "versionNumber": 2,
    "metadata": {
      "comment": "Added new section",
      "reviewer": "Jane Smith"
    }
  }'
echo "Second document version created"

echo "Document management workflow completed successfully!"
```

