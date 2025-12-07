# OpenAPI / Swagger Documentation

This document describes the OpenAPI/Swagger integration for the Document Management System REST API.

## Overview

The application uses **SpringDoc OpenAPI 3** to automatically generate interactive API documentation and provide a Swagger UI for testing REST endpoints.

## Features

- **Interactive API Documentation**: Browse all REST endpoints with detailed descriptions
- **Try It Out**: Test API endpoints directly from the browser
- **Schema Definitions**: View request/response models with validation rules
- **Example Values**: Pre-populated example requests for quick testing
- **Download OpenAPI Spec**: Export the API specification in JSON or YAML format

## Accessing the Documentation

### Swagger UI (Interactive Documentation)

```
http://localhost:8082/docmgmt/api/swagger-ui.html
```

The Swagger UI provides an interactive interface where you can:
- Browse all API endpoints organized by tags
- View detailed operation descriptions
- Test endpoints with the "Try it out" button
- See request/response schemas and examples
- View HTTP status codes and their meanings

### OpenAPI JSON Specification

```
http://localhost:8082/docmgmt/api/v3/api-docs
```

This endpoint returns the complete OpenAPI 3.0 specification in JSON format.

### OpenAPI YAML Specification

```
http://localhost:8082/docmgmt/api/v3/api-docs.yaml
```

Alternative format for the OpenAPI specification.

## API Organization

The REST API is organized into the following tags:

### 1. **Documents**
Document management operations including creation, versioning, and queries.

**Key Endpoints:**
- `GET /api/documents` - Get all documents
- `GET /api/documents/latest` - Get latest versions only
- `GET /api/documents/{id}` - Get document by ID
- `POST /api/documents` - Create new document
- `PUT /api/documents/{id}` - Update document
- `DELETE /api/documents/{id}` - Delete document
- `GET /api/documents/by-type/{documentType}` - Find by type
- `GET /api/documents/by-tag/{tag}` - Find by tag
- `GET /api/documents/search?keywords=` - Search by keywords
- `POST /api/documents/{id}/versions/major` - Create major version
- `POST /api/documents/{id}/versions/minor` - Create minor version
- `GET /api/documents/{id}/versions/history` - Get version history

### 2. **Search**
Full-text search operations using Apache Lucene.

**Key Endpoints:**
- `GET /api/search?q={query}&limit={limit}` - Simple text search
- `POST /api/search/fields` - Field-specific search with AND/OR operators
- `POST /api/search/rebuild` - Rebuild search index
- `GET /api/search/stats` - Get index statistics

**Search Features:**
- Searches across all fields: name, description, keywords, tags, content
- Supports Lucene query syntax
- Wildcards: `spring*`, `?pring`
- Phrase queries: `"spring framework"`
- Boolean operators: `spring AND java`, `python OR ruby`
- Field-specific: `name:tutorial`, `tags:java`

### 3. **Content**
Content management operations including upload, download, and storage migration.

**Key Endpoints:**
- `GET /api/content/{id}` - Get content metadata
- `GET /api/content/by-sysobject/{sysObjectId}` - List content for document
- `POST /api/content/upload` - Upload content (multipart/form-data)
- `GET /api/content/{id}/download` - Download content bytes
- `DELETE /api/content/{id}` - Delete content
- `PUT /api/content/{id}/move-to-filestore?fileStoreId={id}` - Move to file store
- `PUT /api/content/{id}/move-to-database` - Move to database

### 4. **File Stores**
File store management for external content storage.

**Key Endpoints:**
- `GET /api/filestores` - Get all file stores
- `GET /api/filestores/active` - Get active file stores
- `GET /api/filestores/{id}` - Get file store by ID
- `POST /api/filestores` - Create file store
- `PUT /api/filestores/{id}` - Update file store
- `DELETE /api/filestores/{id}` - Delete file store
- `PUT /api/filestores/{id}/activate` - Activate file store
- `PUT /api/filestores/{id}/deactivate` - Deactivate file store
- `GET /api/filestores/{id}/space` - Get available space
- `GET /api/filestores/{id}/space/check?requiredBytes=` - Check space

## Using Swagger UI

### Step 1: Start the Application

```bash
mvn spring-boot:run
```

### Step 2: Open Swagger UI

Navigate to: http://localhost:8082/docmgmt/api/swagger-ui.html

### Step 3: Test an Endpoint

1. **Expand a tag** (e.g., "Documents")
2. **Click an operation** (e.g., "POST /api/documents")
3. **Click "Try it out"**
4. **Fill in the request body** (or use the example)
5. **Click "Execute"**
6. **View the response** with status code, headers, and body

## Example API Calls

### Create a Document

