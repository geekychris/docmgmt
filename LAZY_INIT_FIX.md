# Lazy Initialization Fix for Tile Display

## Problem

When clicking "View as Tiles" in the UI, the application threw a `LazyInitializationException`:

```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role: 
com.docmgmt.model.Folder.items: could not initialize proxy - no Session
```

This occurred because the `TileService.getTiles()` method was trying to access lazy-loaded collections (`folder.getItems()` and `folder.getChildFolders()`) outside of an active Hibernate session.

## Root Cause

The issue occurred in `TileService.java` at lines:
- Line 119: `folder.getChildFolders()` - accessing child folders
- Line 123: `subfolder.getItems()` - accessing items in subfolders  
- Line 130: `folder.getItems()` - accessing items in folder
- Line 136: `folder.getItems()` - accessing items in folder

When these collections are accessed, they need to be loaded within an active transaction/session. The folder was being loaded in one transaction, but then accessed in a different context where the session was no longer available.

## Solution

### 1. Added Eager Fetch Queries to FolderRepository

Added two new query methods to `FolderRepository.java`:

```java
@Query("SELECT DISTINCT f FROM Folder f " +
    "LEFT JOIN FETCH f.items " +
    "LEFT JOIN FETCH f.childFolders " +
    "WHERE f.id = :id")
Optional<Folder> findByIdWithItemsAndChildren(@Param("id") Long id);

@Query("SELECT DISTINCT f FROM Folder f " +
    "LEFT JOIN FETCH f.items " +
    "LEFT JOIN FETCH f.childFolders " +
    "WHERE f.name = :name")
List<Folder> findByNameWithItemsAndChildren(@Param("name") String name);
```

These queries use `LEFT JOIN FETCH` to eagerly load both the `items` and `childFolders` collections in a single query, avoiding the N+1 problem and ensuring data is loaded within the transaction.

### 2. Added Service Methods to FolderService

Added two new methods to `FolderService.java` that use the eager fetch queries and also initialize nested collections:

```java
@Transactional(readOnly = true)
public Folder findByIdForTileDisplay(Long id) {
    return repository.findByIdWithItemsAndChildren(id)
        .map(folder -> {
            // Initialize child folder items as well
            if (folder.getChildFolders() != null) {
                for (Folder child : folder.getChildFolders()) {
                    if (child.getItems() != null) {
                        child.getItems().size(); // Force initialization
                    }
                }
            }
            return folder;
        })
        .orElseThrow(() -> new EntityNotFoundException("Folder not found with id: " + id));
}

@Transactional(readOnly = true)
public List<Folder> findByNameForTileDisplay(String name) {
    List<Folder> folders = repository.findByNameWithItemsAndChildren(name);
    // Initialize child folder items as well
    folders.forEach(folder -> {
        if (folder.getChildFolders() != null) {
            for (Folder child : folder.getChildFolders()) {
                if (child.getItems() != null) {
                    child.getItems().size(); // Force initialization
                }
            }
        }
    });
    return folders;
}
```

The key aspects:
- Annotated with `@Transactional(readOnly = true)` to ensure a transaction context
- Use the eager fetch repository methods
- Additionally initialize nested child folder items within the transaction
- All data is fully loaded before the transaction ends

### 3. Updated TileService

Modified `TileService.getTilesByFolderName()` to use the new service method:

```java
@Transactional(readOnly = true)
public List<TileDTO> getTilesByFolderName(String folderName) {
    // Changed from: folderService.findAllVersionsByName(folderName)
    List<Folder> folders = folderService.findByNameForTileDisplay(folderName);
    if (folders.isEmpty()) {
        throw new EntityNotFoundException("Folder not found: " + folderName);
    }
    
    Folder folder = folders.get(0);
    TileConfiguration config = getConfiguration(folder.getId());
    
    return getTiles(folder, config);
}
```

## Why This Works

1. **Single Query with JOIN FETCH**: The repository query loads the folder along with its items and child folders in one SQL query using `LEFT JOIN FETCH`. This is more efficient than multiple queries and ensures all data is loaded within the transaction.

2. **Nested Collection Initialization**: The service methods explicitly initialize nested collections (child folder items) by calling `.size()` on them while still within the transaction context.

3. **Transaction Boundary**: The `@Transactional(readOnly = true)` annotation ensures that the entire loading process happens within a single transaction, so the Hibernate session remains open while accessing all lazy collections.

4. **Data Availability**: By the time `getTiles()` is called, all required data is already loaded into memory and detached from the session, so no further database access is needed.

## Files Modified

1. `/src/main/java/com/docmgmt/repository/FolderRepository.java`
   - Added `findByIdWithItemsAndChildren()`
   - Added `findByNameWithItemsAndChildren()`

2. `/src/main/java/com/docmgmt/service/FolderService.java`
   - Added `findByIdForTileDisplay()`
   - Added `findByNameForTileDisplay()`

3. `/src/main/java/com/docmgmt/service/TileService.java`
   - Modified `getTilesByFolderName()` to use new eager loading method
   - Added `@Transactional(readOnly = true)` annotation

## Testing

To test the fix:

1. Start the application: `mvn spring-boot:run`
2. Navigate to: `http://localhost:8082/docmgmt/folders`
3. Create or select a folder with documents
4. Click the "View as Tiles" button
5. The tile view should load without any LazyInitializationException

## Performance Considerations

**Pros:**
- Single query loads all necessary data (fewer database round trips)
- Avoids N+1 query problem
- All data loaded within transaction boundary

**Cons:**
- If a folder has many child folders and items, the query will load all of them at once
- For very large folders, consider pagination or limiting the number of items displayed

## Alternative Solutions Considered

1. **Open Session in View Pattern**: Not recommended for Vaadin applications due to architectural concerns
2. **DTO Projection**: Would require more code changes and separate DTO mapping
3. **@EntityGraph**: Similar to JOIN FETCH but more complex to configure
4. **Lazy Collection Initialization in Service**: Current approach is a hybrid of this with eager fetching

## Future Improvements

Consider adding:
- Pagination support for folders with many items
- Configuration to limit the number of items/folders loaded
- Caching of frequently accessed tile configurations
- Async loading for large datasets
