# Word to PDF Transformer

A transformer component that converts Microsoft Word documents (.doc and .docx) to PDF format.

## Overview

The `WordToPdfTransformer` is a Spring-managed component (`@Component`) that implements the `ContentTransformer` interface. It uses:
- **Apache POI** to read Word documents (.doc and .docx)
- **Apache PDFBox** to generate PDF files

## Features

- Supports both legacy Word format (.doc) and modern format (.docx)
- Handles multiple MIME type variants
- Automatic page breaks and text wrapping
- Proper resource management with try-with-resources
- Comprehensive error handling

## Supported MIME Types

### Source (Input)
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (.docx)
- `application/vnd.ms-word.document.macroenabled.12` (.docm)
- `application/msword` (.doc)
- `application/vnd.ms-word` (.doc)
- `application/x-msword` (.doc)

### Target (Output)
- `application/pdf`

## Usage

### As a Spring Bean

The transformer is automatically registered as a Spring component and can be injected:

```java
@Autowired
private WordToPdfTransformer wordToPdfTransformer;

public void convertDocument(Content wordContent) throws IOException, TransformationException {
    if (wordToPdfTransformer.canTransform(wordContent)) {
        byte[] pdfBytes = wordToPdfTransformer.transform(wordContent);
        // Save or process the PDF bytes
    }
}
```

### Via TransformerRegistry

The transformer is also registered in the `TransformerRegistry` and can be accessed through it:

```java
@Autowired
private TransformerRegistry transformerRegistry;

public void convertDocument(Content wordContent) throws IOException, TransformationException {
    String sourceType = wordContent.getContentType();
    String targetType = "application/pdf";
    
    Optional<ContentTransformer> transformer = 
        transformerRegistry.getTransformer(sourceType, targetType);
        
    if (transformer.isPresent()) {
        byte[] pdfBytes = transformer.get().transform(wordContent);
        // Save or process the PDF bytes
    }
}
```

### Direct Usage

You can also use the transformer directly:

```java
WordToPdfTransformer transformer = new WordToPdfTransformer();

// Create a Content object with Word document bytes
Content wordContent = new Content();
wordContent.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
wordContent.setContentBytes(wordDocBytes);

// Transform to PDF
byte[] pdfBytes = transformer.transform(wordContent);
```

## Implementation Details

### Text Extraction

- **DOCX**: Extracts text from paragraphs using Apache POI's XWPFDocument
- **DOC**: Extracts text using Apache POI's WordExtractor for legacy format

### PDF Generation

- Uses standard Letter size (8.5" x 11")
- 50-point margins on all sides
- 12-point Helvetica font
- 1.5x line spacing
- Automatic line wrapping to fit page width
- Automatic page breaks when content exceeds page height

### Error Handling

The transformer handles various error cases:
- Null or empty content
- Unsupported content types
- Corrupted Word files
- Empty Word documents (creates PDF with placeholder text)

## Limitations

⚠️ **Note**: This is a text-based conversion. The following Word features are **not** preserved:

- Images and embedded objects
- Tables (converted to plain text)
- Text formatting (bold, italic, colors, fonts)
- Headers and footers
- Page numbers
- Comments and tracked changes
- Complex layouts

For production use cases requiring high-fidelity Word-to-PDF conversion, consider using commercial libraries like:
- Documents4j
- Aspose.Words
- Apache POI + iText (more complex but better formatting)

## Testing

Comprehensive unit tests are available in `WordToPdfTransformerTest.java`:

```bash
mvn test -Dtest=WordToPdfTransformerTest
```

Tests cover:
- Simple document conversion
- Empty document handling
- Long documents with multiple pages
- Error cases (null content, empty bytes, unsupported types)
- All supported MIME types

## Example: Convert All Word Documents in a Folder

```java
@Service
public class BulkConverter {
    
    @Autowired
    private WordToPdfTransformer transformer;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    public void convertAllWordDocuments() {
        List<Document> wordDocs = documentRepository.findByContentType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        
        for (Document doc : wordDocs) {
            try {
                Content wordContent = doc.getLatestContent();
                
                if (transformer.canTransform(wordContent)) {
                    byte[] pdfBytes = transformer.transform(wordContent);
                    
                    // Create new PDF version
                    Content pdfContent = new Content();
                    pdfContent.setContentType("application/pdf");
                    pdfContent.setContentBytes(pdfBytes);
                    pdfContent.setDocument(doc);
                    
                    doc.addContent(pdfContent);
                    documentRepository.save(doc);
                    
                    System.out.println("Converted: " + doc.getTitle());
                }
            } catch (Exception e) {
                System.err.println("Failed to convert: " + doc.getTitle() + 
                                   " - " + e.getMessage());
            }
        }
    }
}
```

## Configuration

No additional configuration is required. The transformer is auto-configured as a Spring component.

## Dependencies

Already included in `pom.xml`:

```xml
<!-- Apache POI for Word processing -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-scratchpad</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Apache PDFBox for PDF generation -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>
```

## Related Transformers

- `WordToTextTransformer` - Converts Word documents to plain text
- `PdfToTextTransformer` - Extracts text from PDF files

## Contributing

To add support for additional features:

1. Extend the `convertDocxToPdf()` or `convertDocToPdf()` methods
2. Update the PDF generation logic in `createSimplePdf()`
3. Add corresponding unit tests

## License

This component is part of the Document Management System project.
