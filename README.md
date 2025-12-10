# Document Management System

A comprehensive document management system with versioning support, flexible storage options, full-text search, and a modern web UI.

## Overview

This application provides a complete solution for managing documents and their associated content. It features:
- RESTful API for integration
- Vaadin-based web UI for direct interaction  
- Python CLI for command-line operations
- Full-text search with Apache Lucene
- Content transformations (PDF-to-text)
- Major/minor versioning
- Flexible storage (database or file system)

## Quick Start

```bash
# Build and run
mvn spring-boot:run

# Access the application
Web UI:    http://localhost:8082/docmgmt
Swagger:   http://localhost:8082/docmgmt/api/swagger-ui.html
H2 Console: http://localhost:8082/docmgmt/h2-console
```

## Features

### Document Management
- Create, read, update, and delete documents
- Document types: ARTICLE, MANUAL, REPORT, SPREADSHEET, PRESENTATION, IMAGE, VIDEO, AUDIO, OTHER
- Tag-based organization and keyword search
- Copy-on-write versioning (major/minor)
- Complete version history tracking

### Content Management
- Upload and download files
- Multiple storage options (database or file system)
- Content type detection and validation
- Move content between storage locations
- Primary/secondary renditions support
- PDF-to-text transformation

### Search
- Full-text search with Apache Lucene
- Search across all fields (name, description, keywords, tags, content)
- Field-specific queries
- Boolean operators (AND, OR)
- Phrase and wildcard search
- Automatic indexing of text content

### Storage Management
- Configurable file storage locations
- Storage space monitoring
- Active/inactive storage tracking
- Automatic directory creation

### Web Interface
- Responsive Vaadin 24 UI
- Document management with filtering
- File store configuration
- Content upload/download
- Search interface

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.2.3, Spring Data JPA
- **Database**: H2 (embedded, file-based)
- **Search**: Apache Lucene 9.9.1
- **PDF Processing**: Apache PDFBox 3.0.1
- **UI**: Vaadin 24.3.5
- **API Docs**: SpringDoc OpenAPI 3 (Swagger)
- **Build**: Maven 3.6+

## Installation

### Prerequisites

- Java 21 or newer
- Maven 3.6 or newer

### Build

```bash
git clone <repository-url>
cd document-management
mvn clean install
```

## Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

Application endpoints:
- **Web UI**: http://localhost:8082/docmgmt
- **Swagger UI**: http://localhost:8082/docmgmt/api/swagger-ui.html
- **API**: http://localhost:8082/docmgmt/api/\*
- **H2 Console**: http://localhost:8082/docmgmt/h2-console
  - JDBC URL: `jdbc:h2:file:./docmgmt_db`
  - Username: `sa`
  - Password: `password`

### Production Mode

```bash
mvn clean package -Pproduction
java -jar target/document-management-0.0.1-SNAPSHOT.jar
```

### macOS Native Application

Build a native macOS application with DMG installer:

```bash
./build-mac-dmg.sh
```

This creates a self-contained macOS app with:
- Native .app bundle
- Bundled Java runtime (no separate Java installation needed)
- DMG installer for easy distribution
- Automatic browser launch
- Data storage in `~/Library/Application Support/DocMgmt/`

**Output**: `target/dist/DocMgmt-1.0.0.dmg` (~250-350 MB)

**Documentation**:
- Quick Start: [QUICK_START_MAC.md](QUICK_START_MAC.md)
- Complete Guide: [BUILD_MAC.md](BUILD_MAC.md)
- Permissions & Security: [MACOS_PERMISSIONS.md](MACOS_PERMISSIONS.md)

## Command Line Interface

### Overview

The Document Management CLI (`docmgmt-cli.py`) provides command-line access to the REST API.

### Quick Start

```bash
# Install dependencies
pip install requests

# Make executable
chmod +x docmgmt-cli.py

# Get help
./docmgmt-cli.py --help
```

### Basic Usage

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

### Available Commands

- **documents** - Create, read, update, delete documents and manage versions
- **content** - Upload, download, and manage content files
- **search** - Full-text search (simple and field-specific queries)
- **filestores** - Manage file storage locations

### Complete CLI Documentation

For comprehensive CLI documentation including all commands, workflows, and scripting examples:

**See: [CLI_GUIDE.md](CLI_GUIDE.md)**

### Python Client Library

For programmatic access in Python applications:

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

**See: [clients/python/README.md](clients/python/README.md)**

## API Documentation

### Interactive Documentation (Swagger UI)

Access the complete interactive API documentation at:

```
http://localhost:8082/docmgmt/api/swagger-ui.html
```

Features:
- Browse all endpoints with descriptions
- Test APIs directly from browser
- View request/response schemas
- Example values and validation rules
- Download OpenAPI specification

