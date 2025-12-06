# Hierarchical File Storage

## Overview

The Document Management System uses a hierarchical directory structure for storing files in FileStores. This prevents filesystem performance degradation that occurs when storing thousands of files in a single directory.

## Directory Structure

Files are stored using a 4-level hierarchical path based on the UUID generated for each piece of content:

```
aa/bb/cc/dd/aaaabbbb-cccc-dddd-eeee-ffffffffffff.ext
```

### Example

For a file with UUID `d43a7b2e-f9c4-4a1b-8e5d-123456789abc` and extension `.pdf`:

```
d4/3a/7b/2e/d43a7b2e-f9c4-4a1b-8e5d-123456789abc.pdf
```

### Structure Breakdown

- **Level 1**: First 2 characters of UUID (without hyphens) → `d4`
- **Level 2**: Next 2 characters → `3a`
- **Level 3**: Next 2 characters → `7b`
- **Level 4**: Next 2 characters → `2e`
- **Filename**: Full UUID with original extension → `d43a7b2e-f9c4-4a1b-8e5d-123456789abc.pdf`

## Benefits

### Performance

With 4 levels of 2-character hex directories:
- **256 options per level** (00-ff in hexadecimal)
- **256^4 = 4.3 billion** possible directory combinations
- **Maximum ~256 entries per directory** at each level
- Prevents filesystem slowdown from thousands of files in one directory

### Scalability

The system can efficiently handle:
- Millions of files without performance degradation
- Uniform distribution across directories
- Efficient filesystem operations (list, search, delete)

### Reliability

- Automatic directory creation on file upload
- Automatic cleanup of empty directories on file deletion
- No manual directory management required

## Implementation

### File Creation

When content is uploaded to a FileStore:

1. UUID is generated for the file
2. Hierarchical path is computed from the UUID
3. Full directory path is created automatically
4. File is written to the final location

```java
// ContentService.generateStoragePath()
String uuid = UUID.randomUUID().toString();
String uuidNoDashes = uuid.replace("-", "");

String level1 = uuidNoDashes.substring(0, 2);
String level2 = uuidNoDashes.substring(2, 4);
String level3 = uuidNoDashes.substring(4, 6);
String level4 = uuidNoDashes.substring(6, 8);

return String.format("%s/%s/%s/%s/%s%s", 
    level1, level2, level3, level4, uuid, extension);
```

### File Deletion

When content is deleted:

1. File is deleted from filesystem
2. Parent directories are checked from innermost to outermost
3. Empty directories are recursively removed
4. Cleanup stops when a non-empty directory is encountered

```java
// Content.cleanupStorage()
Path currentDir = filePath.getParent();
while (currentDir != null && !currentDir.equals(rootPath)) {
    if (directory is empty) {
        Files.delete(currentDir);
        currentDir = currentDir.getParent();
    } else {
        break; // Stop at first non-empty directory
    }
}
```

## Storage Path Format

### Database Storage

The `storage_path` column in the `content` table stores the relative path:

```
d4/3a/7b/2e/d43a7b2e-f9c4-4a1b-8e5d-123456789abc.pdf
```

### Filesystem Path

The full path combines the FileStore root with the storage path:

```
/var/docmgmt/file-store-1/d4/3a/7b/2e/d43a7b2e-f9c4-4a1b-8e5d-123456789abc.pdf
```

## API Usage

### Uploading Content to FileStore

```java
Content content = contentService.createContentInFileStore(
    multipartFile,     // The uploaded file
    sysObject,         // Parent document/object
    fileStoreId        // ID of the target FileStore
);

// Storage path is automatically generated:
// e.g., "d4/3a/7b/2e/d43a7b2e-f9c4-4a1b-8e5d-123456789abc.pdf"
String storagePath = content.getStoragePath();
```

### Reading Content

```java
// Read content bytes (works transparently for both DB and FileStore)
byte[] bytes = content.getContentBytes();

// Or via service
byte[] bytes = contentService.getContentBytes(contentId);
```

### Deleting Content

```java
// Deletes file and cleans up empty directories automatically
contentService.delete(contentId);
```

