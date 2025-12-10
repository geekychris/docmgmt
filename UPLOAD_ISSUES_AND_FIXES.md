# Content Upload Issues and Fixes

## Issue 1: TextField Not Editable

**Problem**: The Content Name field shows an X button but text cannot be edited.

**Possible Causes**:
1. Browser/Vaadin cache not cleared
2. Field is being set to read-only elsewhere
3. Vaadin component state issue

**Current Code** (DocumentDetailDialog.java lines 1096-1103):
```java
TextField nameField = new TextField("Content Name");
nameField.setPlaceholder("e.g., Annual Report 2024");
nameField.setRequired(true);
nameField.setWidthFull();
nameField.setReadOnly(false); // Explicitly set as editable
nameField.setClearButtonVisible(true); // Show clear button
nameField.setHelperText("Provide a descriptive name for this content");
nameField.setAutofocus(true); // Focus on this field when dialog opens
```

**Debug Steps**:
1. Clear browser cache completely (Cmd+Shift+R or hard refresh)
2. Try in incognito/private browser window
3. Check browser console for JavaScript errors
4. Add logging to verify field state

**Alternative Fix**: Try using TextArea instead of TextField, or remove setClearButtonVisible:
```java
TextField nameField = new TextField("Content Name");
nameField.setPlaceholder("Enter custom name here");
nameField.setWidthFull();
// Don't set clearButtonVisible - might be causing issues
nameField.setHelperText("Type a descriptive name for this content");
```

## Issue 2: Slow Document Creation with Progress Bar

**Problem**: Creating documents takes multiple seconds with a progress bar.

**Root Cause**: Lucene indexing is likely running synchronously on the main thread.

**Files to Check**:
- `DocumentService.java` - Look for `@Transactional` on create methods
- `LuceneIndexService.java` - Check if indexing is synchronous
- Any listeners that trigger on document creation

**Fix**: Make indexing asynchronous
```java
@Async
public void indexDocument(Document document) {
    // Indexing code here
}
```

Or use `CompletableFuture`:
```java
CompletableFuture.runAsync(() -> {
    luceneIndexService.indexDocument(document);
});
```

**Temporary Workaround**: Disable auto-indexing for document creation:
- Check application.properties for indexing settings
- Look for event listeners that trigger indexing

## Issue 3: Content Not Showing After Upload

**Problem**: After uploading content, the content grid doesn't update.

**Current Refresh Code** (DocumentDetailDialog.java line 1252):
```java
refreshDialog();
```

**The `refreshDialog()` method** (lines 1020-1080) should:
1. Reload document with fresh contents
2. Update content grid items
3. Update content selector items

**Potential Issues**:
1. Transaction not committed before refresh
2. Content not properly saved
3. Grid not properly bound to new data
4. Cache not cleared

**Debug Logging Needed**:
```java
logger.info("Before upload - content count: {}", 
    document.getContents() != null ? document.getContents().size() : 0);
    
// After save
content.setPrimary(isPrimary);
Content savedContent = contentService.save(content);
logger.info("Saved content: id={}, name={}", savedContent.getId(), savedContent.getName());

// After refresh
Document reloaded = documentService.findById(document.getId());
logger.info("After refresh - content count: {}", 
    reloaded.getContents() != null ? reloaded.getContents().size() : 0);
```

**Alternative Fix**: Close and reopen the dialog instead of refresh:
```java
uploadDialog.close();

// Close the parent dialog
close();

// Reopen with fresh data
new DocumentDetailDialog(document, documentService, userService,
    contentService, pluginService, similarityService, 
    fieldExtractionService, fileStoreService).open();
```

## Recommended Immediate Actions

### 1. Fix TextField Editability (CRITICAL)

Remove `setReadOnly()` and `setClearButtonVisible()` - these might be interfering:

```java
TextField nameField = new TextField("Content Name");
nameField.setPlaceholder("Enter custom name");
nameField.setWidthFull();
nameField.setHelperText("Type a descriptive name");
nameField.focus(); // Instead of setAutofocus
```

### 2. Fix Refresh by Closing/Reopening Dialog

Replace line 1252 `refreshDialog();` with:

```java
uploadDialog.close();

// Store the document ID
Long documentId = document.getId();

// Close this dialog
close();

// Reopen with fresh document
Document freshDoc = documentService.findById(documentId);
new DocumentDetailDialog(freshDoc, documentService, userService,
    contentService, pluginService, similarityService, 
    fieldExtractionService, fileStoreService).open();

Notification.show("Content uploaded and dialog refreshed", 
    2000, Notification.Position.BOTTOM_START)
    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
```

### 3. Add Comprehensive Logging

Add debug logging throughout the upload process:

```java
logger.info("=== UPLOAD START ===");
logger.info("Document: {} (id={})", document.getName(), document.getId());
logger.info("Custom name: {}", nameField.getValue());
logger.info("Rendition type: {}", renditionType.getValue());
logger.info("Current content count: {}", 
    document.getContents() != null ? document.getContents().size() : 0);

// After save
logger.info("Content saved: id={}, name={}, isPrimary={}", 
    content.getId(), content.getName(), content.isPrimary());

// After dialog operations
logger.info("=== UPLOAD COMPLETE ===");
```

## Testing Protocol

1. **Clear browser cache completely**
2. **Restart application** with fresh compile
3. **Open document detail dialog**
4. **Click "Upload Content"**
5. **Verify**:
   - Name field accepts keyboard input
   - Can type and edit text freely
   - Can select Primary/Secondary
   - Upload button works
6. **After upload, verify**:
   - Content appears in grid
   - Notification shows success
   - No errors in browser console
   - Check app.log for any exceptions

## Long-term Solutions

1. **Async Indexing**: Move Lucene indexing to background threads
2. **Optimistic UI Updates**: Update UI immediately, sync in background
3. **WebSocket/Push**: Use Vaadin Push for real-time updates
4. **Cache Busting**: Add cache headers to prevent stale UI
5. **Unit Tests**: Add tests for content upload workflow