**Documentation:**
- Quick Start: [SWAGGER_QUICKSTART.md](SWAGGER_QUICKSTART.md)
- Complete Guide: [docs/OPENAPI_SWAGGER.md](docs/OPENAPI_SWAGGER.md)

### Key API Endpoints

#### Documents

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/documents | Get all documents |
| GET | /api/documents/latest | Get latest versions |
| GET | /api/documents/{id} | Get document by ID |
| POST | /api/documents | Create document |
| PUT | /api/documents/{id} | Update document |
| DELETE | /api/documents/{id} | Delete document |
| POST | /api/documents/{id}/versions/major | Create major version |
| POST | /api/documents/{id}/versions/minor | Create minor version |
| GET | /api/documents/{id}/versions/history | Get version history |
| GET | /api/documents/by-type/{type} | Find by type |
| GET | /api/documents/by-tag/{tag} | Find by tag |
| GET | /api/documents/search?keywords={keywords} | Search by keywords |

#### Content

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/content/{id} | Get content metadata |
| GET | /api/content/by-sysobject/{id} | List content for document |
| POST | /api/content/upload | Upload content |
| GET | /api/content/{id}/download | Download content |
| DELETE | /api/content/{id} | Delete content |
| PUT | /api/content/{id}/move-to-filestore | Move to file store |
| PUT | /api/content/{id}/move-to-database | Move to database |

#### Search

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/search?q={query}&limit={n} | Simple text search |
| POST | /api/search/fields | Field-specific search |
| POST | /api/search/rebuild | Rebuild search index |
| GET | /api/search/stats | Get index statistics |

#### File Stores

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/filestores | Get all file stores |
| GET | /api/filestores/active | Get active file stores |
| GET | /api/filestores/{id} | Get file store by ID |
| POST | /api/filestores | Create file store |
| PUT | /api/filestores/{id} | Update file store |
| DELETE | /api/filestores/{id} | Delete file store |
| PUT | /api/filestores/{id}/activate | Activate file store |
| PUT | /api/filestores/{id}/deactivate | Deactivate file store |

## Examples

### Creating a Document

```bash
curl -X POST http://localhost:8082/docmgmt/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Annual Report 2025",
    "documentType": "REPORT",
    "description": "Annual financial report",
    "keywords": "financial report annual 2025",
    "tags": ["finance", "annual", "report"]
  }'
```

### Uploading Content

```bash
# Store in database
curl -X POST http://localhost:8082/docmgmt/api/content/upload \
  -F "file=@document.pdf" \
  -F "sysObjectId=1" \
  -F "storeInDatabase=true"

# Store in file system
curl -X POST http://localhost:8082/docmgmt/api/content/upload \
  -F "file=@largefile.mp4" \
  -F "sysObjectId=1" \
  -F "storeInDatabase=false" \
  -F "fileStoreId=1"
```

### Searching Documents

```bash
# Simple search
curl "http://localhost:8082/docmgmt/api/search?q=spring&limit=10"

# Field-specific search
curl -X POST http://localhost:8082/docmgmt/api/search/fields \
  -H "Content-Type: application/json" \
  -d '{"name": "tutorial", "tags": "java"}' \
  -G --data-urlencode "operator=AND"
```

### Creating Document Versions

```bash
# Minor version (1.0 → 1.1)
curl -X POST http://localhost:8082/docmgmt/api/documents/1/versions/minor

# Major version (1.1 → 2.0)
curl -X POST http://localhost:8082/docmgmt/api/documents/1/versions/major
```

## Additional Documentation

- **[CLI_GUIDE.md](CLI_GUIDE.md)** - Complete CLI documentation
- **[SWAGGER_QUICKSTART.md](SWAGGER_QUICKSTART.md)** - Quick Swagger UI guide
- **[docs/OPENAPI_SWAGGER.md](docs/OPENAPI_SWAGGER.md)** - Complete API documentation
- **[docs/CONTENT_RENDITIONS.md](docs/CONTENT_RENDITIONS.md)** - Content transformations guide
- **[clients/python/README.md](clients/python/README.md)** - Python client library
- **[clients/java/README.md](clients/java/README.md)** - Java client examples

## Configuration

Key configuration in `src/main/resources/application.properties`:

```properties
# Server
server.port=8082
server.servlet.context-path=/docmgmt

# Database
spring.datasource.url=jdbc:h2:file:./docmgmt_db
spring.datasource.username=sa
spring.datasource.password=password

# File Upload
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Swagger UI
springdoc.swagger-ui.path=/api/swagger-ui.html
springdoc.api-docs.path=/api/v3/api-docs
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
