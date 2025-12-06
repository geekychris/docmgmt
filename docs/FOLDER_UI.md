# Folder Browser UI

## Overview

The Folder Browser UI provides a visual interface for managing hierarchical folder structures and organizing documents within folders. It is accessible at `/folders` in the application and appears in the navigation menu.

## Features

### 1. Hierarchical Folder Tree

The left panel displays a tree view of all folders in the system:
- **Root folders** appear at the top level
- **Subfolders** appear nested under their parent folders
- **Item count** is displayed for each folder
- Folders can be **expanded/collapsed** to show/hide their children
- **Click on a folder** to view its contents in the right panel

### 2. Folder Operations

#### Create Root Folder
- Click **"New Root Folder"** button in the toolbar
- Fill in the folder details:
  - **Name** (required): The folder name
  - **Path**: Optional path identifier
  - **Description**: Optional description
- Click **"Create"** to save

#### Create Subfolder
- Select a parent folder in the tree
- Click **"New Subfolder"** button (enabled after selecting a folder)
- Fill in the folder details
- The new folder will be created as a child of the selected folder

### 3. Document Operations

#### Create Document in Folder
- Select a folder in the tree
- Click **"New Document Here"** button
- Fill in the document details:
  - **Name** (required): Document name
  - **Type** (required): Choose from ARTICLE, REPORT, CONTRACT, MANUAL, PRESENTATION, OTHER
  - **Description**: Optional description
  - **Author**: Optional author name
- The document will be created and automatically linked to the selected folder

#### Link Existing Document
- Select a folder in the tree
- Click **"Link Existing Document"** button
- Choose a document from the dropdown (shows latest versions only)
- The document will be linked to the folder
- **Note**: Documents can exist in multiple folders

### 4. Folder Contents View

The right panel shows the contents of the selected folder:
- **Icon column**: Visual indicator (folder icon or document icon)
- **Name**: The item's name
- **Type**: "Folder" or the document type
- **Version**: The version number (major.minor)

#### Remove Item from Folder
- Select an item in the contents grid
- Click **"Remove from Folder"** button
- The item will be unlinked from the folder (but not deleted)

## Technical Details

### Architecture

```
FolderView.java (UI Component)
    ├── TreeGrid<Folder> (Hierarchical folder display)
    ├── Grid<SysObject> (Folder contents display)
    ├── Dialogs for CRUD operations
    └── Service Integration
        ├── FolderService (Folder operations)
        └── DocumentService (Document operations)
```

### Key Components

1. **FolderView**: Main UI component extending `VerticalLayout`
2. **TreeGrid**: Vaadin component for hierarchical data display
3. **AbstractBackEndHierarchicalDataProvider**: Custom data provider that loads folders on-demand
4. **Dialogs**: Modal forms for creating folders and documents

### Data Loading

The folder tree uses lazy loading:
- Root folders are loaded initially
- Child folders are loaded when a parent is expanded
- Folder contents are loaded when a folder is selected

### State Management

- `currentFolder`: Tracks the selected folder
- Buttons are enabled/disabled based on selection state
- Tree and contents are refreshed after operations

## Usage Examples

### Example 1: Organize Project Documents

1. Create a root folder "Projects"
2. Create subfolders "Project Alpha" and "Project Beta" under "Projects"
3. Create documents in each project folder
4. Documents are now organized hierarchically

### Example 2: Document in Multiple Locations

1. Create a document in "Legal" folder
2. Navigate to "Finance" folder
3. Use "Link Existing Document" to add the same document to "Finance"
4. The document now appears in both folders

### Example 3: Reorganize Structure

1. Create a new organizational structure with folders
2. Link existing documents to appropriate folders
3. Remove documents from old folders as needed
4. Documents remain available even when removed from folders

## Integration with REST API

The UI uses the same service layer as the REST API:
- All operations are available via both UI and REST endpoints
- Folder hierarchy created in the UI is visible in API responses
- Documents linked via API appear in the UI

See `clients/README.md` for REST API documentation.

## Future Enhancements

Potential future improvements:
- Drag-and-drop folder/document organization
- Context menu for folder operations (rename, delete, move)
- Breadcrumb navigation
- Search within folder hierarchy
- Folder permissions and access control
- Bulk operations (move multiple items)
- Folder metadata editing
