package com.docmgmt.integration;

import com.docmgmt.model.Article;
import com.docmgmt.model.Document;
import com.docmgmt.model.User;
import com.docmgmt.repository.DocumentRepository;
import com.docmgmt.repository.UserRepository;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.UserService;
import com.docmgmt.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnershipAndAuthorsIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSetDocumentOwner() {
        // Given
        User owner = TestDataBuilder.createUser(null, "owner", "owner@example.com", "Owner", "User");
        User savedOwner = userService.save(owner);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.setOwner(savedOwner);

        // When
        Document savedDocument = documentService.save(document);

        // Then
        assertNotNull(savedDocument.getOwner());
        assertEquals(savedOwner.getId(), savedDocument.getOwner().getId());
        assertEquals("owner", savedDocument.getOwner().getUsername());
    }

    @Test
    void testDocumentWithNullOwner() {
        // Given
        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        // Owner is null

        // When
        Document savedDocument = documentService.save(document);

        // Then
        assertNull(savedDocument.getOwner());
    }

    @Test
    void testAddSingleAuthor() {
        // Given
        User author = TestDataBuilder.createUser(null, "author1", "author1@example.com", "Author", "One");
        User savedAuthor = userService.save(author);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.addAuthor(savedAuthor);

        // When
        Document savedDocument = documentService.save(document);

        // Then
        assertEquals(1, savedDocument.getAuthors().size());
        assertTrue(savedDocument.getAuthors().contains(savedAuthor));
    }

    @Test
    void testAddMultipleAuthors() {
        // Given
        User author1 = TestDataBuilder.createUser(null, "author1", "author1@example.com", "Author", "One");
        User author2 = TestDataBuilder.createUser(null, "author2", "author2@example.com", "Author", "Two");
        User author3 = TestDataBuilder.createUser(null, "author3", "author3@example.com", "Author", "Three");
        User savedAuthor1 = userService.save(author1);
        User savedAuthor2 = userService.save(author2);
        User savedAuthor3 = userService.save(author3);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.addAuthor(savedAuthor1);
        document.addAuthor(savedAuthor2);
        document.addAuthor(savedAuthor3);

        // When
        Document savedDocument = documentService.save(document);

        // Then
        assertEquals(3, savedDocument.getAuthors().size());
        assertTrue(savedDocument.getAuthors().contains(savedAuthor1));
        assertTrue(savedDocument.getAuthors().contains(savedAuthor2));
        assertTrue(savedDocument.getAuthors().contains(savedAuthor3));
    }

    @Test
    void testRemoveAuthor() {
        // Given
        User author1 = TestDataBuilder.createUser(null, "author1", "author1@example.com", "Author", "One");
        User author2 = TestDataBuilder.createUser(null, "author2", "author2@example.com", "Author", "Two");
        User savedAuthor1 = userService.save(author1);
        User savedAuthor2 = userService.save(author2);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.addAuthor(savedAuthor1);
        document.addAuthor(savedAuthor2);
        Document savedDocument = documentService.save(document);

        // When
        savedDocument.removeAuthor(savedAuthor1);
        Document updatedDocument = documentService.save(savedDocument);

        // Then
        assertEquals(1, updatedDocument.getAuthors().size());
        assertFalse(updatedDocument.getAuthors().contains(savedAuthor1));
        assertTrue(updatedDocument.getAuthors().contains(savedAuthor2));
    }

    @Test
    void testOwnerAndAuthors() {
        // Given
        User owner = TestDataBuilder.createUser(null, "owner", "owner@example.com", "Owner", "User");
        User author1 = TestDataBuilder.createUser(null, "author1", "author1@example.com", "Author", "One");
        User author2 = TestDataBuilder.createUser(null, "author2", "author2@example.com", "Author", "Two");
        User savedOwner = userService.save(owner);
        User savedAuthor1 = userService.save(author1);
        User savedAuthor2 = userService.save(author2);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.setOwner(savedOwner);
        document.addAuthor(savedAuthor1);
        document.addAuthor(savedAuthor2);

        // When
        Document savedDocument = documentService.save(document);

        // Then
        assertNotNull(savedDocument.getOwner());
        assertEquals(savedOwner.getId(), savedDocument.getOwner().getId());
        assertEquals(2, savedDocument.getAuthors().size());
        assertTrue(savedDocument.getAuthors().contains(savedAuthor1));
        assertTrue(savedDocument.getAuthors().contains(savedAuthor2));
    }

    @Test
    void testOwnerCanAlsoBeAuthor() {
        // Given
        User ownerAndAuthor = TestDataBuilder.createUser(null, "user1", "user1@example.com", "User", "One");
        User savedUser = userService.save(ownerAndAuthor);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.setOwner(savedUser);
        document.addAuthor(savedUser);

        // When
        Document savedDocument = documentService.save(document);

        // Then
        assertNotNull(savedDocument.getOwner());
        assertEquals(savedUser.getId(), savedDocument.getOwner().getId());
        assertEquals(1, savedDocument.getAuthors().size());
        assertTrue(savedDocument.getAuthors().contains(savedUser));
    }

    @Test
    void testRetrieveDocumentWithOwnerAndAuthors() {
        // Given
        User owner = TestDataBuilder.createUser(null, "owner", "owner@example.com", "Owner", "User");
        User author = TestDataBuilder.createUser(null, "author", "author@example.com", "Author", "User");
        User savedOwner = userService.save(owner);
        User savedAuthor = userService.save(author);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.setOwner(savedOwner);
        document.addAuthor(savedAuthor);
        Document savedDocument = documentService.save(document);

        // When
        Document retrievedDocument = documentService.findById(savedDocument.getId());

        // Then - verify lazy collections are initialized
        assertNotNull(retrievedDocument.getOwner());
        assertEquals("owner", retrievedDocument.getOwner().getUsername());
        assertNotNull(retrievedDocument.getAuthors());
        assertEquals(1, retrievedDocument.getAuthors().size());
    }

    @Test
    void testDocumentVersionInheritsOwnerAndAuthors() {
        // Given
        User owner = TestDataBuilder.createUser(null, "owner", "owner@example.com", "Owner", "User");
        User author = TestDataBuilder.createUser(null, "author", "author@example.com", "Author", "User");
        User savedOwner = userService.save(owner);
        User savedAuthor = userService.save(author);

        Document document = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.ARTICLE, 1, 0);
        document.setOwner(savedOwner);
        document.addAuthor(savedAuthor);
        Document savedDocument = documentService.save(document);

        // When
        Document majorVersion = documentService.createMajorVersion(savedDocument.getId());

        // Then
        assertNotNull(majorVersion.getOwner());
        assertEquals(savedOwner.getId(), majorVersion.getOwner().getId());
        assertEquals(1, majorVersion.getAuthors().size());
        assertTrue(majorVersion.getAuthors().contains(savedAuthor));
    }

    @Test
    void testUserCanOwnMultipleDocuments() {
        // Given
        User owner = TestDataBuilder.createUser(null, "owner", "owner@example.com", "Owner", "User");
        User savedOwner = userService.save(owner);

        Document doc1 = TestDataBuilder.createDocument(null, "Document 1", Document.DocumentType.ARTICLE, 1, 0);
        Document doc2 = TestDataBuilder.createDocument(null, "Document 2", Document.DocumentType.REPORT, 1, 0);
        Document doc3 = TestDataBuilder.createDocument(null, "Document 3", Document.DocumentType.CONTRACT, 1, 0);
        
        doc1.setOwner(savedOwner);
        doc2.setOwner(savedOwner);
        doc3.setOwner(savedOwner);

        // When
        documentService.save(doc1);
        documentService.save(doc2);
        documentService.save(doc3);

        // Then - all documents should have the same owner
        assertEquals(savedOwner.getId(), documentService.findById(doc1.getId()).getOwner().getId());
        assertEquals(savedOwner.getId(), documentService.findById(doc2.getId()).getOwner().getId());
        assertEquals(savedOwner.getId(), documentService.findById(doc3.getId()).getOwner().getId());
    }

    @Test
    void testUserCanBeAuthorOfMultipleDocuments() {
        // Given
        User author = TestDataBuilder.createUser(null, "author", "author@example.com", "Author", "User");
        User savedAuthor = userService.save(author);

        Document doc1 = TestDataBuilder.createDocument(null, "Document 1", Document.DocumentType.ARTICLE, 1, 0);
        Document doc2 = TestDataBuilder.createDocument(null, "Document 2", Document.DocumentType.REPORT, 1, 0);
        
        doc1.addAuthor(savedAuthor);
        doc2.addAuthor(savedAuthor);

        // When
        Document savedDoc1 = documentService.save(doc1);
        Document savedDoc2 = documentService.save(doc2);

        // Then
        assertTrue(savedDoc1.getAuthors().contains(savedAuthor));
        assertTrue(savedDoc2.getAuthors().contains(savedAuthor));
    }

    @Test
    void testUserEntityCanHaveOwnerAndAuthors() {
        // Given - Users can also have owners and authors since they extend SysObject
        User manager = TestDataBuilder.createUser(null, "manager", "manager@example.com", "Manager", "User");
        User editor1 = TestDataBuilder.createUser(null, "editor1", "editor1@example.com", "Editor", "One");
        User editor2 = TestDataBuilder.createUser(null, "editor2", "editor2@example.com", "Editor", "Two");
        
        User savedManager = userService.save(manager);
        User savedEditor1 = userService.save(editor1);
        User savedEditor2 = userService.save(editor2);

        User employee = TestDataBuilder.createUser(null, "employee", "employee@example.com", "Employee", "User");
        employee.setOwner(savedManager);
        employee.addAuthor(savedEditor1);
        employee.addAuthor(savedEditor2);

        // When
        User savedEmployee = userService.save(employee);

        // Then
        assertNotNull(savedEmployee.getOwner());
        assertEquals(savedManager.getId(), savedEmployee.getOwner().getId());
        assertEquals(2, savedEmployee.getAuthors().size());
        assertTrue(savedEmployee.getAuthors().contains(savedEditor1));
        assertTrue(savedEmployee.getAuthors().contains(savedEditor2));
    }
}
