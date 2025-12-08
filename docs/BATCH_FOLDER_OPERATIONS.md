# Batch Folder Operations

This document describes the mass select and move functionality for folders in the Document Management System.

## Overview

The folder view now supports efficient batch operations to reorganize folder hierarchies. You can select multiple folders and move them to a new parent folder or to the root level.

## Features

### 1. Multi-Select Folders
- The folder tree grid now supports multi-selection mode
- Select multiple folders by clicking while holding Ctrl/Cmd or Shift keys
- Selected folders are highlighted in the tree

### 2. Mass Move Operations
Two buttons are available for moving selected folders:

- **Move Selected**: Opens a dialog to select a destination folder. The selected folders will become children of the destination.
- **Move to Root**: Instantly moves selected folders to root level (no parent folder).

### 3. Efficient Querying
The implementation uses lazy-loading hierarchical data providers:
- **Root folders** are queried first when the view loads
- **Child folders** are only queried when a parent node is expanded
- The folder hierarchy is never fully reconstituted - only visible nodes are loaded
- The move destination dialog uses the same efficient lazy-loading approach

### 4. Circular Reference Prevention
The system prevents circular references when moving folders:
- Cannot move a folder into itself
- Cannot move a folder into one of its descendants
- Validation occurs before the move operation
- Clear error messages are displayed if an invalid move is attempted

## API Endpoints

### Link Folders to Parent
```
PUT /api/folders/batch/link?parentId={parentId}
Body: [folderId1, folderId2, ...]
```
Links multiple folders to a parent folder. Pass `null` for `parentId` to move folders to root level.

### Unlink Folders from Parent
```
DELETE /api/folders/batch/unlink
Body: [folderId1, folderId2, ...]
```
Unlinks multiple folders from their parent, moving them to root level.

## Service Layer

### FolderService Methods

#### `linkFoldersToParent(Long parentId, List<Long> folderIds)`
- Links multiple folders to a parent folder in a single transaction
- Validates against circular references
- Unlinks folders from their current parent before linking to new parent
- Returns the updated parent folder (or null if moved to root)

#### `unlinkFoldersFromParent(List<Long> folderIds)`
- Unlinks multiple folders from their parent folders
- Moves folders to root level
- Performs operation in a single transaction

#### `wouldCreateCircularReference(Folder child, Folder parent)`
- Private helper method to detect circular references
- Traverses up the parent chain to ensure the child is not an ancestor of the parent

## UI Components

### FolderView Updates

1. **Multi-Select Tree Grid**
   - Changed selection mode from SINGLE to MULTI
   - Added selection listener to enable/disable batch operation buttons

2. **Move Buttons**
   - `moveFoldersButton`: Enabled when folders are selected
   - `moveToRootButton`: Enabled when folders are selected

3. **Move Dialog**
   - Displays count of selected folders
   - Shows lazy-loading folder tree for destination selection
   - Validates destination selection
   - Provides clear success/error feedback

## Usage Example

1. Navigate to the Folders view
2. Select multiple folders (Ctrl/Cmd + click or Shift + click)
3. Click "Move Selected" to choose a destination folder
4. Expand the destination tree to find the target parent folder
5. Select the destination and click "Move Here"
6. Alternatively, click "Move to Root" to move selected folders to root level

## Performance Considerations

- **Lazy Loading**: Only visible nodes are queried from the database
- **Batch Operations**: Multiple folders are moved in a single transaction
- **No Full Reconstitution**: The entire folder hierarchy is never loaded into memory
- **Efficient Queries**: 
  - `findRootFolders()` - Only root level folders
  - `findChildFolders(parent)` - Only immediate children of a specific folder

## Error Handling

The system handles the following error cases:
- **Circular Reference**: Clear message explaining which folder causes the cycle
- **Missing Folders**: 404 error if folder IDs don't exist
- **Empty Selection**: Warning notification if no folders are selected
- **Missing Destination**: Warning if destination folder is not selected in move dialog

## Testing

Unit tests are provided in `FolderBatchOperationsTest.java`:
- `testLinkFoldersToParent_Success`: Verifies successful linking
- `testLinkFoldersToRoot_Success`: Verifies moving to root
- `testLinkFoldersToParent_CircularReference`: Validates circular reference detection
- `testUnlinkFoldersFromParent_Success`: Verifies unlinking operation
- `testWouldCreateCircularReference_DirectSelfReference`: Tests self-reference prevention

## Future Enhancements

Possible improvements:
- Drag-and-drop folder moving
- Undo/redo for batch operations
- Bulk folder operations via CSV import
- Folder move history/audit trail
