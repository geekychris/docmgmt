# Troubleshooting: Folder Structure Issues After CLI Import

## Problem Description

After running the CLI import tool, documents appear in a flat list in the UI rather than organized in the expected folder hierarchy that mirrors the source directory structure.

## Root Causes

### 1. **Silent Failures in Document-to-Folder Linking**

**Issue**: The original CLI code called the folder link API but didn't check for errors:

```java
// OLD CODE - No error handling!
if (createdDocument != null && folder != null) {
    String addItemUrl = apiBaseUrl + "/api/folders/" + folder.getId() + "/items/" + createdDocument.getId();
    restTemplate.put(addItemUrl, null);  // Errors ignored!
}
```

If the API call failed (network issue, permission problem, etc.), the document was created but never linked to its folder. The CLI would continue without reporting the problem.

**Fixed**: Now includes proper error handling and statistics tracking:

```java
// NEW CODE - With error handling
if (createdDocument != null && folder != null) {
    try {
        String addItemUrl = apiBaseUrl + "/api/folders/" + folder.getId() + "/items/" + createdDocument.getId();
        ResponseEntity<Void> linkResponse = restTemplate.exchange(
            addItemUrl,
            HttpMethod.PUT,
            null,
            Void.class
        );
        
        if (linkResponse.getStatusCode().is2xxSuccessful()) {
            logger.debug("  ✓ Linked document to folder: {}", folder.getName());
            documentsLinkedToFolders.incrementAndGet();
        } else {
            logger.warn("  ⚠ Failed to link document to folder (HTTP {})", linkResponse.getStatusCode());
            errors.incrementAndGet();
        }
    } catch (Exception e) {
        logger.error("  ✗ Error linking document to folder {}: {}", folder.getName(), e.getMessage());
        errors.incrementAndGet();
    }
}
```

### 2. **Possible API/Database Issues**

Even with proper error handling, linking might fail due to:
- **Transaction rollback**: Database transaction issues
- **Permission errors**: Insufficient API permissions
- **Entity state**: Documents not fully persisted before linking
- **Network issues**: Connection problems during link API call

## Diagnostic Steps

### Step 1: Run the Diagnostic Script

```bash
cd /Users/chris/code/warp_experiments/docmgmt
./check_folder_structure.sh http://localhost:8082/docmgmt
```

This script will show you:
- Total folders vs root folders
- How many documents are in folders vs orphaned
- Sample folder hierarchy
- Specific recommendations

### Step 2: Check CLI Import Logs

Look for these messages in the CLI output:

**Good signs:**
```
✓ Created folder: /some/path
✓ Created document ID: 123
✓ Linked document to folder: some_folder
```

**Warning signs:**
```
⚠ Failed to link document to folder (HTTP 404)
✗ Error linking document to folder xyz: Connection refused
WARNING: 42 document(s) were created but NOT linked to folders!
```

### Step 3: Verify via API

Check if folders were created:
```bash
curl http://localhost:8082/docmgmt/api/folders/roots | jq '.'
```

Check if a specific document is in any folder:
```bash
# Replace {doc_id} with actual document ID
curl http://localhost:8082/docmgmt/api/folders | \
  jq --arg docid "{doc_id}" '[.[] | select(.items != null and (.items | map(.id) | contains([($docid | tonumber)])))]'
```

## Solutions

### Solution 1: Re-import with Fixed CLI

1. **Backup current data** (optional but recommended):
   ```bash
   # H2 database is at ./docmgmt_db.*
   cp docmgmt_db.mv.db docmgmt_db.mv.db.backup
   ```

2. **Clear existing data** (if you want a fresh start):
   - Stop the server
   - Delete `docmgmt_db.mv.db` and `docmgmt_db.trace.db`
   - Restart the server (it will recreate empty tables)

3. **Run import with fixed CLI**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=import \
     -Dspring-boot.run.arguments="--import.root-dir=/path/to/docs --import.api-base-url=http://localhost:8082/docmgmt"
   ```

4. **Watch for the new statistics**:
   ```
   Folders Created:        42
   Documents Created:      156
   Documents Linked:       156    ← Should match documents created!
   Files Uploaded:         156
   Errors:                 0      ← Should be 0 or very low
   ```

### Solution 2: Fix Existing Data (Link Orphaned Documents)

If you have orphaned documents and want to link them to folders without reimporting:

1. **Create a script to fix links** (example):

```bash
#!/bin/bash
# fix_document_links.sh

BASE_URL="http://localhost:8082/docmgmt"
API_URL="${BASE_URL}/api"

# Get all documents
DOCS=$(curl -s "${API_URL}/documents")

# Get all folders
FOLDERS=$(curl -s "${API_URL}/folders")

# For each document, find its intended folder by matching paths
echo "$DOCS" | jq -r '.[] | "\(.id)|\(.description)"' | while IFS='|' read doc_id description; do
    # Extract original file path from description
    if [[ "$description" =~ "Imported from: "(.*)$ ]]; then
        file_path="${BASH_REMATCH[1]}"
        dir_path=$(dirname "$file_path")
        
        # Convert filesystem path to folder path
        folder_path="${dir_path#/path/to/docs}"  # Adjust this to match your root
        folder_path="/${folder_path#/}"
        
        # Find folder with this path
        folder_id=$(echo "$FOLDERS" | jq -r --arg path "$folder_path" '.[] | select(.path == $path) | .id')
        
        if [ -n "$folder_id" ]; then
            echo "Linking document $doc_id to folder $folder_id ($folder_path)"
            curl -X PUT "${API_URL}/folders/${folder_id}/items/${doc_id}"
        else
            echo "No folder found for path: $folder_path"
        fi
    fi
