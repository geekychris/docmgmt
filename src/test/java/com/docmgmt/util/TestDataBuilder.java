package com.docmgmt.util;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.FileStore;
import com.docmgmt.model.SysObject;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for building test data objects
 */
public class TestDataBuilder {

    /**
     * Create a test FileStore
     * 
     * @param id Optional ID, null for new entities
     * @param name Store name
     * @param rootPath Root path
     * @param status Status of the FileStore
     * @return A FileStore entity
     */
    public static FileStore createFileStore(Long id, String name, String rootPath, FileStore.Status status) {
        return FileStore.builder()
                .id(id)
                .name(name != null ? name : "test-store-" + UUID.randomUUID().toString().substring(0, 8))
                .rootPath(rootPath != null ? rootPath : "./target/test-temp/" + UUID.randomUUID().toString())
                .status(status != null ? status : FileStore.Status.ACTIVE)
                .contents(new HashSet<>())
                .build();
    }

    /**
     * Create a test Document
     * 
     * @param id Optional ID, null for new entities
     * @param name Document name
     * @param type Document type
     * @param majorVersion Major version
     * @param minorVersion Minor version
     * @return A Document entity
     */
    public static Document createDocument(Long id, String name, Document.DocumentType type, Integer majorVersion, Integer minorVersion) {
        Document document = Document.builder()
                .description("Test document description")
                .documentType(type != null ? type : Document.DocumentType.OTHER)
                .author("Test Author")
                .keywords("test, document, keywords")
                .build();
        
        document.setId(id);
        document.setName(name != null ? name : "test-doc-" + UUID.randomUUID().toString().substring(0, 8));
        document.setMajorVersion(majorVersion != null ? majorVersion : 1);
        document.setMinorVersion(minorVersion != null ? minorVersion : 0);
        
        // Add some test tags
        document.addTag("test");
        document.addTag("sample");
        
        return document;
    }

    /**
     * Create a test Content in database
     * 
     * @param id Optional ID, null for new entities
     * @param name Content name
     * @param contentType MIME type
     * @param sysObject Parent SysObject
     * @return A Content entity stored in database
     */
    public static Content createDatabaseContent(Long id, String name, String contentType, SysObject sysObject) {
        Content content = Content.builder()
                .id(id)
                .name(name != null ? name : "test-content-" + UUID.randomUUID().toString().substring(0, 8))
                .contentType(contentType != null ? contentType : "text/plain")
                .content("Test content data".getBytes())
                .sysObject(sysObject)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
        
        return content;
    }

    /**
     * Create a test Content in file store
     * 
     * @param id Optional ID, null for new entities
     * @param name Content name
     * @param contentType MIME type
     * @param sysObject Parent SysObject
     * @param fileStore FileStore for storage
     * @return A Content entity stored in file store
     */
    public static Content createFileStoreContent(Long id, String name, String contentType, SysObject sysObject, FileStore fileStore) {
        Content content = Content.builder()
                .id(id)
                .name(name != null ? name : "test-content-" + UUID.randomUUID().toString().substring(0, 8))
                .contentType(contentType != null ? contentType : "text/plain")
                .sysObject(sysObject)
                .fileStore(fileStore)
                .storagePath(UUID.randomUUID().toString() + ".txt")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
        
        return content;
    }
}

