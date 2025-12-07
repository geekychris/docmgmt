#!/usr/bin/env python3
"""
Document Management System - Content, Renditions, and Search Demo

This demo shows:
1. Creating documents with indexable content
2. Uploading PDF files (simulated)
3. Using transformation features (via UI - not yet exposed in REST)
4. Searching documents with full-text and fielded search
5. Rebuilding the search index
"""

import requests
from docmgmt_client import DocumentManagementClient
import base64


def create_sample_pdf_bytes() -> bytes:
    """
    Create a minimal valid PDF for demonstration.
    In production, you'd use real PDF files.
    """
    # This is a minimal PDF that says "Hello World"
    pdf_content = b"""%PDF-1.4
1 0 obj
<<
/Type /Catalog
/Pages 2 0 R
>>
endobj
2 0 obj
<<
/Type /Pages
/Kids [3 0 R]
/Count 1
>>
endobj
3 0 obj
<<
/Type /Page
/Parent 2 0 R
/Resources <<
/Font <<
/F1 <<
/Type /Font
/Subtype /Type1
/BaseFont /Helvetica
>>
>>
>>
/MediaBox [0 0 612 792]
/Contents 4 0 R
>>
endobj
4 0 obj
<<
/Length 44
>>
stream
BT
/F1 12 Tf
100 700 Td
(Hello World from PDF!) Tj
ET
endstream
endobj
xref
0 5
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
0000000317 00000 n
trailer
<<
/Size 5
/Root 1 0 R
>>
startxref
410
%%EOF
"""
    return pdf_content


