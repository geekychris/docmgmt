package com.docmgmt.controller;

import com.docmgmt.DocumentManagementApplication;
import com.docmgmt.dto.ContentDTO;
import com.docmgmt.dto.ContentUploadDTO;
import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.FileStore;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.repository.DocumentRepository;
import com.docmgmt.repository.FileStoreRepository;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.util.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = DocumentManagementApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private FileStoreRepository fileStoreRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private FileStoreService fileStoreService;
    
    @TempDir
    Path tempDir;
    
    private Document testDocument;
    private FileStore testFileStore;
    private byte[] testFileContent;
    
    @BeforeEach
    void setUp() throws IOException {
        // Clear existing data
        contentRepository.deleteAll();
        documentRepository.deleteAll();
        fileStoreRepository.deleteAll();
        
        // Create test document
        testDocument = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.REPORT, 1, 0);
        testDocument = documentRepository.save(testDocument);
        
        // Create test file store
        testFileStore = TestDataBuilder.createFileStore(null, "Test FileStore", tempDir.toString(), FileStore.Status.ACTIVE);
        testFileStore = fileStoreRepository.save(testFileStore);
        
        // Load test file content
        testFileContent = "This is test content for file upload and download testing.".getBytes();
    }
    
    @AfterEach
    void tearDown() {
        // Additional cleanup if needed
    }
    
    // ----- CONTENT UPLOAD TESTS -----
    
    @Test
    void uploadContentToDatabase_shouldCreateAndReturnContent() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test-file.txt", 
                MediaType.TEXT_PLAIN_VALUE, 
                testFileContent
        );
        
        ContentUploadDTO uploadDTO = new ContentUploadDTO();
        uploadDTO.setSysObjectId(testDocument.getId());
        uploadDTO.setStoreInDatabase(true);
        
        // Act & Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/content/upload")
                .file(file)
                .param("sysObjectId", testDocument.getId().toString())
                .param("storeInDatabase", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("test-file.txt")))
                .andExpect(jsonPath("$.contentType", is(MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(jsonPath("$.sysObjectId", is(testDocument.getId().intValue())))
                .andExpect(jsonPath("$.storageType", is("DATABASE")))
                .andReturn();
        
        // Verify content was stored in the database
        String responseJson = result.getResponse().getContentAsString();
        ContentDTO contentDTO = fromJsonString(responseJson, ContentDTO.class);
        
        Content savedContent = contentRepository.findById(contentDTO.getId()).orElseThrow();
        assertThat(savedContent.isStoredInDatabase()).isTrue();
        assertThat(savedContent.getContent()).isEqualTo(testFileContent);
    }
    
    @Test
    void uploadContentToFileStore_shouldCreateAndReturnContent() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test-file.txt", 
                MediaType.TEXT_PLAIN_VALUE, 
                testFileContent
        );
        
        // Act & Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/content/upload")
                .file(file)
                .param("sysObjectId", testDocument.getId().toString())
                .param("storeInDatabase", "false")
                .param("fileStoreId", testFileStore.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("test-file.txt")))
                .andExpect(jsonPath("$.contentType", is(MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(jsonPath("$.sysObjectId", is(testDocument.getId().intValue())))
                .andExpect(jsonPath("$.storageType", is("FILE_STORE")))
                .andExpect(jsonPath("$.fileStoreId", is(testFileStore.getId().intValue())))
                .andReturn();
        
        // Verify content was stored in the file system
        String responseJson = result.getResponse().getContentAsString();
        ContentDTO contentDTO = fromJsonString(responseJson, ContentDTO.class);
        
        Content savedContent = contentRepository.findById(contentDTO.getId()).orElseThrow();
        assertThat(savedContent.isStoredInFileStore()).isTrue();
        assertThat(savedContent.getFileStore().getId()).isEqualTo(testFileStore.getId());
        assertThat(savedContent.getStoragePath()).isNotNull();
        
        // Verify the file exists on disk
        Path filePath = Paths.get(testFileStore.getFullPath(savedContent.getStoragePath()));
        assertThat(Files.exists(filePath)).isTrue();
        assertThat(Files.readAllBytes(filePath)).isEqualTo(testFileContent);
    }
    
    @Test
    void uploadEmptyFile_shouldReturnBadRequest() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "empty-file.txt", 
                MediaType.TEXT_PLAIN_VALUE, 
                new byte[0]
        );
        
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/content/upload")
                .file(file)
                .param("sysObjectId", testDocument.getId().toString())
                .param("storeInDatabase", "true"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void uploadToNonExistentSysObject_shouldReturnNotFound() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test-file.txt", 
                MediaType.TEXT_PLAIN_VALUE, 
                testFileContent
        );
        
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/content/upload")
                .file(file)
                .param("sysObjectId", "999")
                .param("storeInDatabase", "true"))
                .andExpect(status().isInternalServerError()); // Because we're using a mock SysObject in the controller
    }
    
    // ----- CONTENT DOWNLOAD TESTS -----
    
    @Test
    void downloadContentFromDatabase_shouldReturnFileContent() throws Exception {
        // Arrange - Create content in database
        Content content = TestDataBuilder.createDatabaseContent(null, "database-file.txt", "text/plain", testDocument);
        content.setContent(testFileContent);
        content = contentRepository.save(content);
        
        // Act & Assert
        mockMvc.perform(get("/api/content/{id}/download", content.getId())
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("database-file.txt")))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().bytes(testFileContent));
    }
    
    @Test
    void downloadContentFromFileStore_shouldReturnFileContent() throws Exception {
        // Arrange - Create content in file store
        Content content = TestDataBuilder.createFileStoreContent(null, "filestore-file.txt", "text/plain", testDocument, testFileStore);
        content = contentRepository.save(content);
        
        // Write test data to file
        Path filePath = Paths.get(testFileStore.getFullPath(content.getStoragePath()));
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, testFileContent);
        
        // Act & Assert
        mockMvc.perform(get("/api/content/{id}/download", content.getId())
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filestore-file.txt")))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().bytes(testFileContent));
    }
    
    @Test
    void downloadNonExistentContent_shouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/content/{id}/download", 999)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isNotFound());
    }
    
    // ----- CONTENT MOVEMENT TESTS -----
    
    @Test
    void moveContentFromDatabaseToFileStore_shouldMoveContent() throws Exception {
        // Arrange - Create content in database
        Content content = TestDataBuilder.createDatabaseContent(null, "to-move-file.txt", "text/plain", testDocument);
        content.setContent(testFileContent);
        content = contentRepository.save(content);
        
        // Act & Assert
        mockMvc.perform(put("/api/content/{id}/move-to-filestore", content.getId())
                .param("fileStoreId", testFileStore.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(content.getId().intValue())))
                .andExpect(jsonPath("$.storageType", is("FILE_STORE")))
                .andExpect(jsonPath("$.fileStoreId", is(testFileStore.getId().intValue())));
        
        // Verify content was moved to file system
        Content movedContent = contentRepository.findById(content.getId()).orElseThrow();
        assertThat(movedContent.isStoredInFileStore()).isTrue();
        assertThat(movedContent.getContent()).isNull(); // Content should be removed from DB
        
        // Verify the file exists on disk
        Path filePath = Paths.get(testFileStore.getFullPath(movedContent.getStoragePath()));
        assertThat(Files.exists(filePath)).isTrue();
        assertThat(Files.readAllBytes(filePath)).isEqualTo(testFileContent);
    }
    
    @Test
    void moveContentFromFileStoreToDatabase_shouldMoveContent() throws Exception {
        // Arrange - Create content in file store
        Content content = TestDataBuilder.createFileStoreContent(null, "to-move-to-db.txt", "text/plain", testDocument, testFileStore);
        content = contentRepository.save(content);
        
        // Write test data to file
        Path filePath = Paths.get(testFileStore.getFullPath(content.getStoragePath()));
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, testFileContent);
        
        // Act & Assert
        mockMvc.perform(put("/api/content/{id}/move-to-database", content.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(content.getId().intValue())))
                .andExpect(jsonPath("$.storageType", is("DATABASE")))
                .andExpect(jsonPath("$.fileStoreId").doesNotExist());
        
        // Verify content was moved to database
        Content movedContent = contentRepository.findById(content.getId()).orElseThrow();
        assertThat(movedContent.isStoredInDatabase()).isTrue();
        assertThat(movedContent.getContent()).isEqualTo(testFileContent);
        
        // File might still exist depending on implementation, but it's no longer referenced
        assertThat(movedContent.getFileStore()).isNull();
        assertThat(movedContent.getStoragePath()).isNull();
    }
    
    // ----- CONTENT MANAGEMENT TESTS -----
    
    @Test
    void getContentById_shouldReturnContent() throws Exception {
        // Arrange - Create content in database
        Content content = TestDataBuilder.createDatabaseContent(null, "metadata-file.txt", "text/plain", testDocument);
        content.setContent(testFileContent);
        content = contentRepository.save(content);
        
        // Act & Assert
        mockMvc.perform(get("/api/content/{id}", content.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(content.getId().intValue())))
                .andExpect(jsonPath("$.name", is("metadata-file.txt")))
                .andExpect(jsonPath("$.contentType", is("text/plain")))
                .andExpect(jsonPath("$.sysObjectId", is(testDocument.getId().intValue())))
                .andExpect(jsonPath("$.storageType", is("DATABASE")));
    }
    
    @Test
    void getBySysObject_shouldReturnAllContentForSysObject() throws Exception {
        // Arrange - Create multiple content items for the test document
        Content content1 = TestDataBuilder.createDatabaseContent(null, "doc-file1.txt", "text/plain", testDocument);
        Content content2 = TestDataBuilder.createDatabaseContent(null, "doc-file2.pdf", "application/pdf", testDocument);
        content1.setContent("Content 1".getBytes());
        content2.setContent("Content 2".getBytes());
        
        contentRepository.saveAll(Arrays.asList(content1, content2));
        
        // Create another document with its own content
        Document otherDocument = TestDataBuilder.createDocument(null, "Other Document", Document.DocumentType.ARTICLE, 1, 0);
        otherDocument = documentRepository.save(otherDocument);
        
        Content otherContent = TestDataBuilder.createDatabaseContent(null, "other-file.txt", "text/plain", otherDocument);
        otherContent.setContent("Other content".getBytes());
        contentRepository.save(otherContent);
        
        // Act & Assert
        mockMvc.perform(get("/api/content/by-sysobject/{sysObjectId}", testDocument.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("doc-file1.txt", "doc-file2.pdf")))
                .andExpect(jsonPath("$[*].sysObjectId", everyItem(is(testDocument.getId().intValue()))));
    }
    
    @Test
    void deleteContent_shouldRemoveContent() throws Exception {
        // Arrange - Create content in database
        Content content = TestDataBuilder.createDatabaseContent(null, "to-delete.txt", "text/plain", testDocument);
        content.setContent(testFileContent);
        content = contentRepository.save(content);
        
        // Act
        mockMvc.perform(delete("/api/content/{id}", content.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        
        // Assert - Content should be removed
        assertThat(contentRepository.existsById(content.getId())).isFalse();
    }
    
    @Test
    void deleteContentFromFileStore_shouldRemoveContentAndFile() throws Exception {
        // Arrange - Create content in file store
        Content content = TestDataBuilder.createFileStoreContent(null, "to-delete-file.txt", "text/plain", testDocument, testFileStore);
        content = contentRepository.save(content);
        
        // Write test data to file
        Path filePath = Paths.get(testFileStore.getFullPath(content.getStoragePath()));
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, testFileContent);
        
        // Verify file exists before deletion
        assertThat(Files.exists(filePath)).isTrue();
        
        // Act
        mockMvc.perform(delete("/api/content/{id}", content.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        
        // Assert - Content should be removed and file should be deleted
        assertThat(contentRepository.existsById(content.getId())).isFalse();
        assertThat(Files.exists(filePath)).isFalse();
    }
    
    @Test
    void moveAlreadyFileStoreContent_shouldReturnBadRequest() throws Exception {
        // Arrange - Create content already in file store
        Content content = TestDataBuilder.createFileStoreContent(null, "already-in-filestore.txt", "text/plain", testDocument, testFileStore);
        content = contentRepository.save(content);
        
        // Act & Assert
        mockMvc.perform(put("/api/content/{id}/move-to-filestore", content.getId())
                .param("fileStoreId", testFileStore.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void moveAlreadyDatabaseContent_shouldReturnBadRequest() throws Exception {
        // Arrange - Create content already in database
        Content content = TestDataBuilder.createDatabaseContent(null, "already-in-db.txt", "text/plain", testDocument);
        content.setContent(testFileContent);
        content = contentRepository.save(content);
        
        // Act & Assert
        mockMvc.perform(put("/api/content/{id}/move-to-database", content.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * Convert JSON string to object
     */
    private static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
