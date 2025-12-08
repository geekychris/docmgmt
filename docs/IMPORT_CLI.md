# Document Import CLI

## Overview

The Document Import CLI is a powerful tool for bulk importing files and directories into the Document Management System. It recursively crawls a directory structure, creates documents with primary content renditions, performs PDF-to-text transformations, creates folder hierarchies, and indexes everything for search.

## Features

- **Recursive Directory Crawling** - Walks entire directory trees
- **Folder Structure Preservation** - Mirrors source directory structure
- **Multiple File Type Support** - PDF, DOCX, TXT, MD, RTF, and more
- **Automatic Transformations** - PDF → text conversion with secondary renditions
- **Automatic Indexing** - Full-text search indexing
- **Flexible Storage** - Database or file system storage
- **Progress Tracking** - Real-time statistics and error reporting
- **Configurable** - Many options via command-line arguments

## Usage

### Basic Syntax

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="--import.root-dir=/path/to/docs"
```

### Using JAR File

```bash
java -jar target/document-management-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=import \
  --import.root-dir=/path/to/docs
```

## Command-Line Arguments

### Required

| Argument | Description | Example |
|----------|-------------|---------|
| `--import.root-dir` | Root directory to import | `--import.root-dir=/Users/chris/Documents` |

### Optional

| Argument | Default | Description |
|----------|---------|-------------|
| `--import.file-types` | `pdf,docx,txt,doc,rtf,md` | Comma-separated file extensions |
| `--import.filestore-id` | `null` (database) | File store ID for storage |
| `--import.transform` | `true` | Enable PDF-to-text transformation |
| `--import.index` | `true` | Enable search indexing |
| `--import.create-folders` | `true` | Create folder structure |

## Examples

### Example 1: Import PDFs with Transformations

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/Users/chris/PDFs \
--import.file-types=pdf \
--import.transform=true"
```

**What it does:**
- Imports all PDF files from `/Users/chris/PDFs`
- Creates text renditions from PDFs
- Stores content in database
- Indexes all documents
- Creates folder structure

### Example 2: Import Multiple File Types to File Store

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/Users/chris/Documents \
--import.file-types=pdf,docx,txt,md \
--import.filestore-id=1"
```

**What it does:**
- Imports PDFs, DOCX, TXT, and MD files
- Stores files in file store ID 1
- Transforms PDFs to text
- Preserves directory structure

### Example 3: Import Text Files Only (No Transformation)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/Users/chris/notes \
--import.file-types=txt,md \
--import.transform=false"
```

**What it does:**
- Imports only TXT and MD files
- Skips transformation step
- Stores in database
- Indexes all content

### Example 4: Flat Import (No Folders)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/Users/chris/archive \
--import.create-folders=false"
```

**What it does:**
- Imports files but doesn't create folder hierarchy
- All documents at root level
- Faster for flat structures

## What Gets Created

### For Each File

1. **Document Object**
   - Name: Filename without extension
   - Type: Inferred from extension
   - Description: Source file path
   - Keywords: Extension + "import"
   - Tags: ["imported", extension]
   - Folder: Mapped from directory structure

2. **Primary Content**
   - Original file uploaded
   - Marked as primary rendition
   - Marked as indexable (if text or PDF)
   - Stored in database or file store

3. **Secondary Content** (for PDFs)
   - Text extraction from PDF
   - Marked as secondary rendition
   - Marked as indexable
   - Filename: `originalname.txt`

4. **Folder Structure**
   - Mirrors source directory tree
   - Parent/child relationships preserved
   - Full path stored

5. **Search Index Entry**
   - All fields indexed
   - Content indexed (text + transformed text)
   - Available for immediate search

## File Type Support

### Document Types

| Extension | Document Type | Transformable |
|-----------|---------------|---------------|
| pdf | MANUAL | ✅ Yes (to text) |
| docx, doc | MANUAL | ❌ No (future) |
| txt, md | ARTICLE | ❌ Already text |
| rtf | ARTICLE | ❌ No (future) |
| xlsx, xls, csv | SPREADSHEET | ❌ No |
| pptx, ppt | PRESENTATION | ❌ No |
| jpg, jpeg, png, gif | IMAGE | ❌ No |
| mp4, avi, mov | VIDEO | ❌ No |
| mp3, wav | AUDIO | ❌ No |

### Content Types

Automatically detected MIME types:
- `application/pdf`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- `application/msword`
- `text/plain`
- `text/markdown`
- `image/jpeg`, `image/png`, `image/gif`
- `video/mp4`, `audio/mpeg`
- And more...

## Output Example

```
================================================================================
Document Import CLI
================================================================================
Parsing command line arguments...
Validating configuration...
Using file store: Primary Storage (/data/docmgmt/files)

Configuration:
  Root Directory: /Users/chris/Documents/project
  File Types: [pdf, docx, txt]
  File Store ID: 1
  Transform to Text: true
  Index Documents: true
  Create Folders: true

Starting import from: /Users/chris/Documents/project

  ✓ Created folder: /specs
