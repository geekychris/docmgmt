# Tile Display Feature - Complete Guide

## Table of Contents
1. [Overview](#overview)
2. [Architecture & Design](#architecture--design)
3. [Installation & Setup](#installation--setup)
4. [Configuration](#configuration)
5. [Usage Guide](#usage-guide)
6. [API Reference](#api-reference)
7. [Development](#development)
8. [Troubleshooting](#troubleshooting)

---

## Overview

The Tile Display Feature transforms how documents are visualized in your document management system. Instead of traditional list or grid views, documents are displayed as attractive, customizable tiles (cards) that can be organized, color-coded, and grouped according to your needs.

### Key Capabilities
- **Visual Organization**: Display documents as tiles with configurable layouts
- **URL Integration**: Each document can have an external URL for quick access
- **Color Coding**: Automatically or manually color-code tiles by type, folder, or tag
- **Flexible Grouping**: Organize tiles by subfolders
- **Responsive Sizing**: Choose from small, medium, or large tile sizes
- **Customizable Fields**: Select which document fields appear on each tile

### Use Cases
1. **Project Documentation Hubs**: Create visual dashboards linking to external resources (GitHub, Confluence, Jira)
2. **Legal Document Libraries**: Color-code contracts, reports, and articles for quick identification
3. **Resource Collections**: Group internal and external resources in visually distinct categories
4. **Knowledge Bases**: Create browsable collections of documents with rich metadata display

---

## Architecture & Design

### System Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Presentation Layer                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │  TileView    │  │TileConfigView│  │ FolderView ││
│  │   (Vaadin)   │  │   (Vaadin)   │  │  (Vaadin)  ││
│  └──────────────┘  └──────────────┘  └────────────┘│
└─────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│                   REST API Layer                     │
│              ┌──────────────────────┐                │
│              │   TileController     │                │
│              │  /api/tiles/...      │                │
│              └──────────────────────┘                │
└─────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│                   Service Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ TileService  │  │FolderService │  │   Others   ││
│  └──────────────┘  └──────────────┘  └────────────┘│
└─────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│                  Repository Layer                    │
│  ┌──────────────────────────────────────────────┐   │
│  │    TileConfigurationRepository               │   │
│  │    FolderRepository, etc.                    │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│                   Database (H2)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │  sys_object  │  │tile_config   │  │   folder   ││
│  │  + url field │  │              │  │            ││
│  └──────────────┘  └──────────────┘  └────────────┘│
└─────────────────────────────────────────────────────┘
```

### Data Model

#### TileConfiguration Entity
Stores display preferences for each folder:
```java
@Entity
public class TileConfiguration {
    Long id;
    Folder folder;                    // One-to-one with folder
    Boolean groupBySubfolder;         // Enable subfolder grouping
    String visibleFields;             // Comma-separated field list
    ColorStrategy colorStrategy;      // Color coding method
    String colorMappings;             // JSON for custom colors
    TileSize tileSize;               // SMALL, MEDIUM, LARGE
    Boolean showDetailLink;           // Show document detail link
    Boolean showUrlLink;              // Show external URL link
    SortOrder sortOrder;              // Sorting method
}
```

#### SysObject Enhancement
The base SysObject class now includes:
```java
@Column(name = "url", length = 2048)
private String url;
```
This allows any document to reference external resources.

#### TileDTO
Data transfer object for tile display:
```java
public class TileDTO {
    Long id;
    String name;
    String description;
    String url;                  // External URL
    String detailUrl;            // Internal detail page URL
    String documentType;
    Set<String> tags;
    String color;                // Computed color for the tile
    String groupName;            // Subfolder name for grouping
    Map<String, Object> customFields;
}
```

### Design Patterns

1. **Service Layer Pattern**: Business logic isolated in `TileService`
2. **DTO Pattern**: Separate DTOs for API data transfer
3. **Repository Pattern**: Data access through Spring Data JPA repositories
4. **Builder Pattern**: Used extensively for entity and DTO construction
5. **Strategy Pattern**: Color coding strategies (BY_TYPE, BY_FOLDER, etc.)

### Color Coding Strategy

The system supports multiple color assignment strategies:

```java
public enum ColorStrategy {
    NONE,        // No color coding
    BY_FOLDER,   // Hash-based color from folder name
    BY_TYPE,     // Hash-based color from document type
    BY_TAG,      // Hash-based color from first tag
    CUSTOM       // User-defined JSON mapping
}
```

Colors are either:
- Auto-generated using hash functions for consistency
- User-defined through JSON mappings like:
  ```json
  {
    "ARTICLE": "#FF5733",
    "REPORT": "#33FF57",
    "CONTRACT": "#3357FF"
  }
  ```

---

## Installation & Setup

### Prerequisites
- Java 21 (Amazon Corretto 23 JDK)
- Maven 3.8+
- Git

### Step 1: Get the Code
```bash
cd /Users/chris/code/warp_experiments/docmgmt
git pull  # If using version control
```

### Step 2: Build the Project
```bash
mvn clean install
```

This will:
- Compile all source files
- Run tests (including new tile integration tests)
- Create executable JARs

### Step 3: Database Schema
The application uses H2 database with automatic schema generation. On first run, the following will be created:

**New table: `tile_configuration`**
```sql
CREATE TABLE tile_configuration (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  folder_id BIGINT UNIQUE,
  group_by_subfolder BOOLEAN,
  visible_fields TEXT,
  color_strategy VARCHAR(50),
  color_mappings TEXT,
  tile_size VARCHAR(20),
  show_detail_link BOOLEAN,
  show_url_link BOOLEAN,
  sort_order VARCHAR(50),
  FOREIGN KEY (folder_id) REFERENCES folder(id)
);
```

**Updated table: `sys_object`**
```sql
ALTER TABLE sys_object ADD COLUMN url VARCHAR(2048);
```

### Step 4: Run the Application

#### Option A: Run the Web Application
```bash
mvn spring-boot:run
```

#### Option B: Run the pre-built JAR
```bash
java -jar target/document-management-0.0.1-SNAPSHOT-web.jar
```

#### Option C: Development Mode with Hot Reload
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dvaadin.productionMode=false"
```

### Step 5: Access the Application
Open your browser to: **http://localhost:8082/docmgmt**

Default routes:
- Main UI: `http://localhost:8082/docmgmt/`
- Folders: `http://localhost:8082/docmgmt/folders`
- Tiles: `http://localhost:8082/docmgmt/tiles/{folderName}`
- Swagger API: `http://localhost:8082/docmgmt/api/swagger-ui.html`
- H2 Console: `http://localhost:8082/docmgmt/h2-console`

---

## Configuration

### Application Configuration

Edit `src/main/resources/application.properties` or `application.yml`:

```properties
# Server Configuration
server.port=8082
server.servlet.context-path=/docmgmt

# Database Configuration (H2)
spring.datasource.url=jdbc:h2:file:./docmgmt_db
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update

# Vaadin Configuration
vaadin.productionMode=false
```

### Tile Display Defaults

Default configuration (when no custom config exists):
```java
{
  "groupBySubfolder": false,
  "visibleFields": "name,description,url",
  "colorStrategy": "NONE",
  "tileSize": "MEDIUM",
  "showDetailLink": true,
  "showUrlLink": true,
  "sortOrder": "NAME"
}
```

### Environment Variables

You can override settings using environment variables:
```bash
export SERVER_PORT=9090
export SPRING_DATASOURCE_URL=jdbc:h2:file:./custom_data/docmgmt
mvn spring-boot:run
```

---

## Usage Guide

### Creating a Tile View

#### Step 1: Create a Folder
1. Navigate to **Folders** view (`/folders`)
2. Click **"New Root Folder"**
3. Enter folder details:
   - Name: e.g., "Project Resources"
   - Description: e.g., "External links and documentation"
   - Owner and Authors (optional)
4. Click **"Create"**

#### Step 2: Add Documents with URLs
1. Select your folder in the tree
2. Click **"New Document Here"**
3. Fill in document details:
   - Name: e.g., "GitHub Repository"
   - Description: e.g., "Main project repository"
   - **URL**: `https://github.com/yourproject/repo`
   - Document Type: ARTICLE, REPORT, etc.
   - Tags: Add relevant tags
4. Click **"Create"**
5. Repeat for more documents

#### Step 3: View as Tiles
1. Select the folder in the tree
2. Click **"View as Tiles"** button in toolbar
3. Tiles are displayed with default configuration

### Configuring Tile Display

#### From the Tile View
1. Open your tile view (`/tiles/YourFolderName`)
2. Click **"Configure"** button
3. The configuration form opens

#### Configuration Options

**Group by Subfolder**
- ☑ Enabled: Tiles are grouped under subfolder headings
- ☐ Disabled: All tiles displayed in a single grid

**Visible Fields**
Enter comma-separated field names:
```
name,description,url,documentType,tags
```
Available fields:
- `name` - Document name (always shown)
- `description` - Document description
- `url` - External URL
- `documentType` - Type of document
- `tags` - Document tags

**Color Strategy**
Choose from dropdown:
- **NONE**: No color coding
- **BY_FOLDER**: Auto-color by subfolder name
- **BY_TYPE**: Auto-color by document type
- **BY_TAG**: Auto-color by first tag
- **CUSTOM**: Use custom color mappings

**Custom Color Mappings** (if CUSTOM selected)
Enter JSON format:
```json
{
  "ARTICLE": "#FF5733",
  "REPORT": "#33FF57",
  "CONTRACT": "#3357FF",
  "SubfolderName": "#FFC300"
}
```

**Tile Size**
- **SMALL**: 250px width
- **MEDIUM**: 300px width (default)
- **LARGE**: 400px width

**Show Detail Link**
- ☑ Displays "Details" button linking to document detail view
- ☐ Hides detail button

**Show URL Link**
- ☑ Displays "Open" button linking to external URL
- ☐ Hides URL button

**Sort Order**
- **NAME**: Alphabetical by name (default)
- **TYPE**: By document type
- **CREATED_DATE**: By creation date
- **MODIFIED_DATE**: By last modified date

#### Save Configuration
1. Click **"Save Configuration"**
2. Success notification appears
3. Click **"Preview"** to see changes

### Advanced Usage

#### Creating Grouped Tile Views

1. Create a parent folder: "Resources"
2. Create subfolders:
   - "Internal Documentation"
   - "External Links"
   - "Templates"
3. Add documents to each subfolder
4. Configure parent folder:
   - Enable "Group by Subfolder"
   - Set Color Strategy to "BY_FOLDER"
5. View tiles - documents are grouped and color-coded

#### Color-Coded Document Types

1. Create folder with mixed document types
2. Add documents of various types (ARTICLE, REPORT, CONTRACT, MANUAL)
3. Configure tile display:
   - Color Strategy: CUSTOM
   - Custom mappings:
     ```json
     {
       "ARTICLE": "#3498db",
       "REPORT": "#2ecc71",
       "CONTRACT": "#e74c3c",
       "MANUAL": "#f39c12"
     }
     ```
4. Each document type displays with its assigned color

#### Direct URL Access

Share tile views with others:
```
http://localhost:8082/docmgmt/tiles/ProjectResources
```

Users can bookmark specific tile views for quick access.

---

## API Reference

### REST Endpoints

All endpoints are under `/api/tiles`

#### Get Tiles for a Folder

```http
GET /api/tiles/{folderName}
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Project Roadmap",
    "description": "Q1 2025 project roadmap",
    "url": "https://example.com/roadmap",
    "detailUrl": "/document-detail/1",
    "documentType": "REPORT",
    "tags": ["planning", "roadmap"],
    "color": "#3498db",
    "groupName": "Planning"
  }
]
```

#### Get Tile Configuration

```http
GET /api/tiles/config/{folderId}
GET /api/tiles/config/by-name/{folderName}
```

**Response:**
```json
{
  "id": 1,
  "folderId": 5,
  "folderName": "ProjectResources",
  "groupBySubfolder": true,
  "visibleFields": "name,description,url,tags",
  "colorStrategy": "BY_TYPE",
  "colorMappings": "{\"ARTICLE\":\"#FF5733\"}",
  "tileSize": "MEDIUM",
  "showDetailLink": true,
  "showUrlLink": true,
  "sortOrder": "NAME"
}
```

#### Save/Update Configuration

```http
POST /api/tiles/config
Content-Type: application/json
```

**Request Body:**
```json
{
  "folderId": 5,
  "groupBySubfolder": true,
  "visibleFields": "name,description,url,documentType",
  "colorStrategy": "CUSTOM",
  "colorMappings": "{\"ARTICLE\":\"#FF5733\",\"REPORT\":\"#33FF57\"}",
  "tileSize": "LARGE",
  "showDetailLink": true,
  "showUrlLink": true,
  "sortOrder": "NAME"
}
```

**Response:** TileConfigurationDTO

#### Delete Configuration

```http
DELETE /api/tiles/config/{configId}
```

**Response:** 204 No Content

### Service Layer API

For programmatic access within the application:

```java
@Autowired
private TileService tileService;

// Get tiles for a folder
List<TileDTO> tiles = tileService.getTilesByFolderName("MyFolder");

// Get configuration
TileConfiguration config = tileService.getConfiguration(folderId);

// Save configuration
TileConfigurationDTO dto = TileConfigurationDTO.builder()
    .folderId(folderId)
    .colorStrategy("BY_TYPE")
    .build();
TileConfiguration saved = tileService.saveConfiguration(dto);
```

---

## Development

### Project Structure

```
src/main/java/com/docmgmt/
├── model/
│   ├── TileConfiguration.java       # Entity
│   └── SysObject.java              # Enhanced with URL field
├── dto/
│   ├── TileDTO.java                # Tile data transfer
│   └── TileConfigurationDTO.java   # Config data transfer
├── repository/
│   └── TileConfigurationRepository.java
├── service/
│   └── TileService.java            # Business logic
├── controller/
│   └── TileController.java         # REST API
└── ui/views/
    ├── TileView.java               # Tile display UI
    ├── TileConfigurationView.java  # Configuration UI
    └── FolderView.java             # Enhanced with tile button

src/test/java/com/docmgmt/
└── integration/
    └── TileIntegrationTest.java    # Integration tests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run only tile tests
mvn test -Dtest=TileIntegrationTest

# Run with coverage
mvn test jacoco:report
```

### Building for Production

```bash
# Build production-ready JAR
mvn clean package -Pproduction

# Run production build
java -jar target/document-management-0.0.1-SNAPSHOT-web.jar
```

Production mode enables:
- Minified frontend assets
- Optimized Vaadin bundles
- Enhanced performance

### Extending the Feature

#### Adding New Color Strategies

1. Add to enum in `TileConfiguration.java`:
```java
public enum ColorStrategy {
    NONE, BY_FOLDER, BY_TYPE, BY_TAG, CUSTOM, BY_OWNER  // New
}
```

2. Implement in `TileService.determineColor()`:
```java
case BY_OWNER:
    if (item.getOwner() != null) {
        return generateColorFromString(item.getOwner().getUsername());
    }
    return null;
```

#### Adding New Tile Fields

1. Add getter to `TileDTO`:
```java
private LocalDateTime createdAt;
```

2. Populate in `TileDTO.fromSysObject()`:
```java
builder.createdAt(sysObject.getCreatedAt());
```

3. Update UI in `TileView.createTileCard()`:
```java
if (visibleFields.contains("createdAt")) {
    Span date = new Span(tile.getCreatedAt().format(formatter));
    card.add(date);
}
```

### Debugging

Enable debug logging in `application.properties`:
```properties
logging.level.com.docmgmt.service.TileService=DEBUG
logging.level.com.docmgmt.controller.TileController=DEBUG
```

Access H2 Console for database inspection:
```
http://localhost:8082/docmgmt/h2-console
JDBC URL: jdbc:h2:file:./docmgmt_db
User: sa
Password: password
```

---

## Troubleshooting

### Issue: Tiles Not Displaying

**Symptoms:** Blank page or "No documents found"

**Solutions:**
1. Verify folder has documents:
   ```sql
   SELECT * FROM folder_sysobjects WHERE folder_id = ?;
   ```
2. Check folder name spelling (case-sensitive)
3. Verify documents are linked to folder, not just in subfolders
4. Check browser console for JavaScript errors

### Issue: Configuration Not Saving

**Symptoms:** Changes revert after refresh

**Solutions:**
1. Check database connection
2. Verify folder ID is valid
3. Check logs for constraint violations:
   ```bash
   tail -f logs/spring.log
   ```
4. Ensure only one configuration per folder (UNIQUE constraint)

### Issue: Colors Not Applying

**Symptoms:** All tiles have same color or no color

**Solutions:**
1. Verify Color Strategy is not "NONE"
2. For CUSTOM strategy, validate JSON syntax:
   ```bash
   echo '{"ARTICLE":"#FF5733"}' | python -m json.tool
   ```
3. Check browser DevTools for CSS issues
4. Clear browser cache

### Issue: URLs Not Clickable

**Symptoms:** "Open" button doesn't appear

**Solutions:**
1. Verify document has URL field populated:
   ```sql
   SELECT id, name, url FROM sys_object WHERE url IS NOT NULL;
   ```
2. Check "Show URL Link" is enabled in configuration
3. Ensure URL is valid (starts with http:// or https://)

### Issue: Performance Degradation

**Symptoms:** Slow tile loading with many documents

**Solutions:**
1. Enable pagination (future enhancement)
2. Reduce visible fields to minimum needed
3. Use smaller tile size
4. Add database indexes:
   ```sql
   CREATE INDEX idx_folder_items ON folder_sysobjects(folder_id);
   ```
5. Enable Hibernate query caching

### Getting Help

1. **Check Logs:**
   ```bash
   tail -f logs/spring.log | grep -i tile
   ```

2. **Enable Debug Mode:**
   Add to `application.properties`:
   ```properties
   logging.level.root=DEBUG
   ```

3. **Run Tests:**
   ```bash
   mvn test -Dtest=TileIntegrationTest -X
   ```

4. **Database Inspection:**
   Use H2 Console to verify data integrity

---

## Summary

The Tile Display Feature provides a modern, visual way to organize and access documents in your document management system. With flexible configuration options, color coding, and grouping capabilities, it transforms static document listings into dynamic, user-friendly interfaces.

### Quick Start Checklist
- ✅ Build project: `mvn clean install`
- ✅ Run application: `mvn spring-boot:run`
- ✅ Create folder with documents
- ✅ Add URLs to documents
- ✅ Click "View as Tiles"
- ✅ Configure display settings
- ✅ Share tile view URL

### Next Steps
- Explore the REST API with Swagger UI
- Create grouped tile views with subfolders
- Experiment with color coding strategies
- Review integration tests for examples
- Build custom tile configurations for your workflow

For additional information, see:
- `TILE_FEATURE.md` - Feature overview
- `src/test/java/com/docmgmt/integration/TileIntegrationTest.java` - Test examples
- Swagger API documentation at `/swagger-ui.html`
