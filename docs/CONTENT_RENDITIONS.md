# Content Renditions and Transformer System

## Overview

The Document Management System supports a robust content rendition system that allows documents to have multiple representations of their content. This includes primary renditions (original content) and secondary renditions (transformed versions like text extractions from PDFs).

## Key Concepts

### Primary Renditions
- The original content uploaded by users
- Marked with `isPrimary = true`
- Can have multiple secondary renditions
- When primary content is updated, all secondary renditions are automatically removed

### Secondary Renditions
- Derived versions of primary content (e.g., text extracted from PDF)
- Marked with `isPrimary = false`
- Linked to their parent primary rendition via `parentRendition` relationship
- Automatically deleted when parent primary rendition is deleted (cascade)

### Indexable Content
- Content that contains searchable text
- Marked with `isIndexable = true`
- Typically secondary renditions produced by transformers are marked as indexable
- Can be queried separately for full-text indexing purposes

## Data Model

### Content Entity Fields

```java
// Rendition support
@Column(name = "is_primary", nullable = false)
@Builder.Default
private boolean isPrimary = true;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_rendition_id")
private Content parentRendition;

@OneToMany(mappedBy = "parentRendition", cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private List<Content> secondaryRenditions = new ArrayList<>();

@Column(name = "is_indexable", nullable = false)
@Builder.Default
private boolean isIndexable = false;
```

### Content Methods

- `isSecondaryRendition()` - Check if content is a secondary rendition
- `addSecondaryRendition(Content)` - Add a secondary rendition
- `removeSecondaryRendition(Content)` - Remove a specific secondary rendition
- `removeAllSecondaryRenditions()` - Remove all secondary renditions

## Transformer Plugin Framework

### Architecture

The system uses a plugin-based architecture for content transformation:

1. **ContentTransformer Interface** - Defines the contract for transformers
2. **AbstractContentTransformer** - Base class with common functionality
3. **TransformerRegistry** - Manages and discovers transformer plugins
4. **Concrete Transformers** - Implementations like PdfToTextTransformer

### ContentTransformer Interface

```java
public interface ContentTransformer {
    String getSourceContentType();      // e.g., "application/pdf"
    String getTargetContentType();      // e.g., "text/plain"
    String getName();                   // Descriptive name
    boolean canTransform(Content);      // Check if content is supported
    byte[] transform(Content) throws IOException, TransformationException;
    boolean producesIndexableContent(); // Default: true for text/*
}
```

### Creating a Custom Transformer

```java
@Component
public class MyCustomTransformer extends AbstractContentTransformer {
    
    public MyCustomTransformer() {
        super("application/custom", "text/plain", "Custom to Text Transformer");
    }
    
    @Override
    public byte[] transform(Content sourceContent) throws IOException, TransformationException {
        validateContent(sourceContent);
        byte[] sourceBytes = getContentBytes(sourceContent);
        
        // Transform logic here
        String extractedText = extractTextFromCustomFormat(sourceBytes);
        
        return extractedText.getBytes(StandardCharsets.UTF_8);
    }
}
```

The transformer will be automatically discovered and registered by Spring.

## ContentService API

### Adding Renditions

```java
// Manually add a rendition
Content addRendition(Long primaryContentId, String name, byte[] bytes, 
                    String contentType, boolean isIndexable);

// Transform and add rendition automatically
Content transformAndAddRendition(Long primaryContentId, String targetContentType);
```

### Managing Renditions

```java
// Remove all secondary renditions
void removeSecondaryRenditions(Long primaryContentId);

// Update primary content (auto-removes secondaries)
Content updatePrimaryContent(Long contentId, byte[] newBytes);

// Get all renditions (primary + secondary)
List<Content> getAllRenditions(Long primaryContentId);
```

### Querying

```java
// Get all indexable content for a SysObject
List<Content> getIndexableContent(SysObject sysObject);
```

## Usage Examples

### Example 1: Adding PDF and Creating Text Rendition

