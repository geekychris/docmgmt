# Troubleshooting: Content and Renditions Not Showing

## Problems Identified

### 1. **Secondary Renditions Were Never Created**

**Symptom**: When viewing documents imported via CLI, you only see the primary PDF but no text renditions.

**Root Cause**: The `DocumentImportCli` tool uploads files but **does not automatically create text renditions**. See lines 256-258 in `DocumentImportCli.java`:

```java
// Note: Transform and index operations would need separate API endpoints
// For now, these are skipped. The server can handle transformation and indexing
// through its own background processes or triggered separately.
```

**Solution**: 
- Added new REST endpoint `POST /api/content/{id}/transform` to trigger transformation
- Created utility script `transform_pdfs.sh` to batch-transform all PDFs

### 2. **ContentDTO Missing Rendition Metadata**

**Symptom**: Even if renditions existed, the API/UI couldn't display which items are primary vs. secondary, or which are indexable.

**Root Cause**: The `ContentDTO` class was missing key fields:
- `isPrimary` - indicates if this is primary or secondary content
- `isIndexable` - indicates if content should be indexed for search
- `parentRenditionId` - links secondary renditions to their primary
- `secondaryRenditions` - list of secondary renditions for a primary

**Solution**: Updated `ContentDTO` to include these fields and enhanced `fromEntity()` method to populate them.

### 3. **Repository Queries Didn't Load Secondary Renditions**

**Symptom**: Secondary renditions might exist in the database but weren't loaded into memory when querying content.

**Root Cause**: The JPA queries in `ContentRepository` didn't use `LEFT JOIN FETCH` for the `secondaryRenditions` collection, causing lazy loading issues.

**Solution**: Updated queries to eagerly fetch secondary renditions:
```java
@Query("SELECT c FROM Content c " +
       "LEFT JOIN FETCH c.fileStore " +
       "LEFT JOIN FETCH c.secondaryRenditions " +
       "WHERE c.sysObject = :sysObject")
```

### 4. **No Empty Content Issue**

**Important Note**: If documents appear to have "no content", this is likely because:
1. The content bytes are stored in the filesystem (not database), and the file path might be incorrect
2. The database BLOB column is NULL for file-stored content (which is expected)
3. There's an actual data corruption or import issue

To check: Look at the `storage_type` field in ContentDTO - if it says "FILE_STORE", the content should be in the filesystem at the path indicated by the FileStore's rootPath + storagePath.

## How to Fix Existing Data

### Option 1: Use the Transformation Script (Recommended)

Run the provided script to automatically transform all PDFs:

```bash
cd /Users/chris/code/warp_experiments/docmgmt
./transform_pdfs.sh http://localhost:8082/docmgmt
```

This script will:
1. Query all documents in the system
2. Find PDF content items that are primary
3. Check if text renditions already exist
4. Create text renditions for PDFs that don't have them

### Option 2: Manual Transformation via API

If you prefer to transform specific PDFs:

1. Find the content ID from the UI or API:
   ```bash
   curl http://localhost:8082/docmgmt/api/content/by-sysobject/{document_id}
   ```

2. Transform the PDF:
   ```bash
   curl -X POST http://localhost:8082/docmgmt/api/content/{content_id}/transform
   ```

3. Verify the rendition was created:
   ```bash
   curl http://localhost:8082/docmgmt/api/content/{content_id}/renditions
   ```

### Option 3: View Renditions in UI

Once renditions are created, the DocumentDetailView will show:
- **Rendition Column**: Badge showing "Primary" (green) or "Secondary" (gray)
- **Indexable Column**: ✓ (green) for indexable content, ○ (gray) for non-indexable
- All renditions appear as separate rows in the content grid

## Verifying the Fix

### 1. Check that ContentDTO includes rendition fields

```bash
curl http://localhost:8082/docmgmt/api/content/{content_id}
```

Expected response should now include:
```json
{
  "id": 1,
  "name": "document.pdf",
  "contentType": "application/pdf",
  "isPrimary": true,
  "isIndexable": false,
  "parentRenditionId": null,
  ...
}
```

### 2. Check that secondary renditions are created

After transforming a PDF:
```bash
curl http://localhost:8082/docmgmt/api/content/{pdf_content_id}/renditions
```

Expected response:
```json
[
  {
    "id": 1,
    "name": "document.pdf",
    "contentType": "application/pdf",
    "isPrimary": true,
    "isIndexable": false
  },
  {
    "id": 2,
    "name": "document.pdf.txt",
    "contentType": "text/plain",
    "isPrimary": false,
    "isIndexable": true,
    "parentRenditionId": 1
  }
]
```

### 3. Verify in UI

Navigate to a document detail page and check the "Associated Content" section:
- You should see multiple rows if renditions exist
- The "Rendition" column should show "Primary" or "Secondary"
- The "Indexable" column should show appropriate icons
- Text renditions should be viewable (they'll display in a text area)

## Database Verification

If you want to check the database directly:

```sql
-- Check content and their renditions
SELECT 
    c.id,
    c.name,
    c.content_type,
    c.is_primary,
    c.is_indexable,
    c.parent_rendition_id,
    CASE 
        WHEN c.file_store_id IS NULL THEN 'DATABASE'
        ELSE 'FILESTORE'
    END as storage_location
FROM content c
WHERE c.sys_object_id = {your_document_id}
ORDER BY c.is_primary DESC, c.id;

-- Count primary vs secondary content
SELECT 
    is_primary,
    COUNT(*) as count
FROM content
GROUP BY is_primary;

-- Find content with renditions
SELECT 
    primary.name as primary_name,
    COUNT(secondary.id) as rendition_count
FROM content primary
LEFT JOIN content secondary ON secondary.parent_rendition_id = primary.id
WHERE primary.is_primary = true
GROUP BY primary.id, primary.name
HAVING COUNT(secondary.id) > 0;
```

## Future Improvements

To prevent this issue going forward:

1. **Auto-transform on upload**: Modify `ContentController.uploadContent()` to automatically trigger transformation for PDFs:
   ```java
   if (content.getContentType().equals("application/pdf")) {
       try {
           contentService.transformAndAddRendition(content.getId(), null);
       } catch (Exception e) {
           logger.warn("Failed to auto-transform PDF: {}", e.getMessage());
       }
   }
   ```

2. **Background job**: Create a scheduled task to periodically check for PDFs without text renditions and transform them.

3. **Update CLI**: Modify `DocumentImportCli` to call the transformation API after uploading PDFs.

## Common Issues

### "No transformer found for content type"

This means the PDF transformer isn't registered. Check:
1. Is `PdfToTextTransformer` annotated with `@Component`?
2. Is it in the component scan path?
3. Check logs for transformer registration messages

### "Content bytes are empty"

For file-stored content:
1. Check the FileStore's `rootPath` is correct
2. Verify the file exists at: `{rootPath}/{storagePath}`
3. Check file permissions

For database-stored content:
1. Query the database directly to see if the BLOB is NULL
2. Check if the import process actually wrote the bytes

### "LazyInitializationException"

This should be fixed by the query updates, but if you still see it:
1. Ensure you're using the repository methods that include `LEFT JOIN FETCH`
2. Check that the transaction is still active when accessing collections
3. Consider adding `@Transactional(readOnly = true)` to the service method

## Support

If issues persist:
1. Check server logs for error messages
2. Use H2 console to inspect database: http://localhost:8082/docmgmt/h2-console
3. Enable SQL logging by setting `logging.level.org.hibernate.SQL=DEBUG` in application.properties
4. Use the API documentation: http://localhost:8082/docmgmt/api/swagger-ui.html
