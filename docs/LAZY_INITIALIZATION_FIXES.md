# Lazy Initialization Exception Fixes

## Overview

This document describes the fixes applied to resolve `LazyInitializationException` errors that occurred when accessing lazy-loaded Hibernate relationships after the session was closed.

## Issues Identified

### Issue 1: Document Reindexing After Transaction Commit

**Error:**
```
org.hibernate.LazyInitializationException: could not initialize proxy [com.docmgmt.model.FileStore#1] - no Session
```

**Location:** `DocumentIndexListener` when indexing documents after transaction commit

**Root Cause:** 
- The automatic reindexing was triggered after the database transaction committed
- The listener held a reference to a detached `Document` entity
- When indexing tried to access `Content` objects and their `FileStore` relationships, the Hibernate session was already closed

**Solution:**
Modified `DocumentIndexListener` to:
1. Store only the document ID (not the entity reference)
2. Reload the document from the database using `DocumentService.findById()` in the transaction synchronization callback
3. This ensures all lazy relationships are properly loaded in a new transaction context

**Files Changed:**
- `src/main/java/com/docmgmt/listener/DocumentIndexListener.java`

### Issue 2: UI Grid Displaying Content Storage Information

**Error:**
```
org.hibernate.LazyInitializationException: could not initialize proxy [com.docmgmt.model.FileStore#1] - no Session
```

**Location:** `FolderView.openDocumentDetailDialog()` at line 857 in the content grid column

**Root Cause:**
- The UI grid was trying to display the FileStore name for each Content object
- While the `Document` and its `Contents` collection were loaded, the `FileStore` relationship within each `Content` was not initialized
- When the grid rendered, it tried to access `content.getFileStore().getName()` on a lazy proxy

**Solution:**
Enhanced `DocumentService.initializeDocument()` to:
1. Initialize the `Contents` collection
2. For each `Content` object, touch the `FileStore` proxy to initialize it
3. Applied this initialization to all document loading methods (`findById`, `findAll`, `findAllLatestVersions`)

**Files Changed:**
- `src/main/java/com/docmgmt/service/DocumentService.java`

## Implementation Details

### DocumentIndexListener Changes

```java
private void reindexDocument(Document document, String operation) {
    if (indexService != null && documentService != null && document.getId() != null) {
        final Long documentId = document.getId();
        final String documentName = document.getName();
        
        // Register synchronization callback
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // Reload document in new transaction
                        Document freshDocument = documentService.findById(documentId);
                        indexService.indexDocument(freshDocument);
                        // ...
                    } catch (Exception e) {
                        // Error handling
                    }
                }
            });
        }
    }
}
```

### DocumentService Enhancements

```java
private void initializeDocument(Document doc) {
    if (doc.getTags() != null) {
        doc.getTags().size();
    }
    if (doc.getContents() != null) {
        doc.getContents().size();
        // Initialize FileStore for each content object
        doc.getContents().forEach(content -> {
            if (content.getFileStore() != null) {
                content.getFileStore().getName(); // Touch to initialize proxy
            }
        });
    }
    if (doc.getOwner() != null) {
        doc.getOwner().getName();
    }
    if (doc.getAuthors() != null) {
        doc.getAuthors().size();
    }
    if (doc.getParentVersion() != null) {
        doc.getParentVersion().getName();
    }
}
```

## Best Practices Applied

1. **Entity Reloading**: When working across transaction boundaries, reload entities in a fresh transaction rather than using detached entities

2. **Eager Initialization**: Initialize all required lazy relationships within the transaction scope before the session closes

3. **Transaction Synchronization**: Use Spring's `TransactionSynchronization` to defer operations until after transaction commit

4. **Centralized Initialization**: Create helper methods (like `initializeDocument()`) to consistently initialize lazy relationships across all service methods

## Testing

All fixes have been validated with:
- Integration tests in `DocumentIndexListenerTest` (4 tests, all passing)
- Manual UI testing of the folder view and document detail dialog
- Verification that automatic reindexing works with file-stored content

## Related Documentation

- [Automatic Reindexing](./AUTOMATIC_REINDEXING.md) - Details on the automatic document indexing feature