```java
// Upload a PDF document
Content pdfContent = Content.builder()
    .name("document.pdf")
    .contentType("application/pdf")
    .content(pdfBytes)
    .sysObject(document)
    .isPrimary(true)
    .isIndexable(false)
    .build();
pdfContent = contentService.save(pdfContent);

// Transform PDF to text and add as secondary rendition
Content textRendition = contentService.transformAndAddRendition(
    pdfContent.getId(), 
    null  // Auto-select transformer
);

// The text rendition is automatically marked as indexable
assert textRendition.isIndexable() == true;
assert textRendition.isPrimary() == false;
assert textRendition.getContentType().equals("text/plain");
```

### Example 2: Updating Primary Content

```java
// When primary content changes, secondary renditions are removed
byte[] updatedPdfBytes = getUpdatedPdf();
contentService.updatePrimaryContent(pdfContent.getId(), updatedPdfBytes);

// All secondary renditions are now gone
List<Content> renditions = contentService.getAllRenditions(pdfContent.getId());
assert renditions.size() == 1;  // Only primary remains
```

### Example 3: Finding Indexable Content

```java
// Get all text-searchable content for a document
List<Content> indexableContent = contentService.getIndexableContent(document);

// Use this content for full-text indexing
for (Content content : indexableContent) {
    byte[] textBytes = content.getContentBytes();
    String text = new String(textBytes, StandardCharsets.UTF_8);
    fullTextIndex.index(document.getId(), text);
}
```

## Included Transformers

### PdfToTextTransformer

Extracts text from PDF documents using Apache PDFBox.

- **Source Type**: `application/pdf` (also supports variants)
- **Target Type**: `text/plain`
- **Indexable**: Yes
- **Features**:
  - Handles multi-page PDFs
  - Maintains text positioning for better readability
  - Detects encrypted PDFs and throws appropriate errors
  - Validates PDF structure before processing

## UI Integration

The content grid in DocumentView displays rendition information:

- **Rendition Column**: Shows "Primary" (green badge) or "Secondary" (gray badge)
- **Indexable Column**: Shows ✓ (green) for indexable content, ○ (gray) for non-indexable
- Both columns make it easy to see at a glance which content is searchable

## Database Schema

### Content Table Additions

```sql
ALTER TABLE content ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE content ADD COLUMN parent_rendition_id BIGINT;
ALTER TABLE content ADD COLUMN is_indexable BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE content ADD CONSTRAINT fk_parent_rendition 
    FOREIGN KEY (parent_rendition_id) REFERENCES content(id) ON DELETE CASCADE;
```

## Best Practices

1. **Always mark primary content appropriately**: Set `isPrimary = true` for original uploads
2. **Use transformers for secondary renditions**: Don't manually create secondary renditions; use the transformer API
3. **Let the system manage lifecycle**: Secondary renditions are automatically cleaned up when primaries change
4. **Mark text content as indexable**: For full-text search, ensure text renditions are marked `isIndexable = true`
5. **Check for existing renditions**: Before creating a new rendition, check if one already exists
6. **Use appropriate transformers**: The system will auto-select the best transformer if you pass `null` for targetContentType

## Testing

The system includes comprehensive tests:

- `ContentRenditionTest` (9 tests): Tests primary/secondary relationships, cascade deletion, indexable queries
- `PdfToTextTransformerTest` (11 tests): Tests PDF transformation, error handling, transformer registration

Run tests with:
```bash
mvn test -Dtest=ContentRenditionTest,PdfToTextTransformerTest
```

## Performance Considerations

1. **Lazy Loading**: Secondary renditions use lazy loading to avoid loading unnecessary data
2. **Cascade Operations**: Deleting primary content cascades to secondaries efficiently
3. **Transformer Caching**: TransformerRegistry caches transformer instances
4. **Batch Processing**: For bulk transformations, process in batches to manage memory

## Future Enhancements

Potential areas for extension:

1. **Additional Transformers**: Word to text, HTML to text, image OCR
2. **Async Transformation**: Queue-based transformation for large files
3. **Rendition Versioning**: Keep rendition history when primary is updated
4. **Transformation Metadata**: Store transformation parameters and timestamps
5. **Smart Re-transformation**: Only re-create renditions when needed
