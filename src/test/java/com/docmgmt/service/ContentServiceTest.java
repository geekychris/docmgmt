package com.docmgmt.service;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.FileStore;
import com.docmgmt.model.SysObject;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.util.TestDataBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private FileStoreService fileStoreService;
    
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ContentService contentService;

    @TempDir
    Path tempDir;

    private Document testDocument;
    private FileStore testFileStore;
    private byte[] testData = "This is test content data".getBytes();

    @BeforeEach
    public void setUp() {
        // Create test document
        testDocument = TestDataBuilder.createDocument(1L, "Test Document", Document.DocumentType.REPORT, 1, 0);

        // Create test file store
        testFileStore = TestDataBuilder.createFileStore(1L, "Test FileStore", tempDir.toString(), FileStore.Status.ACTIVE);

        // Inject the mocked EntityManager into ContentService
        ReflectionTestUtils.setField(contentService, "entityManager", entityManager);

        // Configure fileStoreService mock with lenient stubbing
        lenient().when(fileStoreService.findById(testFileStore.getId())).thenReturn(testFileStore);
    }

    // ----- CONTENT CREATION TESTS -----

    @Test
    void findById_whenExists_shouldReturnContent() {
        // Arrange
        Long id = 1L;
        Content content = TestDataBuilder.createDatabaseContent(id, "test-content", "text/plain", testDocument);
        when(contentRepository.findByIdWithAssociations(id)).thenReturn(Optional.of(content));

        // Act
        Content result = contentService.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("test-content");
        verify(contentRepository, times(1)).findByIdWithAssociations(id);
    }

    @Test
    void findById_whenNotExists_shouldThrowException() {
        // Arrange
        Long id = 999L;
        when(contentRepository.findByIdWithAssociations(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> contentService.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Content not found with ID: " + id);
    }

    @Test
    void findBySysObject_shouldReturnContentList() {
        // Arrange
        Content content1 = TestDataBuilder.createDatabaseContent(1L, "content1", "text/plain", testDocument);
        Content content2 = TestDataBuilder.createDatabaseContent(2L, "content2", "text/html", testDocument);
        when(contentRepository.findBySysObject(testDocument)).thenReturn(Arrays.asList(content1, content2));

        // Act
        List<Content> results = contentService.findBySysObject(testDocument);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactly("content1", "content2");
    }

    @Test
    void findBySysObjectAndName_whenExists_shouldReturnContent() {
        // Arrange
        String contentName = "specific-content";
        Content content = TestDataBuilder.createDatabaseContent(1L, contentName, "text/plain", testDocument);
        when(contentRepository.findBySysObjectAndName(testDocument, contentName)).thenReturn(Optional.of(content));

        // Act
        Content result = contentService.findBySysObjectAndName(testDocument, contentName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(contentName);
    }

    @Test
    void findBySysObjectAndName_whenNotExists_shouldThrowException() {
        // Arrange
        String contentName = "non-existent-content";
        when(contentRepository.findBySysObjectAndName(testDocument, contentName)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> contentService.findBySysObjectAndName(testDocument, contentName))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Content not found with name: " + contentName);
    }

    @Test
    void save_shouldSaveAndReturnContent() {
        // Arrange
        Content content = TestDataBuilder.createDatabaseContent(null, "new-content", "text/plain", testDocument);
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        // Act
        Content result = contentService.save(content);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("new-content");
        verify(contentRepository, times(1)).save(content);
    }

    @Test
    void delete_whenContentInDatabase_shouldDelete() {
        // Arrange
        Long id = 1L;
        Content content = TestDataBuilder.createDatabaseContent(id, "test-content", "text/plain", testDocument);
        when(contentRepository.findByIdWithAssociations(id)).thenReturn(Optional.of(content));

        // Act
        contentService.delete(id);

        // Assert
        verify(contentRepository, times(1)).delete(content);
    }

    @Test
    void delete_whenContentInFileStore_shouldDeleteFileAndEntity() throws IOException {
        // Arrange
        Long id = 1L;
        
        // Use the actual tempDir as the fileStore root path so getFullPath() works correctly
        FileStore realFileStore = TestDataBuilder.createFileStore(1L, "Test FileStore", tempDir.toString(), FileStore.Status.ACTIVE);
        Content content = TestDataBuilder.createFileStoreContent(id, "test-content", "text/plain", testDocument, realFileStore);

        // Create a real file for testing deletion
        Path filePath = Paths.get(realFileStore.getFullPath(content.getStoragePath()));
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, testData);

        when(contentRepository.findByIdWithAssociations(id)).thenReturn(Optional.of(content));

        // Act
        contentService.delete(id);

        // Assert
        verify(contentRepository, times(1)).delete(content);
        assertThat(Files.exists(filePath)).isFalse();
    }

    // ----- DATABASE CONTENT TESTS -----

    @Test
    void createContentInDatabase_shouldCreateContent() throws IOException {
        // Arrange
        String filename = "test-file.txt";
        String contentType = "text/plain";
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, testData);

        Content expectedContent = TestDataBuilder.createDatabaseContent(1L, filename, contentType, testDocument);
        when(contentRepository.save(any(Content.class))).thenReturn(expectedContent);

        // Act
        Content result = contentService.createContentInDatabase(file, testDocument);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(filename);
        assertThat(result.getContentType()).isEqualTo(contentType);
        assertThat(result.getSysObject()).isEqualTo(testDocument);

        verify(contentRepository, times(1)).save(any(Content.class));
    }

    // ----- FILE STORE CONTENT TESTS -----

    @Test
    void createContentInFileStore_shouldCreateContent() throws IOException {
        // Arrange
        String filename = "test-file.txt";
        String contentType = "text/plain";
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, testData);

        Content expectedContent = TestDataBuilder.createFileStoreContent(1L, filename, contentType, testDocument, testFileStore);
        when(contentRepository.save(any(Content.class))).thenReturn(expectedContent);

        // Act
        Content result = contentService.createContentInFileStore(file, testDocument, testFileStore.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(filename);
        assertThat(result.getContentType()).isEqualTo(contentType);
        assertThat(result.getSysObject()).isEqualTo(testDocument);
        assertThat(result.getFileStore()).isEqualTo(testFileStore);
        assertThat(result.getStoragePath()).isNotNull();

        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    void createContentInFileStore_withInactiveFileStore_shouldThrowException() throws IOException {
        // Arrange
        String filename = "test-file.txt";
        String contentType = "text/plain";
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, testData);

        // Create inactive file store
        FileStore inactiveFileStore = TestDataBuilder.createFileStore(2L, "Inactive Store", tempDir.toString(), FileStore.Status.INACTIVE);
        when(fileStoreService.findById(inactiveFileStore.getId())).thenReturn(inactiveFileStore);

        // Act & Assert
        assertThatThrownBy(() -> contentService.createContentInFileStore(file, testDocument, inactiveFileStore.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FileStore is not active");

        verify(contentRepository, never()).save(any(Content.class));
    }

    // ----- CONTENT RETRIEVAL TESTS -----

    @Test
    void getContentBytes_fromDatabase_shouldReturnBytes() throws IOException {
        // Arrange
        Long id = 1L;
        Content content = TestDataBuilder.createDatabaseContent(id, "test-content", "text/plain", testDocument);
        content.setContent(testData);
        when(contentRepository.findByIdWithAssociations(id)).thenReturn(Optional.of(content));

        // Act
        byte[] result = contentService.getContentBytes(id);

        // Assert
        assertThat(result).isEqualTo(testData);
    }

    @Test
    void getContentBytes_fromFileStore_shouldReturnBytes() throws IOException {
        // Arrange
        Long id = 1L;
        
        // Use the actual tempDir as the fileStore root path so getFullPath() works correctly
        FileStore realFileStore = TestDataBuilder.createFileStore(1L, "Test FileStore", tempDir.toString(), FileStore.Status.ACTIVE);
        Content content = TestDataBuilder.createFileStoreContent(id, "test-content", "text/plain", testDocument, realFileStore);

        // Create real file with test data
        Path filePath = Paths.get(realFileStore.getFullPath(content.getStoragePath()));
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, testData);

        when(contentRepository.findByIdWithAssociations(id)).thenReturn(Optional.of(content));

        // Act
        byte[] result = contentService.getContentBytes(id);

        // Assert
        assertThat(result).isEqualTo(testData);
    }

    // ----- STORAGE MOVEMENT TESTS -----

    @Test
    void moveToFileStore_shouldMoveContent() throws IOException {
        // Arrange
        Long contentId = 1L;
        Long fileStoreId = testFileStore.getId();

        // Create content in database
        Content databaseContent = TestDataBuilder.createDatabaseContent(contentId, "test-content", "text/plain", testDocument);
        databaseContent.setContent(testData);

        // Create expected result after move
        Content fileStoreContent = TestDataBuilder.createFileStoreContent(contentId, "test-content", "text/plain", testDocument, testFileStore);

        when(contentRepository.findByIdWithAssociations(contentId)).thenReturn(Optional.of(databaseContent));
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> {
            Content savedContent = invocation.getArgument(0);
            savedContent.setId(contentId);
            return savedContent;
        });

        // Act
        Content result = contentService.moveToFileStore(contentId, fileStoreId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(contentId);
        assertThat(result.getFileStore()).isEqualTo(testFileStore);
        assertThat(result.getStoragePath()).isNotNull();
        assertThat(result.getContent()).isNull(); // Content should be moved to file system

        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    void moveToFileStore_whenAlreadyInFileStore_shouldThrowException() {
        // Arrange
        Long contentId = 1L;
        Long fileStoreId = testFileStore.getId();

        // Create content already in file store
        Content fileStoreContent = TestDataBuilder.createFileStoreContent(contentId, "test-content", "text/plain", testDocument, testFileStore);

        when(contentRepository.findByIdWithAssociations(contentId)).thenReturn(Optional.of(fileStoreContent));

        // Act & Assert
        assertThatThrownBy(() -> contentService.moveToFileStore(contentId, fileStoreId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Content is already stored in a file store");

        verify(contentRepository, never()).save(any(Content.class));
    }

//    @Test
//    void moveToDatabase_shouldMoveContent() throws IOException {
//        // Arrange
//        Long contentId = 1L;
//
//        // Create content in file store
//        Content fileStoreContent = TestDataBuilder.createFileStoreContent(contentId, "test-content", "text/plain", testDocument, testFileStore);
//
//        // Create a real file for testing
//        Path filePath = tempDir.resolve(fileStoreContent.getStoragePath());
//        Files.write(filePath, testData);
//
//        when(contentRepository.findById(contentId)).thenReturn(

}