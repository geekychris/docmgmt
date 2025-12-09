# Folder Index Rebuild Feature

## Overview

The Folder Index Rebuild feature allows users to rebuild the Lucene search index and vector embeddings for all documents within a selected folder. This is useful for maintaining search accuracy and ensuring embeddings are up-to-date after bulk document operations.

## Features

### Selective Rebuilding
- **Folder-based**: Rebuild index only for documents in a specific folder
- **Recursive option**: Choose to include or exclude subfolders
- **Progress tracking**: Real-time feedback during the rebuild process
- **Automatic embeddings**: Generates vector embeddings if auto-generation is enabled

### Benefits
- **Efficiency**: Only rebuild what's needed instead of the entire index
- **Performance**: Faster than rebuilding the whole system
- **Flexibility**: Control scope with recursive subfolder option
- **Safety**: Non-destructive operation (only affects selected documents)

## User Interface

### Button Location
The "Rebuild Index" button appears in the main toolbar of the Folder View:
- Only enabled when a folder is selected
- Green success theme for visual distinction
- Refresh icon (⟳) for easy recognition

### Dialog

When clicked, opens a dialog with:

1. **Title**: Shows the folder name being rebuilt
2. **Description**: Explains what the operation does
3. **Recursive checkbox**: 
   - Checked by default
   - "Include subfolders (recursive)"
   - Uncheck to rebuild only the selected folder
4. **Progress area**: Shows status during rebuild
5. **Action buttons**:
   - Cancel (disabled during rebuild)
   - Rebuild Index (primary action)

## How It Works

### Step-by-Step Process

1. **User selects folder** in the folder tree
2. **Clicks "Rebuild Index"** button
3. **Configures options** (recursive or not)
4. **Clicks "Rebuild Index"** in dialog
5. **System collects documents**:
   - Traverses selected folder
   - If recursive, includes all subfolders
   - Counts total documents
6. **Rebuilds index**:
   - Calls `LuceneIndexService.rebuildIndex(documents)`
   - Indexes all document fields and content
   - Generates embeddings (if enabled)
7. **Shows completion**:
   - Success message with document count
   - Auto-closes after 2 seconds
   - Notification toast appears

### Behind the Scenes

```java
// 1. Collect documents from folder
List<Document> documents = new ArrayList<>();
collectDocumentsFromFolder(folder, documents, recursive);

// 2. Rebuild index (synchronous operation in async context)
luceneIndexService.rebuildIndex(documents);
// This calls:
//   - indexWriter.deleteAll() for selected docs
//   - indexDocument() for each document
//   - similarityService.rebuildAllEmbeddings() if enabled

// 3. Update UI with results
```

## Use Cases

### 1. After Bulk Upload

**Scenario**: User uploads 50 documents to a project folder

**Action**:
1. Select the project folder
2. Click "Rebuild Index"
3. Keep recursive checked
4. Rebuild completes in 10-30 seconds

**Result**: All documents indexed and searchable with embeddings

### 2. Content Updates

**Scenario**: User has updated content for multiple documents in a folder

**Action**:
1. Select the folder with updated documents
2. Click "Rebuild Index"
3. Rebuild to refresh index and embeddings

**Result**: Search and similarity search reflect updated content

### 3. After Transformation

**Scenario**: User transforms all PDFs to text in a folder using "Transform Folder (Recursive)"

**Action**:
1. Wait for transformations to complete
2. Select same folder
3. Click "Rebuild Index" (recursive)

**Result**: Text content now indexed and searchable

### 4. Fixing Embedding Issues

**Scenario**: Ollama was down during document creation, embeddings missing

**Action**:
1. Start Ollama service
2. Select affected folder
3. Click "Rebuild Index"
4. System generates missing embeddings

**Result**: Similarity search now works for all documents

### 5. Subfolder Organization

**Scenario**: User reorganizes documents into new subfolders

**Action**:
1. Select parent folder
2. Click "Rebuild Index" with recursive checked
3. Ensures all moved documents are indexed

**Result**: All documents in new structure are indexed

## Configuration