def main():
    """Demonstrate content management, transformations, and search."""
    print("=== Document Management: Content, Renditions & Search Demo ===\n")
    
    client = DocumentManagementClient()
    
    try:
        # ========================================
        # PART 1: Create Documents with Content
        # ========================================
        print("📄 PART 1: Creating Documents with Indexable Content\n")
        
        # Create a document with text content (indexable)
        print("1. Creating document with text content...")
        doc1 = client.create_document(
            name="Spring Framework Tutorial",
            document_type="ARTICLE",
            description="A comprehensive guide to Spring Boot development",
            keywords="spring boot java framework web",
            tags=["java", "spring", "tutorial", "backend"]
        )
        print(f"   ✓ Created document ID: {doc1['id']}")
        
        # Upload indexable text content
        text_content = """
Spring Framework Tutorial
========================

Spring Boot is a powerful framework for building enterprise Java applications.
It provides features like:
- Dependency Injection
- Auto-configuration
- Embedded servers
- Spring Data for database access
- Spring Security for authentication
        
This makes it ideal for microservices and web applications.
        """.strip()
        
        content1 = client.upload_content_bytes(
            filename="spring_tutorial.txt",
            content_bytes=text_content.encode('utf-8'),
            sys_object_id=doc1['id'],
            store_in_database=True,
            content_type="text/plain"
        )
        print(f"   ✓ Uploaded text content ID: {content1['id']}")
        
        # Create another document
        print("\n2. Creating document with PDF content...")
        doc2 = client.create_document(
            name="Python Best Practices",
            document_type="MANUAL",
            description="Python programming best practices and design patterns",
            keywords="python programming design patterns",
            tags=["python", "best-practices", "programming"]
        )
        print(f"   ✓ Created document ID: {doc2['id']}")
        
        # Upload PDF content (simulated)
        pdf_bytes = create_sample_pdf_bytes()
        content2 = client.upload_content_bytes(
            filename="python_guide.pdf",
            content_bytes=pdf_bytes,
            sys_object_id=doc2['id'],
            store_in_database=True,
            content_type="application/pdf"
        )
        print(f"   ✓ Uploaded PDF content ID: {content2['id']}")
        
        # Create a third document for search testing
        print("\n3. Creating additional document...")
        doc3 = client.create_document(
            name="Database Design Principles",
            document_type="MANUAL",
            description="SQL and NoSQL database design fundamentals",
            keywords="database sql nosql design optimization",
            tags=["database", "sql", "design"]
        )
        
        content3_text = """
Database Design Principles
=========================

Effective database design is crucial for application performance.

Key principles:
1. Normalization - Organize data to reduce redundancy
2. Indexing - Speed up query performance
3. Relationships - Define clear entity relationships
4. Scalability - Plan for growth

Both SQL and NoSQL databases have their place depending on requirements.
        """.strip()
        
        content3 = client.upload_content_bytes(
            filename="database_guide.txt",
            content_bytes=content3_text.encode('utf-8'),
            sys_object_id=doc3['id'],
            store_in_database=True,
            content_type="text/plain"
        )
        print(f"   ✓ Created document ID: {doc3['id']} with text content")
        
        # ========================================
        # PART 2: Transformations & Renditions
        # ========================================
        print("\n\n📝 PART 2: Content Transformations & Renditions\n")
        
        print("NOTE: Content transformation (PDF → text) is currently done via UI.")
        print("Steps to transform PDF content:")
        print("  1. Go to Documents view in the UI")
        print("  2. Select your document")
        print("  3. In the content panel, find your PDF")
        print("  4. Click the ✨ (magic wand) Transform button")
        print("  5. The system will:")
        print("     - Extract text from the PDF using Apache PDFBox")
        print("     - Create a secondary rendition (text/plain)")
        print("     - Mark it as indexable")
        print("     - Add it to the document\n")
        
        print("After transformation, you'll see:")
        contents = client.get_content_by_sysobject(doc2['id'])
        print(f"   Document {doc2['id']} currently has {len(contents)} content item(s)")
        for c in contents:
            print(f"   - {c['name']} ({c.get('contentType', 'unknown')})")
        
        print("\n   After transformation in UI, you'll have:")
        print("   - python_guide.pdf (application/pdf) [Primary, Not Indexable]")
        print("   - python_guide.pdf.plain (text/plain) [Secondary, Indexable]")
        
        # ========================================
        # PART 3: Search Index Management
        # ========================================
        print("\n\n🔍 PART 3: Search Index Management\n")
        
        print("1. Rebuilding search index...")
        result = client.rebuild_search_index()
        print(f"   ✓ {result}")
        
        print("\n2. Checking index statistics...")
        stats = client.get_search_stats()
        print(f"   Documents indexed: {stats.get('documentCount', 0)}")
        print(f"   Max doc ID: {stats.get('maxDoc', 0)}")
        print(f"   Deleted docs: {stats.get('deletedDocs', 0)}")
        
        # ========================================
        # PART 4: Search Operations
        # ========================================
        print("\n\n🔎 PART 4: Searching Documents\n")
        
        # Simple search
        print("1. Simple search for 'spring'...")
        results = client.search("spring", limit=10)
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')} (score: {r['score']:.2f})")
        
        # Search in description
        print("\n2. Search for 'framework'...")
        results = client.search("framework", limit=10)
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
        
        # Search in content (text was indexed)
        print("\n3. Search for 'microservices' (in content)...")
        results = client.search("microservices", limit=10)
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
        
        # Fielded search - search only in tags
        print("\n4. Field-specific search (tags='python')...")
        results = client.search_fields(
            field_queries={"tags": "python"},
            operator="AND",
            limit=10
        )
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
            if r.get('tags'):
                print(f"     Tags: {r['tags']}")
        
        # Multiple field search with AND
        print("\n5. Multi-field search (name='database' AND keywords='sql')...")
        results = client.search_fields(
            field_queries={
                "name": "database",
                "keywords": "sql"
            },
            operator="AND",
            limit=10
        )
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
        
        # Multiple field search with OR
        print("\n6. Multi-field search (name='Spring' OR name='Python' - OR logic)...")
        results = client.search_fields(
            field_queries={
                "name": "Spring Python"  # Search for either term
            },
            operator="OR",
            limit=10
        )
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
        
        # Phrase search
        print("\n7. Phrase search for 'best practices'...")
        results = client.search('"best practices"', limit=10)
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
        
        # Wildcard search
        print("\n8. Wildcard search for 'databa*'...")
        results = client.search("databa*", limit=10)
        print(f"   Found {len(results)} result(s):")
        for r in results:
            print(f"   - Doc {r['documentId']}: {r.get('name', 'N/A')}")
        
        # ========================================
        # PART 5: Working with Renditions
        # ========================================
        print("\n\n📦 PART 5: Inspecting Content Renditions\n")
        
        print(f"1. Content items for document {doc1['id']} (text document):")
        doc1_contents = client.get_content_by_sysobject(doc1['id'])
        for c in doc1_contents:
            print(f"   - {c['name']}")
            print(f"     Type: {c.get('contentType', 'unknown')}")
            print(f"     Storage: {c.get('storageType', 'unknown')}")
            # Note: isPrimary and isIndexable would be in the response if the DTO includes them
        
        print(f"\n2. Content items for document {doc2['id']} (PDF document):")
        doc2_contents = client.get_content_by_sysobject(doc2['id'])
        for c in doc2_contents:
            print(f"   - {c['name']}")
            print(f"     Type: {c.get('contentType', 'unknown')}")
        
        # ========================================
        # Summary
        # ========================================
        print("\n\n" + "="*60)
        print("📊 DEMO SUMMARY")
        print("="*60)
        print(f"✓ Created {3} documents")
        print(f"✓ Uploaded {3} content items")
        print(f"✓ Demonstrated search across multiple fields")
        print(f"✓ Showed fielded search with AND/OR operators")
        print(f"✓ Used phrase and wildcard search")
        print("\n💡 KEY FEATURES:")
        print("   • All document fields are indexed (name, description, keywords, tags)")
        print("   • Text content is automatically indexed")
        print("   • PDF → text transformation via UI (creates indexable renditions)")
        print("   • Primary/secondary rendition support")
        print("   • Full Lucene query syntax support")
        print("   • Field-specific and combined search")
        print("\n📝 NEXT STEPS:")
        print("   1. Transform PDF content via UI to create text renditions")
        print("   2. Rebuild index after transformations")
        print("   3. Search will include PDF text content")
        print("   4. Try advanced Lucene queries: 'spring AND java', 'name:Tutorial', etc.")
        
    except requests.exceptions.RequestException as e:
        print(f"\n✗ Error: {e}")
        if hasattr(e, 'response') and e.response is not None:
            print(f"  Response: {e.response.text}")
        return 1
    
    return 0


if __name__ == "__main__":
    exit(main())
