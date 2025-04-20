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

The application will be available at http://localhost:8080 with the following access points:
- Web UI: http://localhost:8080
- API: http://localhost:8080/api/*
- H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:file:./docmgmt_db)

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

