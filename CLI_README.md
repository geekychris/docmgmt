# Document Import CLI

A standalone command-line tool for bulk importing documents into the Document Management System via REST API.

## Overview

The `StandaloneDocumentImportCli` is a lightweight CLI tool that:
- Does NOT require database access
- Connects to the document management server via REST API
- Can be run independently from the main application
- Supports folder structure creation
- Handles multiple file types
- Can store content in database or file stores

## Prerequisites

1. The document management server must be running and accessible
2. Java 21 or higher installed
3. The compiled JAR file or classpath with required dependencies

## Building

Build the application first:

```bash
mvn clean package -DskipTests
```

## Usage

### Basic Usage

```bash
java -cp target/document-management-0.0.1-SNAPSHOT.jar \
  com.docmgmt.cli.StandaloneDocumentImportCli \
  --api-base-url=http://localhost:8080 \
  --root-dir=/path/to/documents
```

### Full Options

```bash
java -cp target/document-management-0.0.1-SNAPSHOT.jar \
  com.docmgmt.cli.StandaloneDocumentImportCli \
  --api-base-url=http://localhost:8080 \
  --root-dir=/path/to/documents \
  --file-types=pdf,docx,txt,doc,rtf,md \
  --filestore-id=1 \
  --create-folders=true
```

## Configuration Options

| Option | Required | Default | Description |
|--------|----------|---------|-------------|
| `--api-base-url` | No | `http://localhost:8080` | Base URL of the document management server |
| `--root-dir` | Yes | N/A | Root directory containing documents to import |
| `--file-types` | No | `pdf,docx,txt,doc,rtf,md` | Comma-separated list of file extensions to import |
| `--filestore-id` | No | N/A | ID of file store to use (if not specified, uses database storage) |
| `--create-folders` | No | `true` | Whether to create folder structure matching directory structure |

## Examples

### Import PDFs from a directory (database storage)

```bash
java -cp target/document-management-0.0.1-SNAPSHOT.jar \
  com.docmgmt.cli.StandaloneDocumentImportCli \
  --root-dir=/Users/john/Documents/reports \
  --file-types=pdf
```

### Import multiple file types to a file store

```bash
java -cp target/document-management-0.0.1-SNAPSHOT.jar \
  com.docmgmt.cli.StandaloneDocumentImportCli \
  --root-dir=/Users/john/Documents \
  --file-types=pdf,docx,txt \
  --filestore-id=1
```

### Import without creating folders

```bash
java -cp target/document-management-0.0.1-SNAPSHOT.jar \
  com.docmgmt.cli.StandaloneDocumentImportCli \
  --root-dir=/Users/john/Documents/flat \
  --create-folders=false
```

### Connect to remote server

```bash
java -cp target/document-management-0.0.1-SNAPSHOT.jar \
  com.docmgmt.cli.StandaloneDocumentImportCli \
  --api-base-url=http://docserver.example.com:8080 \
  --root-dir=/path/to/documents
```

## How It Works

1. **Validation**: CLI validates API connectivity and configuration
2. **Directory Traversal**: Walks the directory tree looking for matching file types
3. **Folder Creation** (optional): Creates folder structure via `/api/folders` endpoint
4. **Document Creation**: Creates document metadata via `/api/documents` endpoint
5. **Content Upload**: Uploads file content via `/api/content/upload` endpoint
6. **Statistics**: Displays import results

## Output

The CLI provides progress information and statistics:

```
================================================================================
Standalone Document Import CLI
================================================================================
Parsing command line arguments...
Validating configuration...
Connected to API server at: http://localhost:8080
No file store specified - content will be stored in database

Configuration:
  API Base URL: http://localhost:8080
  Root Directory: /path/to/docs
  File Types: [pdf, docx, txt]
  File Store ID: N/A (database storage)
  Create Folders: true

Starting import from: /path/to/docs

Processing: /path/to/docs/report.pdf
  ✓ Created folder: /reports
  ✓ Created document ID: 123

================================================================================
Import Statistics
================================================================================
  Folders Created:     5
  Documents Created:   42
  Files Uploaded:      42
  Errors:              0
================================================================================
Import completed successfully!
```

## Troubleshooting

### Connection refused
- Ensure the document management server is running
- Check the API base URL is correct
- Verify network connectivity

### Authentication errors
- This CLI currently does not support authentication
- Ensure the API endpoints are accessible without authentication
- Future versions may add authentication support

### File upload failures
- Check file permissions
- Verify file store (if specified) exists and is accessible
- Check server logs for detailed error messages

## Architecture

The CLI is completely independent of the main application's database layer:
- Uses `RestTemplate` for HTTP communication
- Only requires DTO classes and model enums
- No Spring Data repositories or JPA dependencies needed at runtime
- Can be packaged separately if needed

## REST API Endpoints Used

- `GET /actuator/health` - Server connectivity check
- `GET /api/filestores/{id}` - File store validation
- `GET /api/folders/by-path?path={path}` - Find existing folders
- `POST /api/folders` - Create folders
- `POST /api/documents` - Create documents
- `PUT /api/folders/{folderId}/items/{itemId}` - Add documents to folders
- `POST /api/content/upload` - Upload file content

## Future Enhancements

- Authentication support (OAuth2, Basic Auth, API keys)
- Resume capability for interrupted imports
- Parallel/concurrent uploads
- Dry-run mode
- Progress bar/percentage indicator
- Incremental imports (skip existing files)
- Configuration file support
