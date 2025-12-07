package com.docmgmt.util;

import com.docmgmt.model.*;
import com.docmgmt.model.Document.DocumentType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for building test data objects
 */
public class TestDataBuilder {

    /**
     * Create a test User
     * 
     * @param id Optional ID, null for new entities
     * @param username Username
     * @param email Email address
     * @return A User entity
     */
    public static User createUser(Long id, String username, String email) {
        return User.builder()
                .id(id)
                .name(username != null ? username : "test-user-" + UUID.randomUUID().toString().substring(0, 8))
                .username(username != null ? username : "test-user-" + UUID.randomUUID().toString().substring(0, 8))
                .email(email != null ? email : "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .majorVersion(1)
                .minorVersion(0)
                .build();
    }

    /**
     * Create a test User with full name
     * 
     * @param id Optional ID, null for new entities
     * @param username Username
     * @param email Email address
     * @param firstName First name
     * @param lastName Last name
     * @return A User entity
     */
    public static User createUser(Long id, String username, String email, String firstName, String lastName) {
        return User.builder()
                .id(id)
                .name(username != null ? username : "test-user-" + UUID.randomUUID().toString().substring(0, 8))
                .username(username != null ? username : "test-user-" + UUID.randomUUID().toString().substring(0, 8))
                .email(email != null ? email : "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .firstName(firstName)
                .lastName(lastName)
                .isActive(true)
                .majorVersion(1)
                .minorVersion(0)
                .build();
    }

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
    public static Document createDocument(Long id, String name, DocumentType type, Integer majorVersion, Integer minorVersion) {
        // Create the appropriate document subclass based on type
        DocumentType docType = type != null ? type : DocumentType.ARTICLE;
        Document document;
        
        switch (docType) {
            case ARTICLE:
                document = Article.builder().build();
                break;
            case REPORT:
                document = Report.builder().build();
                break;
            case CONTRACT:
                document = Contract.builder().build();
                break;
            case MANUAL:
                document = Manual.builder().build();
                break;
            case PRESENTATION:
                document = Presentation.builder().build();
                break;
            case TRIP_REPORT:
                document = TripReport.builder().build();
                break;
            default:
                document = Article.builder().build();
                break;
        }
        
        document.setId(id);
        document.setName(name != null ? name : "test-doc-" + UUID.randomUUID().toString().substring(0, 8));
        document.setDescription("Test document description");
        document.setKeywords("test, document, keywords");
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

