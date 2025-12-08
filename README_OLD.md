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

# Document Management System

A comprehensive document management system with versioning support, flexible storage options, and a modern web UI.

## Overview

This application provides a complete solution for managing documents and their associated content. It features both a RESTful API for integration with other systems and a user-friendly Vaadin-based web interface for direct user interaction.

Key capabilities include document versioning, configurable storage locations (database or filesystem), content management, and robust search functionality.

## Features

### Document Management
- Create, read, update, and delete documents
- Categorize documents by type (ARTICLE, REPORT, CONTRACT, etc.)
- Tag-based organization
- Keyword search
- Document versioning with major/minor version control
- Version history tracking

### Storage Management
- Configurable file storage locations
- Storage space monitoring
- Active/inactive storage status tracking
- Automatic directory creation
- Storage location validation

### Content Management
- Upload and download files
- Multiple storage options (database or file system)
- Content type detection
- File size validation
- Move content between storage locations
- Associate content with documents

### Web Interface
- Responsive Vaadin-based UI
- Document management view with filtering
- File store configuration
- Content upload/download interface
- Integrated navigation

## Technology Stack

- Java 21
- Spring Boot 3.2.3
- Spring Data JPA
- H2 Database (embedded)
- Vaadin 24 (UI framework)
- Maven (build tool)

## Setup

### Prerequisites

- Java 21 or newer
- Maven 3.6 or newer

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/document-management.git
   cd document-management
   ```

2. Build the project:
   ```
   mvn clean install
   ```

## Running the Application

### Development Mode

Run the application with the Spring Boot Maven plugin:

```
mvn spring-boot:run
```

The application will be available at http://localhost:8082/docmgmt with the following access points:
- Web UI: http://localhost:8082/docmgmt
- API: http://localhost:8082/docmgmt/api/*
- **Swagger UI**: http://localhost:8082/docmgmt/api/swagger-ui.html
- H2 Console: http://localhost:8082/docmgmt/h2-console (JDBC URL: jdbc:h2:file:./docmgmt_db)

### Production Mode

1. Build the project with the production profile:
   ```
   mvn clean package -Pproduction
   ```

2. Run the generated JAR file:
   ```
   java -jar target/document-management-0.0.1-SNAPSHOT.jar
   ```

## API Documentation

### Interactive API Documentation (Swagger UI)

**🚀 Quick Start**: Access the interactive API documentation at:

```
http://localhost:8082/docmgmt/api/swagger-ui.html
```

The Swagger UI provides:
- **Browse all endpoints** with detailed descriptions
- **Try it out** feature to test APIs directly from browser
- **Request/response schemas** with validation rules
- **Example values** for quick testing
- **Download OpenAPI specification** in JSON or YAML format

For detailed Swagger UI documentation, see:
- **Quick Start**: [SWAGGER_QUICKSTART.md](SWAGGER_QUICKSTART.md)
- **Complete Guide**: [docs/OPENAPI_SWAGGER.md](docs/OPENAPI_SWAGGER.md)

### Document Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET    | /api/documents | Get all documents |
| GET    | /api/documents/latest | Get latest versions of all documents |
| GET    | /api/documents/{id} | Get document by ID |
| POST   | /api/documents | Create a new document |
| PUT    | /api/documents/{id} | Update a document |
| DELETE | /api/documents/{id} | Delete a document |
| GET    | /api/documents/by-type/{documentType} | Find documents by type |
| GET    | /api/documents/by-author/{author} | Find documents by author |
| GET    | /api/documents/by-tag/{tag} | Find documents containing a tag |
| GET    | /api/documents/search?keywords={keywords} | Search documents by keywords |
| GET    | /api/documents/{id}/versions | Get all versions of a document |
| POST   | /api/documents/{id}/major-version | Create major version of a document |
| POST   | /api/documents/{id}/minor-version | Create minor version of a document |

### FileStore Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET    | /api/filestores | Get all file stores |
| GET    | /api/filestores/active | Get all active file stores |
| GET    | /api/filestores/{id} | Get file store by ID |
| POST   | /api/filestores | Create a new file store |
| PUT    | /api/filestores/{id} | Update a file store |
| DELETE | /api/filestores/{id} | Delete a file store |
| PUT    | /api/filestores/{id}/activate | Activate a file store |
| PUT    | /api/filestores/{id}/deactivate | Deactivate a file store |
| GET    | /api/filestores/{id}/space | Get available space in file store |
| GET    | /api/filestores/{id}/space/check?requiredBytes={bytes} | Check if file store has enough space |

### Content Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET    | /api/content/{id} | Get content metadata by ID |
| GET    | /api/content/by-sysobject/{sysObjectId} | List content for a document |
| POST   | /api/content/upload | Upload content |
| GET    | /api/content/{id}/download | Download content |
| DELETE | /api/content/{id} | Delete content |
| PUT    | /api/content/{id}/move-to-filestore?fileStoreId={fileStoreId} | Move content to file store |
| PUT    | /api/content/{id}/move-to-database | Move content to database |

## Examples

### Creating a Document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Annual Report 2025",
    "documentType": "REPORT",
    "description": "Annual financial report for FY 2025",
    "author": "Finance Department",
    "tags": ["finance", "annual", "report"],
    "keywords": "financial report annual 2025"
  }'
```

### Creating a File Store

```bash
curl -X POST http://localhost:8080/api/filestores \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Primary Storage",
    "rootPath": "/path/to/storage",
    "status": "ACTIVE"
  }'
```

### Uploading Content to a Document

```bash
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@/path/to/local/file.pdf" \
  -F "sysObjectId=1" \
  -F "storeInDatabase=true"
```

Alternatively, to store in a file store:

```bash
curl -X POST http://localhost:8080/api/content/upload \
  -F "file=@/path/to/local/file.pdf" \
  -F "sysObjectId=1" \
  -F "storeInDatabase=false" \
  -F "fileStoreId=1"
```

### Downloading Content

```bash
curl -X GET http://localhost:8080/api/content/1/download \
  -o downloaded_file.pdf
```

### Creating a New Document Version

```bash
# Create minor version (e.g., 1.0 → 1.1)
curl -X POST http://localhost:8080/api/documents/1/minor-version

# Create major version (e.g., 1.1 → 2.0)
curl -X POST http://localhost:8080/api/documents/1/major-version
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

