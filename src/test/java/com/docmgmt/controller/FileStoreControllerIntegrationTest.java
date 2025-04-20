package com.docmgmt.controller;

import com.docmgmt.DocumentManagementApplication;
import com.docmgmt.dto.FileStoreDTO;
import com.docmgmt.dto.SpaceInfoDTO;
import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.FileStore;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.repository.DocumentRepository;
import com.docmgmt.repository.FileStoreRepository;
import com.docmgmt.util.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
public class FileStoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private FileStoreRepository fileStoreRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @TempDir
    Path tempDir;
    
    private Document testDocument;
    
    @BeforeEach
    void setUp() {
        // Clear existing data
        contentRepository.deleteAll();
        fileStoreRepository.deleteAll();
        documentRepository.deleteAll();
        
        // Create test document
        testDocument = TestDataBuilder.createDocument(null, "Test Document", Document.DocumentType.REPORT, 1, 0);
        testDocument = documentRepository.save(testDocument);
    }
    
    @AfterEach
    void tearDown() {
        // Additional cleanup if needed
    }
    
    // ----- CRUD OPERATIONS TESTS -----
    
    @Test
    void getAllFileStores_shouldReturnAllFileStores() throws Exception {
        // Arrange - Create multiple file stores
        FileStore store1 = createAndSaveTestFileStore("Store 1", tempDir.resolve("store1"));
        FileStore store2 = createAndSaveTestFileStore("Store 2", tempDir.resolve("store2"));
        
        // Act & Assert
        mockMvc.perform(get("/api/filestores")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Store 1", "Store 2")));
    }
    
    @Test
    void getActiveFileStores_shouldReturnOnlyActiveFileStores() throws Exception {
        // Arrange - Create active and inactive file stores
        FileStore activeStore = createAndSaveTestFileStore("Active Store", tempDir.resolve("active"));
        
        FileStore inactiveStore = TestDataBuilder.createFileStore(null, "Inactive Store", 
                tempDir.resolve("inactive").toString(), FileStore.Status.INACTIVE);
        fileStoreRepository.save(inactiveStore);
        
        // Act & Assert
        mockMvc.perform(get("/api/filestores/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Active Store")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }
    
    @Test
    void getFileStoreById_shouldReturnFileStore() throws Exception {
        // Arrange - Create a file store
        FileStore fileStore = createAndSaveTestFileStore("Test Store", tempDir);
        
        // Act & Assert
        mockMvc.perform(get("/api/filestores/{id}", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileStore.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Store")))
                .andExpect(jsonPath("$.rootPath", is(tempDir.toString())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
    
    @Test
    void createFileStore_shouldCreateAndReturnFileStore() throws Exception {
        // Arrange - Create DTO for new file store
        FileStoreDTO fileStoreDTO = new FileStoreDTO();
        fileStoreDTO.setName("New File Store");
        fileStoreDTO.setRootPath(tempDir.resolve("new-store").toString());
        
        // Act & Assert
        mockMvc.perform(post("/api/filestores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(fileStoreDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("New File Store")))
                .andExpect(jsonPath("$.rootPath", is(tempDir.resolve("new-store").toString())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
        
        // Verify directory was created
        assertThat(Files.exists(tempDir.resolve("new-store"))).isTrue();
    }
    
    @Test
    void updateFileStore_shouldUpdateFileStore() throws Exception {
        // Arrange - Create a file store
        FileStore fileStore = createAndSaveTestFileStore("Original Store", tempDir);
        
        // Create update DTO
        FileStoreDTO updateDTO = new FileStoreDTO();
        updateDTO.setId(fileStore.getId());
        updateDTO.setName("Updated Store");
        updateDTO.setRootPath(tempDir.toString()); // Keep same path
        
        // Act & Assert
        mockMvc.perform(put("/api/filestores/{id}", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileStore.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Updated Store")))
                .andExpect(jsonPath("$.rootPath", is(tempDir.toString())));
                
        // Verify update was persisted
        FileStore updatedFileStore = fileStoreRepository.findById(fileStore.getId()).orElseThrow();
        assertThat(updatedFileStore.getName()).isEqualTo("Updated Store");
    }
    
    @Test
    void deleteFileStore_shouldDeleteFileStore() throws Exception {
        // Arrange - Create a file store
        FileStore fileStore = createAndSaveTestFileStore("To Delete Store", tempDir.resolve("to-delete"));
        
        // Act & Assert
        mockMvc.perform(delete("/api/filestores/{id}", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        
        // Verify file store is deleted
        assertThat(fileStoreRepository.existsById(fileStore.getId())).isFalse();
    }
    
    @Test
    void deleteFileStoreWithContent_shouldReturnConflict() throws Exception {
        // Arrange - Create a file store with content
        FileStore fileStore = createAndSaveTestFileStore("Store With Content", tempDir.resolve("has-content"));
        
        // Create content referencing the file store
        Content content = TestDataBuilder.createFileStoreContent(null, "file.txt", "text/plain", testDocument, fileStore);
        contentRepository.save(content);
        
        // Act & Assert
        mockMvc.perform(delete("/api/filestores/{id}", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
        
        // Verify file store still exists
        assertThat(fileStoreRepository.existsById(fileStore.getId())).isTrue();
    }
    
    // ----- STATUS MANAGEMENT TESTS -----
    
    @Test
    void activateFileStore_shouldActivateInactiveFileStore() throws Exception {
        // Arrange - Create inactive file store
        FileStore fileStore = TestDataBuilder.createFileStore(null, "Inactive Store", 
                tempDir.toString(), FileStore.Status.INACTIVE);
        fileStore = fileStoreRepository.save(fileStore);
        
        // Act & Assert
        mockMvc.perform(put("/api/filestores/{id}/activate", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileStore.getId().intValue())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
        
        // Verify status was updated
        FileStore updatedFileStore = fileStoreRepository.findById(fileStore.getId()).orElseThrow();
        assertThat(updatedFileStore.getStatus()).isEqualTo(FileStore.Status.ACTIVE);
    }
    
    @Test
    void deactivateFileStore_shouldDeactivateActiveFileStore() throws Exception {
        // Arrange - Create active file store
        FileStore fileStore = createAndSaveTestFileStore("Active Store", tempDir);
        
        // Act & Assert
        mockMvc.perform(put("/api/filestores/{id}/deactivate", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileStore.getId().intValue())))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
        
        // Verify status was updated
        FileStore updatedFileStore = fileStoreRepository.findById(fileStore.getId()).orElseThrow();
        assertThat(updatedFileStore.getStatus()).isEqualTo(FileStore.Status.INACTIVE);
    }
    
    // ----- SPACE VALIDATION TESTS -----
    
    @Test
    void getAvailableSpace_shouldReturnSpaceInfo() throws Exception {
        // Arrange - Create file store
        FileStore fileStore = createAndSaveTestFileStore("Space Test Store", tempDir);
        
        // Act & Assert
        mockMvc.perform(get("/api/filestores/{id}/space", fileStore.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileStore.getId().intValue())))
                .andExpect(jsonPath("$.name", is(fileStore.getName())))
                .andExpect(jsonPath("$.availableSpace", greaterThan(0)))
                .andExpect(jsonPath("$.formattedAvailableSpace", notNullValue()));
    }
    
    @Test
    void checkEnoughSpace_withEnoughSpace_shouldReturnTrue() throws Exception {
        // Arrange - Create file store
        FileStore fileStore = createAndSaveTestFileStore("Space Check Store", tempDir);
        
        // Act & Assert - Check for small amount of space (should be available)
        mockMvc.perform(get("/api/filestores/{id}/space/check", fileStore.getId())
                .param("requiredBytes", "1024") // 1KB
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
    
    // ----- ERROR HANDLING TESTS -----
    
    @Test
    void createFileStore_withExistingName_shouldReturnBadRequest() throws Exception {
        // Arrange - Create existing file store
        FileStore existingStore = createAndSaveTestFileStore("Existing Name", tempDir.resolve("existing"));
        
        // Create new file store with same name
        FileStoreDTO newFileStore = new FileStoreDTO();
        newFileStore.setName("Existing Name"); // Same name as existing store
        newFileStore.setRootPath(tempDir.resolve("new-path").toString());
        
        // Act & Assert
        mockMvc.perform(post("/api/filestores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(newFileStore)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getFileStoreById_whenNotExists_shouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/filestores/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void activateFileStore_withNonExistentId_shouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/filestores/{id}/activate", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    
    // ----- UTILITY METHODS -----
    
    /**
     * Create and save a test file store
     */
    private FileStore createAndSaveTestFileStore(String name, Path path) throws IOException {
        // Ensure directory exists
        Files.createDirectories(path);
        
        FileStore fileStore = TestDataBuilder.createFileStore(null, name, path.toString(), FileStore.Status.ACTIVE);
        return fileStoreRepository.save(fileStore);
    }
    
    /**
     * Convert object to JSON string
     */
    private static String asJsonString(final Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