**Request:**
```http
POST /docmgmt/api/documents
Content-Type: application/json

{
  "name": "Spring Boot Guide",
  "documentType": "MANUAL",
  "description": "Comprehensive Spring Boot development guide",
  "keywords": "spring boot java framework",
  "tags": ["spring", "java", "backend"]
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Spring Boot Guide",
  "documentType": "MANUAL",
  "description": "Comprehensive Spring Boot development guide",
  "keywords": "spring boot java framework",
  "tags": ["spring", "java", "backend"],
  "versionLabel": "1.0",
  "createdDate": "2025-12-07T19:30:00",
  "modifiedDate": "2025-12-07T19:30:00"
}
```

### Upload Content

**Request:**
```http
POST /docmgmt/api/content/upload
Content-Type: multipart/form-data

file: [binary data]
sysObjectId: 1
storeInDatabase: true
```

### Search Documents

**Simple Search:**
```http
GET /docmgmt/api/search?q=spring&limit=10
```

**Field-Specific Search:**
```http
POST /docmgmt/api/search/fields?operator=AND&limit=10
Content-Type: application/json

{
  "name": "tutorial",
  "tags": "java"
}
```

## Configuration

### Customizing Swagger UI

Edit `src/main/resources/application.properties`:

```properties
# Change Swagger UI path
springdoc.swagger-ui.path=/api/swagger-ui.html

# Change API docs path
springdoc.api-docs.path=/api/api-docs

# Sort operations alphabetically
springdoc.swagger-ui.operationsSorter=alpha

# Sort tags alphabetically
springdoc.swagger-ui.tagsSorter=alpha

# Default expansion behavior (none, list, full)
springdoc.swagger-ui.doc-expansion=none
```

### Excluding Endpoints from Documentation

Add to a controller or method:

```java
@Hidden  // Hides from OpenAPI documentation
@GetMapping("/internal-endpoint")
public ResponseEntity<String> internalEndpoint() {
    // ...
}
```

## OpenAPI Annotations Reference

### Controller-Level Annotations

```java
@Tag(name = "Documents", description = "Document management operations")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    // ...
}
```

### Operation-Level Annotations

```java
@Operation(
    summary = "Create document",
    description = "Creates a new document with initial version 1.0"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Document created"),
    @ApiResponse(responseCode = "400", description = "Invalid input")
})
@PostMapping
public ResponseEntity<DocumentDTO> create(@RequestBody DocumentDTO dto) {
    // ...
}
```

### Parameter Annotations

```java
@Parameter(description = "Document ID", required = true, example = "1")
@PathVariable Long id
```

## Downloading OpenAPI Specification

### JSON Format
```bash
curl http://localhost:8082/docmgmt/api/v3/api-docs > openapi.json
```

### YAML Format
```bash
curl http://localhost:8082/docmgmt/api/v3/api-docs.yaml > openapi.yaml
```

## Generating Client SDKs

Use the OpenAPI specification to generate client libraries:

### Using OpenAPI Generator

```bash
# Install openapi-generator-cli
npm install -g @openapitools/openapi-generator-cli

# Generate Python client
openapi-generator-cli generate \
  -i http://localhost:8082/docmgmt/api/v3/api-docs \
  -g python \
  -o ./generated-clients/python

# Generate TypeScript client
openapi-generator-cli generate \
  -i http://localhost:8082/docmgmt/api/v3/api-docs \
  -g typescript-axios \
  -o ./generated-clients/typescript
```

## Security Considerations

Currently, the API is **not secured**. In production, you should:

1. **Add authentication** (e.g., OAuth2, JWT)
2. **Configure SpringDoc security schemes**:
   ```java
   @SecurityScheme(
       name = "bearerAuth",
       type = SecuritySchemeType.HTTP,
       scheme = "bearer",
       bearerFormat = "JWT"
   )
   ```
3. **Annotate operations**:
   ```java
   @SecurityRequirement(name = "bearerAuth")
   @GetMapping("/api/documents")
   ```

## Troubleshooting

### Swagger UI Not Loading

1. Check application is running: http://localhost:8082/docmgmt
2. Verify property: `springdoc.swagger-ui.enabled=true`
3. Clear browser cache
4. Check logs for errors

### API Docs Empty

1. Ensure controllers have `@RestController` annotation
2. Check Spring component scanning includes controller package
3. Verify SpringDoc dependency is in pom.xml

### Wrong Base URL

Update `OpenAPIConfig.java`:
```java
@Bean
public OpenAPI documentManagementOpenAPI() {
    Server server = new Server();
    server.setUrl("http://your-domain.com/docmgmt");
    // ...
}
```

## Resources

- **SpringDoc OpenAPI**: https://springdoc.org/
- **OpenAPI Specification**: https://swagger.io/specification/
- **Swagger UI**: https://swagger.io/tools/swagger-ui/
- **OpenAPI Generator**: https://openapi-generator.tech/

## Summary

The Document Management System now provides complete interactive API documentation via Swagger UI, making it easy to:
- Explore all available endpoints
- Understand request/response formats
- Test APIs directly from the browser
- Generate client SDKs
- Export OpenAPI specifications

Access Swagger UI at: **http://localhost:8082/docmgmt/api/swagger-ui.html**