done
```

2. **Run the script**:
   ```bash
   chmod +x fix_document_links.sh
   ./fix_document_links.sh
   ```

### Solution 3: Use UI to Manually Organize

If you have a small number of documents:

1. Navigate to **Folders** view in UI
2. Select a folder in the hierarchy
3. Click **"Link Existing Document"**
4. Choose documents to add to that folder

## Verification

After applying fixes, verify the structure is correct:

### Via UI
1. Go to **Folders** view
2. Expand root folders - you should see child folders
3. Click on a folder - you should see its documents in the right panel

### Via Diagnostic Script
```bash
./check_folder_structure.sh http://localhost:8082/docmgmt
```

Should show:
- ✓ Folder structure looks good!
- All documents are properly organized in folders
- No orphaned documents

### Via API
```bash
# Count documents in folders
curl -s http://localhost:8082/docmgmt/api/folders | \
  jq '[.[] | .items | length] | add'

# Should match total document count
curl -s http://localhost:8082/docmgmt/api/documents | jq 'length'
```

## Common Issues and Fixes

### Issue: "Folder not found" errors during import

**Symptoms**: CLI logs show folder creation succeeded but linking fails with 404

**Cause**: Folder creation might have failed silently, or folder ID is not being cached correctly

**Fix**: 
1. Check folder creation logs for errors
2. Verify folders exist: `curl http://localhost:8082/docmgmt/api/folders/roots`
3. If folders are missing, check for exceptions in server logs

### Issue: All folders are at root level (no hierarchy)

**Symptoms**: Folders exist but have no parent-child relationships

**Cause**: Parent folder linking failed during import

**Fix**: The CLI recursively creates folders with proper parent relationships. Check logs for:
```
✗ Failed to create folder /some/nested/path: ...
```

If you see these errors, there may be an issue with the folder creation logic.

### Issue: Documents in wrong folders

**Symptoms**: Documents exist in folders but not in the expected ones

**Cause**: Path mapping issue in CLI - the folder cache might be wrong

**Fix**: 
1. Re-import with correct `--import.root-dir` path
2. Ensure the root directory matches what was used for folder creation

### Issue: "Connection refused" during linking

**Symptoms**: Many linking errors in CLI logs

**Cause**: Server became overloaded or crashed during import

**Fix**:
1. Check server logs for out-of-memory or crash errors
2. Import in smaller batches using `--import.file-types` to filter
3. Increase server memory: `MAVEN_OPTS="-Xmx2g" mvn spring-boot:run`

## Prevention

To avoid these issues in future imports:

1. **Always check the import statistics**:
   - `Documents Linked` should equal `Documents Created`
   - `Errors` should be 0 or very low

2. **Monitor server logs during import**:
   ```bash
   tail -f logs/spring.log  # Adjust path as needed
   ```

3. **Test with a small directory first**:
   ```bash
   # Import just one subdirectory to test
   --import.root-dir=/path/to/docs/test_folder
   ```

4. **Use the diagnostic script after import**:
   ```bash
   ./check_folder_structure.sh
   ```

## Database Queries for Advanced Debugging

If you need to inspect the database directly:

```sql
-- Connect to H2 console: http://localhost:8082/docmgmt/h2-console
-- JDBC URL: jdbc:h2:file:./docmgmt_db
-- Username: sa
-- Password: password

-- Count folders and their relationships
SELECT 
    COUNT(*) as total_folders,
    COUNT(CASE WHEN parent_folder_id IS NULL THEN 1 END) as root_folders,
    COUNT(CASE WHEN parent_folder_id IS NOT NULL THEN 1 END) as child_folders
FROM folder;

-- Find orphaned documents (not in any folder)
SELECT d.id, d.name, d.description
FROM sys_object d
WHERE d.object_type IN ('Document', 'Article', 'Report')
  AND d.id NOT IN (
    SELECT DISTINCT fi.items_id 
    FROM folder_items fi
  );

-- Count documents per folder
SELECT f.name, f.path, COUNT(fi.items_id) as doc_count
FROM folder f
LEFT JOIN folder_items fi ON f.id = fi.folder_id
GROUP BY f.id, f.name, f.path
ORDER BY doc_count DESC;

-- Show folder hierarchy
SELECT 
    f1.name as root_folder,
    f2.name as child_folder,
    COUNT(fi.items_id) as items_in_child
FROM folder f1
LEFT JOIN folder f2 ON f2.parent_folder_id = f1.id
LEFT JOIN folder_items fi ON fi.folder_id = f2.id
WHERE f1.parent_folder_id IS NULL
GROUP BY f1.id, f1.name, f2.id, f2.name;
```

## Related Documentation

- **CLI Import Guide**: See `QUICK_START_RENDITIONS.md` for import examples
- **API Reference**: http://localhost:8082/docmgmt/api/swagger-ui.html
- **Folder Management**: See `docs/API_EXAMPLES.md` for folder API usage