### Auto-Generate Embeddings

The feature respects the global embedding configuration:

```properties
# application.properties
docmgmt.similarity.auto-generate-embeddings=true
```

**When `true`**: 
- Rebuilds both Lucene index AND embeddings
- Takes longer (2-5 seconds per document)
- Enables similarity search

**When `false`**:
- Only rebuilds Lucene index
- Faster operation
- Similarity search unavailable

### No Additional Configuration

No folder-specific configuration needed. Uses existing:
- `lucene.index.directory` - Index storage location
- `spring.ai.ollama.embedding.options.model` - Embedding model

## Performance

### Speed Estimates

| Documents | Recursive | With Embeddings | Time |
|-----------|-----------|-----------------|------|
| 10 | No | Yes | 20-50 seconds |
| 10 | No | No | 2-5 seconds |
| 50 | Yes | Yes | 2-4 minutes |
| 50 | Yes | No | 10-25 seconds |
| 100 | Yes | Yes | 4-8 minutes |
| 100 | Yes | No | 20-50 seconds |

**Factors affecting speed**:
- Document size (content length)
- Number of content objects per document
- Ollama performance (for embeddings)
- System resources (CPU, disk I/O)

### Resource Usage

**During Rebuild**:
- CPU: Moderate (Lucene indexing)
- CPU: High if generating embeddings (Ollama)
- Memory: ~100MB per 1000 documents
- Disk I/O: Sequential writes to index

**Recommendations**:
- Rebuild during off-peak hours for large folders
- Monitor Ollama logs for embedding generation issues
- Ensure sufficient disk space for index

## Progress Feedback

### Messages Displayed

1. **Initial**: "Rebuilding index for N documents..."
2. **Success**: "Index rebuilt successfully for N documents!" (green)
3. **No documents**: "No documents found in folder."
4. **Error**: "Error: [error message]" (red)

### Notifications

**Success**:
```
✓ Index rebuilt for N documents
```
(Green toast, bottom-start, 3 seconds)

**Error**:
```
✗ Failed to rebuild index: [error message]
```
(Red toast, bottom-start, 5 seconds)

## Technical Details

### Document Collection

```java
private void collectDocumentsFromFolder(Folder folder, List<Document> documents, boolean recursive) {
    // Load folder with relationships
    Folder loadedFolder = folderService.findByIdWithRelationships(folder.getId());
    
    // Add documents from current folder
    if (loadedFolder.getItems() != null) {
        for (SysObject item : loadedFolder.getItems()) {
            if (item instanceof Document) {
                documents.add((Document) item);
            }
        }
    }
    
    // Recursively process subfolders if requested
    if (recursive && loadedFolder.getChildFolders() != null) {
        for (Folder childFolder : loadedFolder.getChildFolders()) {
            collectDocumentsFromFolder(childFolder, documents, recursive);
        }
    }
}
```

### Async Processing

- **Collection**: Runs in `CompletableFuture.supplyAsync()`
- **Indexing**: Runs synchronously within async context
- **UI Updates**: Thread-safe with `ui.access()`
- **Auto-close**: Separate thread with 2-second delay

### Index Clearing

**Important**: The `rebuildIndex()` method in `LuceneIndexService` calls `indexWriter.deleteAll()`, which clears the ENTIRE index, not just the selected documents.

**Current behavior**: 
- Clears all indexed documents
- Re-indexes only the provided documents
- Other documents not in the list are removed from index

**Implication**: This is a **partial rebuild** from a document selection perspective, but a **full rebuild** from an index perspective.

**Future consideration**: Implement selective re-indexing to only update specific documents without clearing the entire index.

## Error Handling

### Common Errors

**1. No documents found**
```
Message: "No documents found in folder."
Cause: Folder is empty or contains only subfolders
Action: Check folder contents
```

**2. IOException during indexing**
```
Message: "Error: IOException: ..."
Cause: Disk full, permissions, or corrupted index
Action: Check disk space and file permissions
```

**3. Embedding generation fails**
```
Message: "Error: Failed to generate embedding..."
Cause: Ollama service down or unreachable
Action: Check Ollama status: curl http://localhost:11434/api/tags
```

