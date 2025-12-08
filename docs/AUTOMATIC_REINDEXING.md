# Automatic Document Reindexing

## Overview

The document management system now automatically reindexes documents whenever they are created, updated, or deleted. This ensures the Lucene search index remains synchronized with the database without requiring manual intervention or UI-triggered reindexing.

## Implementation

### Components

1. **DocumentIndexListener** (`com.docmgmt.listener.DocumentIndexListener`)
   - JPA entity listener that intercepts document persistence events
   - Automatically triggers reindexing on create, update, and delete operations
   - Independent of the UI layer

2. **Transaction Synchronization**
   - Indexing is deferred until after the database transaction commits
   - Ensures all related data (content, tags, etc.) is persisted before indexing
   - Prevents indexing failures due to incomplete data

### How It Works

1. **On Document Creation (`@PostPersist`)**
   - Triggered after a new document is persisted to the database
   - Automatically indexes the document with all its attributes and content

2. **On Document Update (`@PostUpdate`)**
   - Triggered after a document is updated in the database
   - Automatically reindexes the document with updated attributes and content
   - Old index entries are replaced with new ones

3. **On Document Deletion (`@PostRemove`)**
   - Triggered after a document is removed from the database
   - Automatically removes the document from the search index

### Indexed Attributes

The following document attributes are automatically indexed:
- Document name
- Description
- Keywords
- Tags
- All indexable content (text files, etc.)

## Benefits

1. **Always Up-to-Date**: Search results always reflect the current state of documents
2. **No Manual Intervention**: No need for explicit reindex commands or UI actions
3. **UI-Independent**: Works regardless of how documents are modified (UI, API, batch operations)
4. **Transactional Safety**: Indexing only occurs after successful database commits
5. **Error Resilient**: Index failures don't affect database operations

## Usage

No special configuration or API calls are required. The automatic reindexing happens transparently whenever documents are saved or deleted using:

```java
// Service layer
documentService.save(document);        // Automatically triggers reindexing
documentService.delete(documentId);    // Automatically removes from index

// Repository layer (also works)
documentRepository.save(document);
documentRepository.delete(document);
```

## Testing

Integration tests verify automatic reindexing functionality:
- `DocumentIndexListenerTest.testDocumentAutomaticallyIndexedOnCreate()`
- `DocumentIndexListenerTest.testDocumentAutomaticallyReindexedOnUpdate()`
- `DocumentIndexListenerTest.testDocumentAutomaticallyRemovedFromIndexOnDelete()`
- `DocumentIndexListenerTest.testMultipleAttributeChangesReindexed()`

## Configuration

The listener is automatically registered via the `@EntityListeners` annotation on the `Document` entity:

```java
@Entity
@EntityListeners(DocumentIndexListener.class)
public abstract class Document extends SysObject {
    // ...
}
```

## Logging

The listener logs all indexing operations at the DEBUG level:
- Successful reindexing: `Document automatically reindexed after {operation}: {name} (ID: {id})`
- Failed reindexing: `Failed to reindex document after {operation}: {name} (ID: {id})`

Enable DEBUG logging for `com.docmgmt.listener.DocumentIndexListener` to monitor automatic reindexing activity.
