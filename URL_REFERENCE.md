# URL Reference Guide

## Application Configuration

- **Port**: 8082
- **Context Path**: /docmgmt
- **Base URL**: http://localhost:8082/docmgmt

## Main Application URLs

### User Interface (Vaadin)

| Route | URL | Description |
|-------|-----|-------------|
| Home | `http://localhost:8082/docmgmt/` | Main landing page |
| Folders | `http://localhost:8082/docmgmt/folders` | Folder browser view |
| Documents | `http://localhost:8082/docmgmt/documents` | Document listing view |
| Search | `http://localhost:8082/docmgmt/search` | Search interface |
| File Store | `http://localhost:8082/docmgmt/filestore` | File storage view |
| Users | `http://localhost:8082/docmgmt/users` | User management |
| Document Detail | `http://localhost:8082/docmgmt/document-detail/{id}` | Detailed document view |

### Tile Display (NEW Feature)

| Route | URL | Description |
|-------|-----|-------------|
| Tile View | `http://localhost:8082/docmgmt/tiles/{folderName}` | Display documents as tiles |
| Tile Configuration | `http://localhost:8082/docmgmt/tile-config/{folderName}` | Configure tile display |

**Examples**:
- `http://localhost:8082/docmgmt/tiles/ProjectDocs`
- `http://localhost:8082/docmgmt/tiles/Resources`
- `http://localhost:8082/docmgmt/tile-config/ProjectDocs`

### REST API

All REST API endpoints are prefixed with `/api`:

| Endpoint | URL | Description |
|----------|-----|-------------|
| API Base | `http://localhost:8082/docmgmt/api/` | REST API base path |
| Documents | `http://localhost:8082/docmgmt/api/documents` | Document operations |
| Folders | `http://localhost:8082/docmgmt/api/folders` | Folder operations |
| Content | `http://localhost:8082/docmgmt/api/content` | Content operations |
| Search | `http://localhost:8082/docmgmt/api/search` | Search API |
| **Tiles** | `http://localhost:8082/docmgmt/api/tiles` | **Tile operations (NEW)** |

### Tile REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tiles/{folderName}` | Get tiles for a folder |
| GET | `/api/tiles/config/{folderId}` | Get tile configuration |
| GET | `/api/tiles/config/by-name/{folderName}` | Get config by folder name |
| POST | `/api/tiles/config` | Save tile configuration |
| DELETE | `/api/tiles/config/{id}` | Delete configuration |

**Examples**:
```bash
# Get tiles for a folder
curl http://localhost:8082/docmgmt/api/tiles/ProjectDocs

# Get tile configuration
curl http://localhost:8082/docmgmt/api/tiles/config/1

# Save tile configuration
curl -X POST http://localhost:8082/docmgmt/api/tiles/config \
  -H "Content-Type: application/json" \
  -d '{"folderId": 1, "tileSize": "LARGE"}'
```

### Documentation & Tools

| Service | URL | Description |
|---------|-----|-------------|
| Swagger UI | `http://localhost:8082/docmgmt/api/swagger-ui.html` | Interactive API documentation |
| OpenAPI JSON | `http://localhost:8082/docmgmt/api/v3/api-docs` | OpenAPI specification |
| H2 Console | `http://localhost:8082/docmgmt/h2-console` | Database admin console |

## Database Connection (H2 Console)

When accessing the H2 Console, use these settings:

```
JDBC URL: jdbc:h2:file:./docmgmt_db
Username: sa
Password: password
Driver Class: org.h2.Driver
```

## Common Workflows

### Workflow 1: View Documents as Tiles

1. Navigate to: `http://localhost:8082/docmgmt/folders`
2. Select a folder in the tree
3. Click "View as Tiles" button
4. Browser navigates to: `http://localhost:8082/docmgmt/tiles/{folderName}`

### Workflow 2: Configure Tile Display

1. Navigate to: `http://localhost:8082/docmgmt/folders`
2. Select a folder in the tree
3. Click "Configure Tiles" button
4. Dialog opens with configuration options
5. Click "Save & Preview"
6. Browser navigates to: `http://localhost:8082/docmgmt/tiles/{folderName}`

### Workflow 3: Direct Tile Access

Share a tile view URL directly:
```
http://localhost:8082/docmgmt/tiles/MySharedFolder
```

Users can bookmark and access directly without navigating through folder tree.

## Environment Variables Override

You can override the port and context path using environment variables:

```bash
# Change port
export SERVER_PORT=9090

# Change context path
export SERVER_SERVLET_CONTEXT_PATH=/myapp

# Run application
mvn spring-boot:run
```

Then access at: `http://localhost:9090/myapp/`

## Configuration File

All URL settings are configured in:
```
src/main/resources/application.properties
```

Key settings:
```properties
# Server Configuration
server.port=8082
server.servlet.context-path=/docmgmt

# Database
spring.datasource.url=jdbc:h2:file:./docmgmt_db
spring.datasource.username=sa
spring.datasource.password=password

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Troubleshooting

### Port Already in Use

If port 8082 is already in use, you'll see:
```
***************************
APPLICATION FAILED TO START
***************************

Description:
Web server failed to start. Port 8082 was already in use.
```

**Solution**: Change the port in `application.properties` or use environment variable:
```bash
SERVER_PORT=8083 mvn spring-boot:run
```

### Context Path Issues

If you access `http://localhost:8082/` (without `/docmgmt`), you'll get a 404 error.

**Solution**: Always include the context path in the URL:
- ✅ `http://localhost:8082/docmgmt/`
- ❌ `http://localhost:8082/`

### Tile View 404

If you get a 404 when accessing a tile view, verify:

1. Folder name is correct (case-sensitive)
2. Context path is included in URL
3. Application is running

**Example**:
- ✅ `http://localhost:8082/docmgmt/tiles/ProjectDocs`
- ❌ `http://localhost:8082/tiles/ProjectDocs` (missing context path)
- ❌ `http://localhost:8080/docmgmt/tiles/ProjectDocs` (wrong port)

## Quick Reference

**Base URL**: `http://localhost:8082/docmgmt`

**Main Views**:
- Folders: `/folders`
- Tiles: `/tiles/{folderName}`
- Tile Config: `/tile-config/{folderName}`

**API**:
- REST Base: `/api/`
- Tiles API: `/api/tiles/`
- Swagger: `/api/swagger-ui.html`

**Admin**:
- H2 Console: `/h2-console`
