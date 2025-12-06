# Document Management System Clients

This directory contains client implementations for the Document Management System REST API, demonstrating copy-on-write versioning operations.

## Available Clients

### Python Client
Locations:
- `python/docmgmt_client.py` - Main client library
- `python/folder_demo.py` - Folder hierarchy demo

### Java Client
Location: `java/src/main/java/com/docmgmt/client/DocumentManagementClient.java`

## Prerequisites

- Document Management System running at `http://localhost:8082/docmgmt` (or `http://[::1]:8082/docmgmt` for IPv6)
- Python 3.7+ (for Python client)
- Java 17+ and Maven (for Java client)

## Python Client

### Installation

```bash
cd python
pip install -r requirements.txt
```

### Running the Copy-on-Write Demo

```bash
python docmgmt_client.py
```

### Running the Folder Hierarchy Demo

```bash
python folder_demo.py
```

This will execute a complete copy-on-write versioning demo:
1. Create document v1.0
2. Add content to v1.0
3. Verify v1.0 content
4. Create v2.0 (major version)
5. Verify content is initially shared
6. Update content in v2.0 (trigger copy-on-write)
7. Verify versions are now independent
8. Check version history
9. Create v2.1 (minor version)

### Using as a Library

```python
from docmgmt_client import DocumentManagementClient

client = DocumentManagementClient("http://localhost:8082/docmgmt/api")

# Create document
doc = client.create_document(
    name="My Document",
    document_type="REPORT",
    description="Test document"
)

# Add content
content = client.upload_content_bytes(
    filename="test.txt",
    content_bytes=b"Hello, World!",
    sys_object_id=doc['id'],
    store_in_database=True
)

# Create new version
doc_v2 = client.create_major_version(doc['id'])

# Check version history
history = client.get_version_history(doc_v2['id'])
```

## Java Client

### Building

```bash
cd java
mvn clean install
```

### Running the Tests

```bash
mvn test
```

The test suite (`DocumentManagementClientTest`) demonstrates the same copy-on-write workflow as the Python demo.

### Using as a Library

```java
import com.docmgmt.client.DocumentManagementClient;
import java.util.HashMap;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocumentManagementClient client = new DocumentManagementClient();
        
        // Create document
        Map<String, Object> document = new HashMap<>();
        document.put("name", "My Document");
        document.put("documentType", "REPORT");
        document.put("description", "Test document");
        
        Map<String, Object> created = client.createDocument(document);
        long docId = ((Number) created.get("id")).longValue();
        
        // Add content
        byte[] content = "Hello, World!".getBytes();
        Map<String, Object> uploaded = client.uploadContent(
            "test.txt",
            content,
            docId,
            true,  // store in database
            null
        );
        
        // Create new version
        Map<String, Object> version2 = client.createMajorVersion(docId);
        
        // Check version history
        List<Map<String, Object>> history = 
            client.getVersionHistory(((Number) version2.get("id")).longValue());
    }
}
```

### Including in Your Project

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.docmgmt</groupId>
    <artifactId>docmgmt-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## API Documentation

For detailed REST API documentation, see: `../docs/API_VERSIONING_GUIDE.md`

## Client Features

Both clients support:

### Document Operations
- Create document
- Get document by ID
- Update document
- Delete document

### Versioning Operations
- Create major version (e.g., 1.0 → 2.0)
- Create minor version (e.g., 1.0 → 1.1)
- Get version history

### Folder Operations
- Create folders
- Create hierarchical folder structures (parent/child)
- Add documents to folders
- Add folders to folders (nesting)
- Get folder items
- Get folder hierarchy
- Document can exist in multiple folders

### Content Operations
- Upload content (database or file store)
- Download content
- Get content metadata
- Get all content for a document
- Delete content

## Copy-on-Write Behavior

### How It Works

1. **Initial Version**: Create document v1.0 with content
   - Content stored in database or file store

2. **Create New Version**: Create v2.0 from v1.0
   - Content entities are **cloned** (different IDs)
   - Content **data is initially shared** (same bytes/file)
   - This is efficient - no data duplication yet

3. **Modify Content**: Update content in v2.0
   - Upload new content to v2.0
   - v2.0 now has **independent data**
   - v1.0 content remains **unchanged** ✓

4. **Result**: True copy-on-write
   - Efficient sharing until modification
   - Independent content after modification
   - Original versions are never affected

### Storage Types

**Database Content**:
- Automatically independent when modified
- Byte arrays are separate in memory

**File Store Content**:
- Initially shares same physical file (same storage path)
- Manual copy-on-write: upload new file to create independence
- Both files exist independently on disk

## Example Output

```
=== Document Management System Copy-on-Write Demo ===

1. Creating document v1.0...
   Created: User Manual (ID: 1, Version: 1.0)

2. Adding content to v1.0...
   Uploaded: manual.txt (ID: 10, Storage: DATABASE)

3. Verifying v1.0 content...
   Content: User Manual Version 1.0
This is the original content.

4. Creating v2.0 (major version)...
   Created: User Manual (ID: 2, Version: 2.0)
   Parent Version ID: 1

5. Checking v2.0 content (initially shared)...
   Found 1 content item(s)
   Content ID: 11 (cloned from 10)
   Content: User Manual Version 1.0
This is the original content.
   ✓ Initially shares same data as v1.0

6. Updating content in v2.0 (triggering copy-on-write)...
   Uploaded: manual.txt (ID: 12)

7. Verifying copy-on-write (versions are now independent)...
   v1.0 content: User Manual Version 1.0
This is the original...
   v2.0 content: User Manual Version 2.0
This version has significa...
   ✓ Copy-on-write successful! Versions are independent.

8. Checking version history...
   Found 2 version(s):
     - v2.0: ID=2 (LATEST)
     - v1.0: ID=1

9. Creating v2.1 (minor version)...
   Created: User Manual (ID: 3, Version: 2.1)
   v2.1 has 2 content item(s) (inherited from v2.0)

=== Demo Complete ===
```

## Troubleshooting

### Connection Refused
Ensure the Document Management System is running:
```bash
cd /path/to/docmgmt
mvn spring-boot:run
```

### Port or Context Path Changes
If the server is running on a different port or context path, update the base URL:

Python:
```python
client = DocumentManagementClient("http://localhost:8081/docmgmt/api")
```

Java:
```java
DocumentManagementClient client = new DocumentManagementClient("http://localhost:8081/docmgmt/api");
```

Note: If you encounter connection issues, the Java client defaults to IPv6 (`[::1]`). The application listens on both IPv4 and IPv6, but if you have multiple services on the same port, you may need to explicitly use the IPv6 address: `http://[::1]:8082/docmgmt/api`

### Java Version Issues
Ensure Java 17 or later:
```bash
java -version
```

### Python Dependencies
Install required packages:
```bash
pip install requests
```

## License

Same license as the Document Management System main project.
