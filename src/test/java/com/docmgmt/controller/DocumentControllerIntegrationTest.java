package com.docmgmt.controller;

import com.docmgmt.DocumentManagementApplication;
import com.docmgmt.dto.DocumentDTO;
import com.docmgmt.dto.SysObjectVersionDTO;
import com.docmgmt.model.Document;
import com.docmgmt.repository.DocumentRepository;
import com.docmgmt.service.DocumentService;
import com.docmgmt.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        // Clear existing data
        documentRepository.deleteAll();
    }

    // ----- CRUD OPERATIONS TESTS -----

    @Test
    void createDocument_shouldCreateAndReturnDocument() throws Exception {
        // Arrange
        DocumentDTO documentDTO = createTestDocumentDTO("Test Document", Document.DocumentType.REPORT);

        // Act & Assert
        mockMvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(documentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is(documentDTO.getName())))
                .andExpect(jsonPath("$.documentType", is(documentDTO.getDocumentType().toString())))
                .andExpect(jsonPath("$.majorVersion", is(1)))
                .andExpect(jsonPath("$.minorVersion", is(0)));
    }

    @Test
    void getDocumentById_whenExists_shouldReturnDocument() throws Exception {
        // Arrange
        Document document = createAndSaveTestDocument("Test Document", Document.DocumentType.REPORT);

        // Act & Assert
        mockMvc.perform(get("/api/documents/{id}", document.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(document.getId().intValue())))
                .andExpect(jsonPath("$.name", is(document.getName())))
                .andExpect(jsonPath("$.documentType", is(document.getDocumentType().toString())));
    }

    @Test
    void getDocumentById_whenNotExists_shouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/documents/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocument_shouldUpdateAndReturnDocument() throws Exception {
        // Arrange
        Document document = createAndSaveTestDocument("Original Document", Document.DocumentType.REPORT);
        DocumentDTO updateDTO = createTestDocumentDTO("Updated Document", Document.DocumentType.MANUAL);
        updateDTO.setId(document.getId());

        // Act & Assert
        mockMvc.perform(put("/api/documents/{id}", document.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(document.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Updated Document")))
                .andExpect(jsonPath("$.documentType", is(Document.DocumentType.MANUAL.toString())));
    }

    @Test
    void deleteDocument_shouldDeleteDocument() throws Exception {
        // Arrange
        Document document = createAndSaveTestDocument("Test Document", Document.DocumentType.REPORT);

        // Act & Assert
        mockMvc.perform(delete("/api/documents/{id}", document.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify document is deleted
        mockMvc.perform(get("/api/documents/{id}", document.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ----- VERSION MANAGEMENT TESTS -----

    @Test
    void createMajorVersion_shouldCreateNewVersionAndReturn() throws Exception {
        // Arrange
        Document document = createAndSaveTestDocument("Original Document", Document.DocumentType.REPORT);

        // Act & Assert
        mockMvc.perform(post("/api/documents/{id}/versions/major", document.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(document.getId().intValue())))
                .andExpect(jsonPath("$.name", is(document.getName())))
                .andExpect(jsonPath("$.majorVersion", is(document.getMajorVersion() + 1)))
                .andExpect(jsonPath("$.minorVersion", is(0)))
                .andExpect(jsonPath("$.parentVersionId", is(document.getId().intValue())));
    }

    @Test
    void createMinorVersion_shouldCreateNewVersionAndReturn() throws Exception {
        // Arrange
        Document document = createAndSaveTestDocument("Original Document", Document.DocumentType.REPORT);

        // Act & Assert
        mockMvc.perform(post("/api/documents/{id}/versions/minor", document.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(document.getId().intValue())))
                .andExpect(jsonPath("$.name", is(document.getName())))
                .andExpect(jsonPath("$.majorVersion", is(document.getMajorVersion())))
                .andExpect(jsonPath("$.minorVersion", is(document.getMinorVersion() + 1)))
                .andExpect(jsonPath("$.parentVersionId", is(document.getId().intValue())));
    }

    @Test
    void getVersionHistory_shouldReturnOrderedHistory() throws Exception {
        // Arrange - Create a chain of documents v1.0 -> v2.0 -> v3.0
        Document v1 = createAndSaveTestDocument("Version Document", Document.DocumentType.REPORT);
        
        // Create major version to get v2.0
        Document v2 = documentService.createMajorVersion(v1.getId());
        
        // Create another major version to get v3.0
        Document v3 = documentService.createMajorVersion(v2.getId());

        // Act & Assert
        mockMvc.perform(get("/api/documents/{id}/versions/history", v3.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(v3.getId().intValue())))
                .andExpect(jsonPath("$[0].version", is("3.0")))
                .andExpect(jsonPath("$[0].isLatestVersion", is(true)))
                .andExpect(jsonPath("$[1].id", is(v2.getId().intValue())))
                .andExpect(jsonPath("$[1].version", is("2.0")))
                .andExpect(jsonPath("$[2].id", is(v1.getId().intValue())))
                .andExpect(jsonPath("$[2].version", is("1.0")));
    }

    // ----- SEARCH OPERATIONS TESTS -----

    @Test
    void findByDocumentType_shouldReturnMatchingDocuments() throws Exception {
        // Arrange - Create documents of different types
        Document report1 = createAndSaveTestDocument("Report 1", Document.DocumentType.REPORT);
        Document report2 = createAndSaveTestDocument("Report 2", Document.DocumentType.REPORT);
        Document manual = createAndSaveTestDocument("Manual", Document.DocumentType.MANUAL);

        // Act & Assert
        mockMvc.perform(get("/api/documents/by-type/{documentType}", "REPORT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Report 1", "Report 2")))
                .andExpect(jsonPath("$[*].documentType", everyItem(is("REPORT"))));
    }

    @Test
    void findByAuthor_shouldReturnMatchingDocuments() throws Exception {
        // Arrange - Create documents with different authors
        Document doc1 = createAndSaveTestDocument("Doc by John", Document.DocumentType.REPORT);
        Document doc2 = createAndSaveTestDocument("Another by John", Document.DocumentType.MANUAL);
        Document doc3 = createAndSaveTestDocument("Doc by Jane", Document.DocumentType.ARTICLE);
        
        String johnAuthor = "John Smith";
        String janeAuthor = "Jane Doe";
        
        doc1.setAuthor(johnAuthor);
        doc2.setAuthor(johnAuthor);
        doc3.setAuthor(janeAuthor);
        
        documentRepository.saveAll(Arrays.asList(doc1, doc2, doc3));

        // Act & Assert
        mockMvc.perform(get("/api/documents/by-author/{author}", johnAuthor)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Doc by John", "Another by John")))
                .andExpect(jsonPath("$[*].author", everyItem(is(johnAuthor))));
    }

    @Test
    void findByTag_shouldReturnDocumentsWithTag() throws Exception {
        // Arrange - Create documents with different tags
        Document doc1 = createAndSaveTestDocument("Important Doc", Document.DocumentType.REPORT);
        Document doc2 = createAndSaveTestDocument("Urgent Doc", Document.DocumentType.MANUAL);
        Document doc3 = createAndSaveTestDocument("Regular Doc", Document.DocumentType.ARTICLE);
        
        doc1.addTag("important");
        doc1.addTag("financial");
        doc2.addTag("important");
        doc2.addTag("urgent");
        doc3.addTag("regular");
        
        documentRepository.saveAll(Arrays.asList(doc1, doc2, doc3));

        // Act & Assert
        mockMvc.perform(get("/api/documents/by-tag/{tag}", "important")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Important Doc", "Urgent Doc")));
    }

    @Test
    void searchByKeywords_shouldReturnMatchingDocuments() throws Exception {
        // Arrange - Create documents with different keywords
        Document doc1 = createAndSaveTestDocument("Financial Report", Document.DocumentType.REPORT);
        Document doc2 = createAndSaveTestDocument("Budget Plan", Document.DocumentType.REPORT);
        Document doc3 = createAndSaveTestDocument("Product Manual", Document.DocumentType.MANUAL);
        
        doc1.setKeywords("financial quarterly report earnings");
        doc2.setKeywords("financial budget planning annual");
        doc3.setKeywords("product usage manual guide");
        
        documentRepository.saveAll(Arrays.asList(doc1, doc2, doc3));

        // Act & Assert
        mockMvc.perform(get("/api/documents/search")
                .param("keywords", "financial")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Financial Report", "Budget Plan")));
    }

    // ----- ERROR HANDLING TESTS -----

    @Test
    void createDocument_withMissingRequiredFields_shouldReturnBadRequest() throws Exception {
        // Arrange - Create document DTO with missing required fields
        DocumentDTO invalidDocument = new DocumentDTO();
        invalidDocument.setDescription("Only a description, missing name and type");

        // Act & Assert
        mockMvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(invalidDocument)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors", notNullValue()));
    }

    @Test
    void createMajorVersion_forNonExistentDocument_shouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/documents/{id}/versions/major", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ----- TAG MANAGEMENT TESTS -----

    @Test
    void documentWithTags_shouldPersistAndRetrieveTags() throws Exception {
        // Arrange
        DocumentDTO documentDTO = createTestDocumentDTO("Tagged Document", Document.DocumentType.REPORT);
        Set<String> tags = new HashSet<>(Arrays.asList("important", "financial", "quarterly"));
        documentDTO.setTags(tags);

        // Act - Create document with tags
        String createResponse = mockMvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(documentDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract ID from response
        DocumentDTO createdDoc = fromJsonString(createResponse, DocumentDTO.class);
        
        // Assert - Verify tags are persisted and retrieved
        mockMvc.perform(get("/api/documents/{id}", createdDoc.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(3)))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("important", "financial", "quarterly")));
    }

    // ----- UTILITY METHODS -----

    /**
     * Create a test document DTO with specified name and type
     */
    private DocumentDTO createTestDocumentDTO(String name, Document.DocumentType type) {
        DocumentDTO dto = new DocumentDTO();
        dto.setName(name);
        dto.setDocumentType(type);
        dto.setDescription("Test description");
        dto.setKeywords("test keywords");
        dto.setAuthor("Test Author");
        return dto;
    }

    /**
     * Create and save a test document to the repository
     */
    private Document createAndSaveTestDocument(String name, Document.DocumentType type) {
        Document document = TestDataBuilder.createDocument(null, name, type, 1, 0);
        document.setDescription("Test description");
        document.setAuthor("Test Author");
        document.setKeywords("test keywords");
        return documentRepository.save(document);
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
