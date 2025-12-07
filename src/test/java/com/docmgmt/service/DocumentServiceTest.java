package com.docmgmt.service;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.FileStore;
import com.docmgmt.repository.DocumentRepository;
import com.docmgmt.util.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @BeforeEach
    public void setUp() {
        // No setup needed - Mockito extension handles mock initialization
    }

    // ----- BASIC CRUD TESTS -----

    @Test
    void findById_whenExists_shouldReturnDocument() {
        // Arrange
        Long id = 1L;
        Document document = TestDataBuilder.createDocument(id, "Test Document", Document.DocumentType.REPORT, 1, 0);
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));

        // Act
        Document result = documentService.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Test Document");
        assertThat(result.getDocumentType()).isEqualTo(Document.DocumentType.REPORT);
        verify(documentRepository, times(1)).findById(id);
    }

    @Test
    void findById_whenNotExists_shouldThrowException() {
        // Arrange
        Long id = 999L;
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentService.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Entity not found with ID: " + id);
    }

    @Test
    void save_newDocument_shouldInitializeVersionAndSave() {
        // Arrange
        Document document = TestDataBuilder.createDocument(null, "New Document", Document.DocumentType.ARTICLE, null, null);
        
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document savedDoc = invocation.getArgument(0);
            if (savedDoc.getId() == null) {
                savedDoc.setId(1L);
            }
            return savedDoc;
        });

        // Act
        Document result = documentService.save(document);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMajorVersion()).isEqualTo(1);
        assertThat(result.getMinorVersion()).isEqualTo(0);
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    void save_existingDocument_shouldNotChangeVersion() {
        // Arrange
        Document document = TestDataBuilder.createDocument(1L, "Existing Document", Document.DocumentType.MANUAL, 2, 3);
        
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        // Act
        Document result = documentService.save(document);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMajorVersion()).isEqualTo(2);
        assertThat(result.getMinorVersion()).isEqualTo(3);
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    void delete_whenExists_shouldDelete() {
        // Arrange
        Long id = 1L;
        Document document = TestDataBuilder.createDocument(id, "Test Document", Document.DocumentType.REPORT, 1, 0);
        when(documentRepository.existsById(id)).thenReturn(true);

        // Act
        documentService.delete(id);

        // Assert
        verify(documentRepository, times(1)).deleteById(id);
    }

    @Test
    void delete_whenNotExists_shouldThrowException() {
        // Arrange
        Long id = 999L;
        when(documentRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> documentService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Entity not found with ID: " + id);
        
        verify(documentRepository, never()).deleteById(anyLong());
    }

    // ----- VERSION MANAGEMENT TESTS -----

    @Test
    void createMajorVersion_shouldCreateNewVersionWithIncrementedMajorVersion() {
        // Arrange
        Long id = 1L;
        Document originalDoc = TestDataBuilder.createDocument(id, "Original Document", Document.DocumentType.REPORT, 1, 0);
        originalDoc.setDescription("Original description");
        
        Document newVersionDoc = TestDataBuilder.createDocument(null, "Original Document", Document.DocumentType.REPORT, 2, 0);
        newVersionDoc.setDescription("Original description");
        newVersionDoc.setParentVersion(originalDoc);
        
        when(documentRepository.findById(id)).thenReturn(Optional.of(originalDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document savedDoc = invocation.getArgument(0);
            if (savedDoc.getId() == null) {
                savedDoc.setId(2L);
            }
            return savedDoc;
        });

        // Act
        Document result = documentService.createMajorVersion(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo(originalDoc.getName());
        assertThat(result.getDescription()).isEqualTo(originalDoc.getDescription());
        assertThat(result.getDocumentType()).isEqualTo(originalDoc.getDocumentType());
        assertThat(result.getMajorVersion()).isEqualTo(2);
        assertThat(result.getMinorVersion()).isEqualTo(0);
        assertThat(result.getParentVersion()).isEqualTo(originalDoc);
        
        // Verify tags were copied
        assertThat(result.getTags()).containsExactlyInAnyOrderElementsOf(originalDoc.getTags());
        
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void createMinorVersion_shouldCreateNewVersionWithIncrementedMinorVersion() {
        // Arrange
        Long id = 1L;
        Document originalDoc = TestDataBuilder.createDocument(id, "Original Document", Document.DocumentType.REPORT, 1, 0);
        originalDoc.setDescription("Original description");
        
        Document newVersionDoc = TestDataBuilder.createDocument(null, "Original Document", Document.DocumentType.REPORT, 1, 1);
        newVersionDoc.setDescription("Original description");
        newVersionDoc.setParentVersion(originalDoc);
        
        when(documentRepository.findById(id)).thenReturn(Optional.of(originalDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document savedDoc = invocation.getArgument(0);
            if (savedDoc.getId() == null) {
                savedDoc.setId(2L);
            }
            return savedDoc;
        });

        // Act
        Document result = documentService.createMinorVersion(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo(originalDoc.getName());
        assertThat(result.getDescription()).isEqualTo(originalDoc.getDescription());
        assertThat(result.getDocumentType()).isEqualTo(originalDoc.getDocumentType());
        assertThat(result.getMajorVersion()).isEqualTo(1);
        assertThat(result.getMinorVersion()).isEqualTo(1);
        assertThat(result.getParentVersion()).isEqualTo(originalDoc);
        
        // Verify tags were copied
        assertThat(result.getTags()).containsExactlyInAnyOrderElementsOf(originalDoc.getTags());
        
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void getVersionHistory_shouldReturnOrderedHistory() {
        // Arrange
        Long id = 3L;
        
        // Create a chain of documents: v1.0 -> v2.0 -> v3.0
        Document v1 = TestDataBuilder.createDocument(1L, "Document", Document.DocumentType.REPORT, 1, 0);
        Document v2 = TestDataBuilder.createDocument(2L, "Document", Document.DocumentType.REPORT, 2, 0);
        Document v3 = TestDataBuilder.createDocument(3L, "Document", Document.DocumentType.REPORT, 3, 0);
        
        v2.setParentVersion(v1);
        v3.setParentVersion(v2);
        
        when(documentRepository.findById(id)).thenReturn(Optional.of(v3));

        // Act
        List<Document> history = documentService.getVersionHistory(id);

        // Assert
        assertThat(history).hasSize(3);
        assertThat(history.get(0)).isEqualTo(v3); // Current version first
        assertThat(history.get(1)).isEqualTo(v2); // Parent next
        assertThat(history.get(2)).isEqualTo(v1); // Original last
    }

    // ----- DOCUMENT TYPE AND TAGS TESTS -----

    @Test
    void findByDocumentType_shouldReturnMatchingDocuments() {
        // Arrange
        Document.DocumentType targetType = Document.DocumentType.REPORT;
        List<Document> expectedDocs = Arrays.asList(
            TestDataBuilder.createDocument(1L, "Report 1", targetType, 1, 0),
            TestDataBuilder.createDocument(2L, "Report 2", targetType, 1, 0)
        );
        
        when(documentRepository.findByDocumentType(targetType)).thenReturn(expectedDocs);

        // Act
        List<Document> results = documentService.findByDocumentType(targetType);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactly("Report 1", "Report 2");
        assertThat(results).extracting("documentType").containsOnly(targetType);
        verify(documentRepository, times(1)).findByDocumentType(targetType);
    }

    @Test
    void findByTag_shouldReturnDocumentsWithTag() {
        // Arrange
        String tag = "important";
        Document doc1 = TestDataBuilder.createDocument(1L, "Doc 1", Document.DocumentType.ARTICLE, 1, 0);
        Document doc2 = TestDataBuilder.createDocument(2L, "Doc 2", Document.DocumentType.MANUAL, 1, 0);
        doc1.addTag(tag);
        doc2.addTag(tag);
        
        when(documentRepository.findByTagsContaining(tag)).thenReturn(Arrays.asList(doc1, doc2));

        // Act
        List<Document> results = documentService.findByTag(tag);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTags()).contains(tag);
        assertThat(results.get(1).getTags()).contains(tag);
        verify(documentRepository, times(1)).findByTagsContaining(tag);
    }

    @Test
    void findLatestVersionByTypeAndName_shouldReturnLatestVersion() {
        // Arrange
        String name = "Annual Report";
        Document.DocumentType type = Document.DocumentType.REPORT;
        Document latestDoc = TestDataBuilder.createDocument(3L, name, type, 3, 0);
        
        when(documentRepository.findByDocumentTypeAndNameOrderByMajorVersionDescMinorVersionDesc(type, name))
            .thenReturn(Optional.of(latestDoc));

        // Act
        Optional<Document> result = documentService.findLatestVersionByTypeAndName(type, name);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(name);
        assertThat(result.get().getDocumentType()).isEqualTo(type);
        assertThat(result.get().getMajorVersion()).isEqualTo(3);
        verify(documentRepository, times(1)).findByDocumentTypeAndNameOrderByMajorVersionDescMinorVersionDesc(type, name);
    }

    @Test
    void findByKeywords_shouldReturnMatchingDocuments() {
        // Arrange
        String searchTerm = "financial";
        Document doc1 = TestDataBuilder.createDocument(1L, "Doc 1", Document.DocumentType.REPORT, 1, 0);
        Document doc2 = TestDataBuilder.createDocument(2L, "Doc 2", Document.DocumentType.REPORT, 1, 0);
        
        doc1.setKeywords("financial report quarterly");
        doc2.setKeywords("annual financial summary");
        
        when(documentRepository.findByKeywordsContaining(searchTerm)).thenReturn(Arrays.asList(doc1, doc2));

        // Act
        List<Document> results = documentService.findByKeywords(searchTerm);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getKeywords()).contains(searchTerm);
        assertThat(results.get(1).getKeywords()).contains(searchTerm);
        verify(documentRepository, times(1)).findByKeywordsContaining(searchTerm);
    }
    
    // ----- EDGE CASES AND ERROR HANDLING -----

    @Test
    void createMajorVersion_whenDocumentDoesNotExist_shouldThrowException() {
        // Arrange
        Long id = 999L;
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentService.createMajorVersion(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Entity not found with ID: " + id);
    }

    @Test
    void createMinorVersion_whenDocumentDoesNotExist_shouldThrowException() {
        // Arrange
        Long id = 999L;
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentService.createMinorVersion(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Entity not found with ID: " + id);
    }
    
    // ----- CONTENT RELATIONSHIP TESTS -----
    
    @Test
    void createMajorVersion_shouldCopyContentReferences() {
        // Arrange
        Long id = 1L;
        Document originalDoc = TestDataBuilder.createDocument(id, "Original Document", Document.DocumentType.REPORT, 1, 0);
        
        // Create content objects for the original document
        Content content1 = TestDataBuilder.createDatabaseContent(1L, "content1.txt", "text/plain", originalDoc);
        Content content2 = TestDataBuilder.createDatabaseContent(2L, "content2.pdf", "application/pdf", originalDoc);
        
        Set<Content> contents = new HashSet<>(Arrays.asList(content1, content2));
        originalDoc.setContents(contents);
        
        when(documentRepository.findById(id)).thenReturn(Optional.of(originalDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document savedDoc = invocation.getArgument(0);
            if (savedDoc.getId() == null) {
                savedDoc.setId(2L);
            }
            return savedDoc;
        });

        // Act
        Document newVersion = documentService.createMajorVersion(id);

        // Assert
        assertThat(newVersion).isNotNull();
        assertThat(newVersion.getContents()).hasSize(originalDoc.getContents().size());
        
        // Verify contents were properly copied
        assertThat(newVersion.getContents())
                .extracting("name")
                .containsExactlyInAnyOrderElementsOf(
                        originalDoc.getContents().stream()
                                .map(Content::getName)
                                .collect(java.util.stream.Collectors.toList())
                );
    }
    
    @Test
    void findAllLatestVersions_shouldReturnOnlyLatestVersions() {
        // Arrange
        Document doc1v1 = TestDataBuilder.createDocument(1L, "Doc 1", Document.DocumentType.REPORT, 1, 0);
        Document doc1v2 = TestDataBuilder.createDocument(2L, "Doc 1", Document.DocumentType.REPORT, 2, 0);
        Document doc2v1 = TestDataBuilder.createDocument(3L, "Doc 2", Document.DocumentType.MANUAL, 1, 0);
        
        // Set up version relationships
        doc1v2.setParentVersion(doc1v1);
        
        when(documentRepository.findLatestVersions()).thenReturn(Arrays.asList(doc1v2, doc2v1));

        // Act
        List<Document> results = documentService.findAllLatestVersions();

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).contains(doc1v2, doc2v1);
        assertThat(results).doesNotContain(doc1v1); // Should not include older versions
        verify(documentRepository, times(1)).findLatestVersions();
    }
}
