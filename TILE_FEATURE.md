# Tile Display Feature

## Overview
The tile display feature allows you to create custom, visually-appealing views of documents organized in folders. Documents are displayed as cards/tiles with configurable fields, colors, and grouping options.

## Key Features

### 1. URL Field on Documents
- All SysObject entities (including documents) now support a `url` field
- Allows documents to link to external resources
- Maximum length: 2048 characters

### 2. Tile Display Views
Access tile views via the URL pattern: `/tiles/{folderName}`

Each tile can display:
- Document name
- Description
- Document type
- Tags
- External URL link
- Link to document detail view

### 3. Configurable Display Options

#### Field Visibility
Configure which fields appear on each tile:
- name
- description
- url
- documentType
- tags

#### Tile Sizing
Choose from three tile sizes:
- SMALL (250px width)
- MEDIUM (300px width - default)
- LARGE (400px width)

#### Color Coding Strategies
Tiles can be color-coded using several strategies:
- **NONE**: No color coding
- **BY_FOLDER**: Color by subfolder (auto-generated or custom)
- **BY_TYPE**: Color by document type
- **BY_TAG**: Color by first tag
- **CUSTOM**: Use custom JSON color mappings

Example custom color mapping:
```json
{
  "ARTICLE": "#FF5733",
  "REPORT": "#33FF57",
  "CONTRACT": "#3357FF"
}
```

#### Grouping
Enable **Group by Subfolder** to organize tiles by their containing subfolder. Documents in subfolders are grouped together under the subfolder name.

#### Sorting
Sort tiles by:
- NAME (default)
- TYPE
- CREATED_DATE
- MODIFIED_DATE

### 4. Navigation & Access

#### From Folder View
1. Navigate to **Folders** view
2. Select a folder in the tree
3. Click the **"View as Tiles"** button in the toolbar
4. This opens the tile display for that folder

#### Direct URL Access
Navigate directly to: `/tiles/{folderName}`

Replace `{folderName}` with the actual folder name (e.g., `/tiles/ProjectDocs`)

### 5. Configuration

#### Accessing Configuration
From any tile view, click the **"Configure"** button to access tile settings.

Or navigate to: `/tile-config/{folderName}`

#### Configuration Options
- **Group by Subfolder**: Toggle subfolder grouping
- **Visible Fields**: Comma-separated list of fields to display
- **Color Strategy**: Choose from color coding options
- **Custom Color Mappings**: JSON object for custom colors
- **Tile Size**: SMALL, MEDIUM, or LARGE
- **Show Detail Link**: Toggle document detail link
- **Show URL Link**: Toggle external URL link
- **Sort Order**: Choose sorting method

## REST API

### Get Tiles for a Folder
```
GET /api/tiles/{folderName}
```
Returns an array of TileDTO objects.

### Get Tile Configuration
```
GET /api/tiles/config/{folderId}
GET /api/tiles/config/by-name/{folderName}
```
Returns the TileConfigurationDTO for the folder.

### Save Tile Configuration
```
POST /api/tiles/config
Content-Type: application/json

{
  "folderId": 1,
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

### Delete Configuration
```
DELETE /api/tiles/config/{configId}
```
Deletes the configuration (folder will revert to default settings).

## Database Schema

### New Table: tile_configuration
```sql
CREATE TABLE tile_configuration (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
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

### Updated: sys_object table
New column: `url VARCHAR(2048)`

## Usage Examples

### Example 1: Simple Project Documentation Folder
1. Create a folder called "ProjectDocs"
2. Add documents with URLs pointing to GitHub, Confluence, etc.
3. Navigate to `/tiles/ProjectDocs`
4. Configure to show name, description, and URL
5. Users can quickly browse and access external resources

### Example 2: Color-Coded by Type
1. Create folder "Legal Documents"
2. Add various document types (contracts, reports, articles)
3. Configure color strategy: BY_TYPE
4. Set custom colors for each document type
5. Tiles display with color-coded borders for easy identification

### Example 3: Grouped by Category
1. Create folder "Resources" with subfolders "Internal" and "External"
2. Add documents to each subfolder
3. Enable "Group by Subfolder"
4. Tiles are visually grouped by their subfolder

## Integration with Existing Features
- Works seamlessly with existing folder hierarchy
- Respects document versioning
- Integrates with document detail views
- Compatible with all document types (Article, Report, Contract, etc.)

## Testing
Integration tests are provided in:
`src/test/java/com/docmgmt/integration/TileIntegrationTest.java`

Tests cover:
- Retrieving tiles by folder name
- Getting and saving tile configurations
- Color mapping functionality
- Subfolder grouping
- API endpoints

## Future Enhancements
Potential improvements for future versions:
- Drag-and-drop tile reordering
- Custom tile templates
- Thumbnail images on tiles
- Filter and search within tile view
- Public/shareable tile views
- Responsive grid layouts
- Export tile view as HTML