## Directory Cleanup Behavior

### Scenario 1: Single File in Branch

```
Before deletion:
  aa/bb/cc/dd/file1.txt

After deletion:
  (all directories removed)
```

### Scenario 2: Shared Parent Directories

```
Before deletion:
  aa/bb/cc/dd/file1.txt
  aa/bb/ee/ff/file2.txt

Delete file1.txt:
  aa/bb/ee/ff/file2.txt  ← file2 remains
  aa/ and bb/ remain (because ee/ff/file2.txt still exists)
  cc/ and dd/ removed (empty)
```

### Scenario 3: FileStore Root Protection

```
The cleanup process never removes the FileStore root directory itself,
only subdirectories created by the hierarchical path generation.
```

## Configuration

### FileStore Setup

```java
FileStore fileStore = FileStore.builder()
    .name("primary-storage")
    .rootPath("/var/docmgmt/primary")
    .status(FileStore.Status.ACTIVE)
    .build();

fileStoreService.save(fileStore);
```

Files will be stored as:
```
/var/docmgmt/primary/aa/bb/cc/dd/[uuid].[ext]
```

## Testing

The hierarchical storage implementation includes comprehensive tests:

### HierarchicalStorageTest

- **testStoragePathIsHierarchical**: Verifies correct path generation
- **testCleanupRemovesEmptyDirectories**: Ensures empty directories are cleaned up
- **testCleanupStopsAtNonEmptyDirectory**: Verifies cleanup stops when directories contain files

Run tests:
```bash
mvn test -Dtest=HierarchicalStorageTest
```

## Migration Notes

### Existing FileStores

If you have existing FileStores with flat directory structure:

1. New files will automatically use hierarchical structure
2. Old files will continue to work (path stored in database)
3. No migration required for existing files
4. Old files will use flat paths, new files will use hierarchical paths

### Manual Migration (Optional)

To migrate existing files to hierarchical structure:

```java
// Pseudo-code for migration script
for each content in fileStore {
    if (!content.getStoragePath().contains("/")) {
        // Old flat path
        byte[] data = content.getContentBytes();
        String newPath = generateHierarchicalPath(content.getName());
        
        // Move file
        content.setStoragePath(newPath);
        content.setContentBytes(data);
        
        // Cleanup old location
        deleteOldFile();
    }
}
```

## Performance Characteristics

### Directory Listing

- **Flat structure**: O(n) where n = total files
- **Hierarchical**: O(m) where m = ~256 (max files per directory)
- **Improvement**: ~4000x faster for 1M files

### File Lookup

- **Both structures**: O(1) - direct path lookup
- **No performance impact** on file access

### Space Overhead

- **Directory entries**: ~4KB per directory (typical)
- **For 1M files**: ~16,000 directories × 4KB = ~64MB
- **Negligible** compared to file content size

## Best Practices

1. **Let the system handle paths**: Don't manually construct storage paths
2. **Use ContentService methods**: They handle all path generation automatically
3. **Don't access filesystem directly**: Use Content.getContentBytes() instead
4. **FileStore cleanup**: Happens automatically, no manual intervention needed
5. **Backup strategy**: Backup entire FileStore root directory

## Troubleshooting

### Issue: File not found

**Check**:
1. Content entity exists in database
2. `storage_path` column is set correctly
3. FileStore `root_path` is accessible
4. Full path = `root_path + "/" + storage_path` exists

### Issue: Permission denied

**Check**:
1. Application has write permissions to FileStore root
2. Directory creation succeeds (logged on first upload)
3. User running application has filesystem access

### Issue: Orphaned directories

**Cause**: Application crash during file deletion

**Solution**: Run directory cleanup script:
```bash
# Find empty directories
find /var/docmgmt -type d -empty

# Remove empty directories (safe)
find /var/docmgmt -type d -empty -delete
```

## Summary

The hierarchical file storage system provides:
- ✅ Scalable performance for millions of files
- ✅ Automatic directory management
- ✅ Transparent to API users
- ✅ Backward compatible with existing flat structures
- ✅ Efficient cleanup on deletion
- ✅ Production-ready with comprehensive tests