Processing: /Users/chris/Documents/project/specs/requirements.pdf
  ✓ Transformed to text
  ✓ Indexed document
  ✓ Created document ID: 1

  ✓ Created folder: /design
Processing: /Users/chris/Documents/project/design/architecture.docx
  ✓ Indexed document
  ✓ Created document ID: 2

Processing: /Users/chris/Documents/project/README.txt
  ✓ Indexed document
  ✓ Created document ID: 3

================================================================================
Import Statistics
================================================================================
  Folders Created:     2
  Documents Created:   3
  Files Uploaded:      3
  Transformations:     1
  Errors:              0
================================================================================
Import completed successfully!
```

## Prerequisites

### 1. Create a File Store (Optional)

If you want to use file system storage:

```bash
curl -X POST http://localhost:8082/docmgmt/api/filestores \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Import Storage",
    "rootPath": "/data/docmgmt/imported",
    "status": "ACTIVE"
  }'
```

Note the returned `id` and use it with `--import.filestore-id`.

### 2. Ensure Service is Not Running

The import CLI runs the Spring Boot application in a special profile. Make sure the regular service is not running on the same port.

## Performance

### Large Imports

For large imports:

**Recommendations:**
- Use file store instead of database for large files
- Consider disabling indexing during import (`--import.index=false`)
- Rebuild index afterwards: `POST /api/search/rebuild`
- Monitor available memory (large PDFs)

**Typical Performance:**
- ~100 small files/minute
- ~10-20 PDFs/minute (with transformation)
- ~5-10 large documents/minute

### Memory Considerations

```bash
# Increase heap size for large imports
java -Xmx4g -jar target/document-management-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=import \
  --import.root-dir=/large/directory
```

## Error Handling

The tool continues processing on errors:
- Failed file access: Logged and counted
- Transformation failures: Logged, document still created
- Index failures: Logged, document still created
- Total error count in final statistics

Check logs for details:
```bash
tail -f logs/spring.log
```

## Advanced Usage

### Import from Multiple Directories

```bash
# Import directory 1
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="--import.root-dir=/path/to/dir1"

# Import directory 2
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="--import.root-dir=/path/to/dir2"
```

### Selective Import

```bash
# Only import PDFs
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/archive \
--import.file-types=pdf"

# Only import text files
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/notes \
--import.file-types=txt,md"
```

### Import Without Indexing (Faster)

```bash
# Import first, index later
mvn spring-boot:run -Dspring-boot.run.profiles=import \
  -Dspring-boot.run.arguments="\
--import.root-dir=/data \
--import.index=false"

# Then rebuild index via API
curl -X POST http://localhost:8082/docmgmt/api/search/rebuild
```

## Troubleshooting

### Issue: "Missing required argument: --import.root-dir"

**Solution**: Provide the root directory argument:
```bash
--import.root-dir=/path/to/directory
```

### Issue: "Root directory does not exist"

**Solution**: Verify the path exists and is accessible:
```bash
ls -la /path/to/directory
```

### Issue: "File store not found"

**Solution**: Create file store first or omit `--import.filestore-id` to use database.

### Issue: Out of Memory

**Solution**: Increase heap size:
```bash
export MAVEN_OPTS="-Xmx4g"
mvn spring-boot:run -Dspring-boot.run.profiles=import ...
```

### Issue: Service Already Running

**Solution**: Stop the main service before running import:
```bash
# Find process
lsof -i :8082

# Kill it
kill <PID>
```

## After Import

### Verify Import

```bash
# Check document count
curl http://localhost:8082/docmgmt/api/documents/latest | jq length

# Search imported documents
curl "http://localhost:8082/docmgmt/api/search?q=imported"

# Check folder structure
curl http://localhost:8082/docmgmt/api/folders | jq .
```

### Rebuild Search Index (if needed)

```bash
curl -X POST http://localhost:8082/docmgmt/api/search/rebuild
```

## Best Practices

1. **Test First** - Import a small subset before bulk import
2. **Use File Stores** - For large files or many files
3. **Monitor Progress** - Watch logs for errors
4. **Verify Results** - Check a few documents after import
5. **Backup** - Backup database before large imports
6. **Plan Folders** - Organize source directory structure first

## Scripting the Import

```bash
#!/bin/bash
# bulk-import.sh

# Configuration
ROOT_DIR="/data/to/import"
FILESTORE_ID="1"

# Build application
mvn clean package -DskipTests

# Run import
java -Xmx4g -jar target/document-management-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=import \
  --import.root-dir="$ROOT_DIR" \
  --import.file-types=pdf,docx,txt \
  --import.filestore-id=$FILESTORE_ID \
  --import.transform=true \
  --import.index=true

# Check exit code
if [ $? -eq 0 ]; then
    echo "Import completed successfully"
else
    echo "Import failed"
    exit 1
fi
```

## See Also

- [Content Renditions](CONTENT_RENDITIONS.md) - PDF transformation details
- [Folder Management](../README.md#folders) - Folder structure
- [Search](OPENAPI_SWAGGER.md#search) - Search API
- [File Stores](../README.md#file-stores) - Storage configuration