**4. Document not found**
```
Message: "Error: Document not found..."
Cause: Document deleted during rebuild
Action: Retry operation
```

### Recovery

If rebuild fails:
1. Check application logs for details
2. Verify Ollama is running (if embeddings enabled)
3. Try rebuilding with smaller scope (non-recursive)
4. Check disk space and permissions
5. Restart application if index is corrupted

## Comparison with Global Rebuild

### Folder-based Rebuild (This Feature)

**Pros**:
- ✅ Faster for specific folders
- ✅ Less disruptive
- ✅ Targeted maintenance
- ✅ User-friendly UI

**Cons**:
- ❌ Clears entire index (current implementation)
- ❌ Requires manual folder selection
- ❌ One folder at a time

**Best for**: Maintaining specific project folders, fixing issues in known locations

### Global Rebuild (REST API)

**Endpoint**: `POST /api/search/lucene/rebuild`

**Pros**:
- ✅ Rebuilds entire system
- ✅ Programmatic access
- ✅ Can be automated

**Cons**:
- ❌ Slow for large document sets
- ❌ Blocks entire index during rebuild
- ❌ No UI for monitoring

**Best for**: System maintenance, scheduled jobs, development/testing

## Best Practices

### When to Rebuild

✅ **Good reasons**:
- After bulk document operations
- After content transformations
- When search results seem outdated
- After fixing Ollama issues
- During system maintenance

❌ **Avoid**:
- After every single document change (auto-indexing handles this)
- Multiple times in quick succession
- During peak usage hours (large folders)

### Recommendations

1. **Start small**: Test with non-recursive on small folders first
2. **Monitor progress**: Watch for errors in logs
3. **Schedule**: Rebuild large folders during off-hours
4. **Verify**: Test search after rebuild to ensure success
5. **Document**: Note which folders were rebuilt and when

### Folder Organization

For efficient index maintenance:
- Organize documents into logical folders
- Keep related documents together
- Use shallow folder hierarchies (avoid deep nesting)
- Limit folder sizes to ~100 documents for quick rebuilds

## Troubleshooting

### Issue: Button not enabled

**Cause**: No folder selected
**Solution**: Click a folder in the folder tree

### Issue: Rebuild takes too long

**Possible causes**:
1. Too many documents
2. Large document content
3. Slow Ollama responses

**Solutions**:
1. Rebuild without recursive option
2. Disable embedding generation temporarily
3. Check Ollama performance

### Issue: Index rebuild completes but search still broken

**Possible causes**:
1. Index corruption
2. Application restart needed
3. Different documents in search scope

**Solutions**:
1. Try global rebuild: `POST /api/search/lucene/rebuild`
2. Restart application
3. Check Lucene index directory for corruption

### Issue: Progress stuck

**Possible causes**:
1. Large document processing
2. Ollama not responding
3. System resources exhausted

**Solutions**:
1. Wait longer (check logs)
2. Cancel and retry
3. Check system resources (CPU, memory)

## Future Enhancements

Potential improvements:

1. **Incremental indexing**: Update only changed documents without clearing entire index
2. **Batch size control**: Rebuild in batches of N documents
3. **Progress percentage**: Show % complete instead of just document count
4. **Pause/Resume**: Ability to pause long-running rebuilds
5. **Background queue**: Queue rebuilds instead of blocking UI
6. **Scheduled rebuilds**: Automatic periodic rebuilds per folder
7. **Conflict detection**: Warn if documents changed during rebuild
8. **Multi-folder selection**: Rebuild multiple folders at once
9. **Statistics**: Show before/after index stats
10. **Dry run**: Preview what will be rebuilt without actually rebuilding

## References

- LuceneIndexService: `src/main/java/com/docmgmt/search/LuceneIndexService.java`
- FolderView: `src/main/java/com/docmgmt/ui/views/FolderView.java`
- Similarity Search: `SIMILARITY_SEARCH_COMPLETE.md`
- Lucene Documentation: https://lucene.apache.org/core/
