package com.docmgmt.client;

import java.io.IOException;
import java.util.*;

/**
 * Document Management System - Content, Renditions, and Search Demo
 * 
 * This demo shows:
 * 1. Creating documents with indexable content
 * 2. Uploading PDF files
 * 3. Using transformation features (via UI)
 * 4. Searching documents with full-text and fielded search
 * 5. Rebuilding the search index
 */
public class ContentAndSearchDemo {
    
    private static final String BASE_URL = "http://localhost:8082/docmgmt/api";
    
    public static void main(String[] args) {
        System.out.println("=== Document Management: Content, Renditions & Search Demo ===\n");
        
        DocumentManagementClient client = new DocumentManagementClient(BASE_URL);
        
        try {
            // ========================================
            // PART 1: Create Documents with Content
            // ========================================
            System.out.println("📄 PART 1: Creating Documents with Indexable Content\n");
            
            // Create a document with text content (indexable)
            System.out.println("1. Creating document with text content...");
            Map<String, Object> doc1 = client.createDocument(
                "Spring Framework Tutorial",
                "ARTICLE",
                "A comprehensive guide to Spring Boot development",
                null,  // author (deprecated)
                "spring boot java framework web",
                Arrays.asList("java", "spring", "tutorial", "backend")
            );
            Long doc1Id = ((Number) doc1.get("id")).longValue();
            System.out.println("   ✓ Created document ID: " + doc1Id);
            
            // Upload indexable text content
            String textContent = """
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
                """.trim();
            
            Map<String, Object> content1 = client.uploadContentBytes(
                "spring_tutorial.txt",
                textContent.getBytes(),
                doc1Id,
                true,  // store in database
                null,  // file store ID
                "text/plain"
            );
            System.out.println("   ✓ Uploaded text content ID: " + content1.get("id"));
            
            // Create another document
            System.out.println("\n2. Creating document with PDF content...");
            Map<String, Object> doc2 = client.createDocument(
                "Python Best Practices",
                "MANUAL",
                "Python programming best practices and design patterns",
                null,
                "python programming design patterns",
                Arrays.asList("python", "best-practices", "programming")
            );
            Long doc2Id = ((Number) doc2.get("id")).longValue();
            System.out.println("   ✓ Created document ID: " + doc2Id);
            
            // Upload PDF content (simulated)
            byte[] pdfBytes = createSamplePdfBytes();
            Map<String, Object> content2 = client.uploadContentBytes(
                "python_guide.pdf",
                pdfBytes,
                doc2Id,
                true,
                null,
                "application/pdf"
            );
            System.out.println("   ✓ Uploaded PDF content ID: " + content2.get("id"));
            
            // Create a third document for search testing
            System.out.println("\n3. Creating additional document...");
            Map<String, Object> doc3 = client.createDocument(
                "Database Design Principles",
                "MANUAL",
                "SQL and NoSQL database design fundamentals",
                null,
                "database sql nosql design optimization",
                Arrays.asList("database", "sql", "design")
            );
            Long doc3Id = ((Number) doc3.get("id")).longValue();
            
            String content3Text = """
                Database Design Principles
                =========================
                
                Effective database design is crucial for application performance.
                
                Key principles:
                1. Normalization - Organize data to reduce redundancy
                2. Indexing - Speed up query performance
                3. Relationships - Define clear entity relationships
                4. Scalability - Plan for growth
                
                Both SQL and NoSQL databases have their place depending on requirements.
                """.trim();
            
            client.uploadContentBytes(
                "database_guide.txt",
                content3Text.getBytes(),
                doc3Id,
                true,
                null,
                "text/plain"
            );
            System.out.println("   ✓ Created document ID: " + doc3Id + " with text content");
            
            // ========================================
            // PART 2: Transformations & Renditions
            // ========================================
            System.out.println("\n\n📝 PART 2: Content Transformations & Renditions\n");
            
            System.out.println("NOTE: Content transformation (PDF → text) is currently done via UI.");
            System.out.println("Steps to transform PDF content:");
            System.out.println("  1. Go to Documents view in the UI");
            System.out.println("  2. Select your document");
            System.out.println("  3. In the content panel, find your PDF");
            System.out.println("  4. Click the ✨ (magic wand) Transform button");
            System.out.println("  5. The system will:");
            System.out.println("     - Extract text from the PDF using Apache PDFBox");
            System.out.println("     - Create a secondary rendition (text/plain)");
            System.out.println("     - Mark it as indexable");
            System.out.println("     - Add it to the document\n");
            
            System.out.println("After transformation, you'll see:");
            List<Map<String, Object>> contents = client.getContentBySysObject(doc2Id);
            System.out.println("   Document " + doc2Id + " currently has " + contents.size() + " content item(s)");
            for (Map<String, Object> c : contents) {
                System.out.println("   - " + c.get("name") + " (" + c.getOrDefault("contentType", "unknown") + ")");
            }
            
            System.out.println("\n   After transformation in UI, you'll have:");
            System.out.println("   - python_guide.pdf (application/pdf) [Primary, Not Indexable]");
            System.out.println("   - python_guide.pdf.plain (text/plain) [Secondary, Indexable]");
            
            // ========================================
            // PART 3: Search Index Management
            // ========================================
            System.out.println("\n\n🔍 PART 3: Search Index Management\n");
            
            System.out.println("1. Rebuilding search index...");
            String result = client.rebuildSearchIndex();
            System.out.println("   ✓ " + result);
            
            System.out.println("\n2. Checking index statistics...");
            Map<String, Object> stats = client.getSearchStats();
            System.out.println("   Documents indexed: " + stats.getOrDefault("documentCount", 0));
            System.out.println("   Max doc ID: " + stats.getOrDefault("maxDoc", 0));
            System.out.println("   Deleted docs: " + stats.getOrDefault("deletedDocs", 0));
            
            // ========================================
            // PART 4: Search Operations
            // ========================================
            System.out.println("\n\n🔎 PART 4: Searching Documents\n");
            
            // Simple search
            System.out.println("1. Simple search for 'spring'...");
            List<Map<String, Object>> results = client.search("spring", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s (score: %.2f)%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"),
                    ((Number) r.get("score")).floatValue());
            }
            
            // Search in description
            System.out.println("\n2. Search for 'framework'...");
            results = client.search("framework", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"));
            }
            
            // Search in content
            System.out.println("\n3. Search for 'microservices' (in content)...");
            results = client.search("microservices", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"));
            }
            
            // Fielded search - tags only
            System.out.println("\n4. Field-specific search (tags='python')...");
            Map<String, String> fieldQueries = new HashMap<>();
            fieldQueries.put("tags", "python");
            results = client.searchFields(fieldQueries, "AND", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"));
                if (r.containsKey("tags")) {
                    System.out.println("     Tags: " + r.get("tags"));
                }
            }
            
            // Multiple field search with AND
            System.out.println("\n5. Multi-field search (name='database' AND keywords='sql')...");
            fieldQueries = new HashMap<>();
            fieldQueries.put("name", "database");
            fieldQueries.put("keywords", "sql");
            results = client.searchFields(fieldQueries, "AND", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"));
            }
            
            // Phrase search
            System.out.println("\n6. Phrase search for 'best practices'...");
            results = client.search("\"best practices\"", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"));
            }
            
            // Wildcard search
            System.out.println("\n7. Wildcard search for 'databa*'...");
            results = client.search("databa*", 10);
            System.out.println("   Found " + results.size() + " result(s):");
            for (Map<String, Object> r : results) {
                System.out.printf("   - Doc %d: %s%n",
                    ((Number) r.get("documentId")).longValue(),
                    r.getOrDefault("name", "N/A"));
            }
            
            // ========================================
            // PART 5: Working with Renditions
            // ========================================
            System.out.println("\n\n📦 PART 5: Inspecting Content Renditions\n");
            
            System.out.println("1. Content items for document " + doc1Id + " (text document):");
            List<Map<String, Object>> doc1Contents = client.getContentBySysObject(doc1Id);
            for (Map<String, Object> c : doc1Contents) {
                System.out.println("   - " + c.get("name"));
                System.out.println("     Type: " + c.getOrDefault("contentType", "unknown"));
                System.out.println("     Storage: " + c.getOrDefault("storageType", "unknown"));
            }
            
            System.out.println("\n2. Content items for document " + doc2Id + " (PDF document):");
            List<Map<String, Object>> doc2Contents = client.getContentBySysObject(doc2Id);
            for (Map<String, Object> c : doc2Contents) {
                System.out.println("   - " + c.get("name"));
                System.out.println("     Type: " + c.getOrDefault("contentType", "unknown"));
            }
            
            // ========================================
            // Summary
            // ========================================
            System.out.println("\n\n" + "=".repeat(60));
            System.out.println("📊 DEMO SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("✓ Created 3 documents");
            System.out.println("✓ Uploaded 3 content items");
            System.out.println("✓ Demonstrated search across multiple fields");
            System.out.println("✓ Showed fielded search with AND/OR operators");
            System.out.println("✓ Used phrase and wildcard search");
            System.out.println("\n💡 KEY FEATURES:");
            System.out.println("   • All document fields are indexed (name, description, keywords, tags)");
            System.out.println("   • Text content is automatically indexed");
            System.out.println("   • PDF → text transformation via UI (creates indexable renditions)");
            System.out.println("   • Primary/secondary rendition support");
            System.out.println("   • Full Lucene query syntax support");
            System.out.println("   • Field-specific and combined search");
            System.out.println("\n📝 NEXT STEPS:");
            System.out.println("   1. Transform PDF content via UI to create text renditions");
            System.out.println("   2. Rebuild index after transformations");
            System.out.println("   3. Search will include PDF text content");
            System.out.println("   4. Try advanced Lucene queries: 'spring AND java', 'name:Tutorial', etc.");
            
        } catch (IOException e) {
            System.err.println("\n✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Create a minimal valid PDF for demonstration.
     */
    private static byte[] createSamplePdfBytes() {
        // Minimal PDF that says "Hello World from PDF!"
        String pdf = "%PDF-1.4\n" +
            "1 0 obj\n<<\n/Type /Catalog\n/Pages 2 0 R\n>>\nendobj\n" +
            "2 0 obj\n<<\n/Type /Pages\n/Kids [3 0 R]\n/Count 1\n>>\nendobj\n" +
            "3 0 obj\n<<\n/Type /Page\n/Parent 2 0 R\n" +
            "/Resources <<\n/Font <<\n/F1 <<\n/Type /Font\n/Subtype /Type1\n" +
            "/BaseFont /Helvetica\n>>\n>>\n>>\n/MediaBox [0 0 612 792]\n" +
            "/Contents 4 0 R\n>>\nendobj\n" +
            "4 0 obj\n<<\n/Length 44\n>>\nstream\n" +
            "BT\n/F1 12 Tf\n100 700 Td\n(Hello World from PDF!) Tj\nET\n" +
            "endstream\nendobj\n" +
            "xref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n" +
            "0000000058 00000 n\n0000000115 00000 n\n0000000317 00000 n\n" +
            "trailer\n<<\n/Size 5\n/Root 1 0 R\n>>\n" +
            "startxref\n410\n%%EOF\n";
        
        return pdf.getBytes();
    }
}
