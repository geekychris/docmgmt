# Swagger UI - Quick Start Guide

## Start the Application

```bash
mvn spring-boot:run
```

Wait for the application to start (you should see "Started DocumentManagementApplication" in the logs).

## Access Swagger UI

Open your browser and navigate to:

```
http://localhost:8082/docmgmt/api/swagger-ui.html
```

## What You'll See

The Swagger UI interface shows all REST API endpoints organized by tags:

- **Documents** - Document CRUD operations, versioning, queries
- **Search** - Full-text search with Lucene
- **Content** - File upload/download, storage management
- **File Stores** - External storage locations

## Try It Out!

### Example 1: Create a Document

1. Expand the **Documents** section
2. Click on **POST /api/documents**
3. Click **"Try it out"**
4. Use this JSON in the request body:

```json
{
  "name": "Getting Started with Spring Boot",
  "documentType": "ARTICLE",
  "description": "A beginner's guide to Spring Boot development",
  "keywords": "spring boot java tutorial web",
  "tags": ["spring", "java", "tutorial"]
}
```

5. Click **"Execute"**
6. See the response with the created document (note the `id` field)

### Example 2: Search Documents

1. Expand the **Search** section
2. Click on **GET /api/search**
3. Click **"Try it out"**
4. Enter `spring` in the **q** parameter
5. Click **"Execute"**
6. See matching documents in the response

### Example 3: Upload Content

1. Expand the **Content** section
2. Click on **POST /api/content/upload**
3. Click **"Try it out"**
4. Choose a file
5. Enter the document ID (from Example 1) in **sysObjectId**
6. Set **storeInDatabase** to `true`
7. Click **"Execute"**

## OpenAPI Specification

Download the API specification:

- **JSON**: http://localhost:8082/docmgmt/api/v3/api-docs
- **YAML**: http://localhost:8082/docmgmt/api/v3/api-docs.yaml

## Need More Details?

See the complete documentation: [docs/OPENAPI_SWAGGER.md](docs/OPENAPI_SWAGGER.md)

## Common Issues

**404 Error**: Make sure the application is running on port 8082

**UI Not Loading**: Clear browser cache and refresh

**Empty API Docs**: Restart the application with `mvn clean spring-boot:run`
