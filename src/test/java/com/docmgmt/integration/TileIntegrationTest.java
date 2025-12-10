package com.docmgmt.integration;

import com.docmgmt.BaseTest;
import com.docmgmt.dto.TileConfigurationDTO;
import com.docmgmt.dto.TileDTO;
import com.docmgmt.model.*;
import com.docmgmt.repository.TileConfigurationRepository;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FolderService;
import com.docmgmt.service.TileService;
import com.docmgmt.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for tile display functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TileIntegrationTest extends BaseTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TileService tileService;
    
    @Autowired
    private FolderService folderService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TileConfigurationRepository configRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private Folder testFolder;
    private Document testDocument;
    
    @BeforeEach
    public void setUp() {
        // Create test user
        testUser = User.builder()
            .username("testuser")
            .name("Test User")
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .build();
        testUser = userService.save(testUser);
        
        // Create test folder
        testFolder = Folder.builder()
            .name("TestTileFolder")
            .description("Test folder for tile display")
            .owner(testUser)
            .build();
        testFolder = folderService.save(testFolder);
        
        // Create test document
        testDocument = Article.builder()
            .name("Test Article")
            .description("Test article for tile display")
            .url("https://example.com/article")
            .documentType(Document.DocumentType.ARTICLE)
            .tags(new HashSet<>())
            .owner(testUser)
            .build();
        testDocument.addTag("test");
        testDocument.addTag("article");
        testDocument = documentService.save(testDocument);
        
        // Add document to folder
        folderService.addItemToFolder(testFolder.getId(), testDocument);
    }
    
    @Test
    public void testGetTilesByFolderName() throws Exception {
        mockMvc.perform(get("/api/tiles/{folderName}", testFolder.getName()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("Test Article"))
            .andExpect(jsonPath("$[0].description").value("Test article for tile display"))
            .andExpect(jsonPath("$[0].url").value("https://example.com/article"));
    }
    
    @Test
    public void testGetTileConfigurationByFolderId() throws Exception {
        mockMvc.perform(get("/api/tiles/config/{folderId}", testFolder.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.folderId").value(testFolder.getId()))
            .andExpect(jsonPath("$.visibleFields").value("name,description,url"))
            .andExpect(jsonPath("$.tileSize").value("MEDIUM"));
    }
    
    @Test
    public void testSaveTileConfiguration() throws Exception {
        TileConfigurationDTO dto = TileConfigurationDTO.builder()
            .folderId(testFolder.getId())
            .groupBySubfolder(true)
            .visibleFields("name,description,url,tags")
            .colorStrategy("BY_TYPE")
            .tileSize("LARGE")
            .showDetailLink(true)
            .showUrlLink(true)
            .sortOrder("NAME")
            .build();
        
        String jsonContent = objectMapper.writeValueAsString(dto);
        
        mockMvc.perform(post("/api/tiles/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.folderId").value(testFolder.getId()))
            .andExpect(jsonPath("$.groupBySubfolder").value(true))
            .andExpect(jsonPath("$.tileSize").value("LARGE"))
            .andExpect(jsonPath("$.colorStrategy").value("BY_TYPE"));
    }
    
    @Test
    public void testTileServiceGetTiles() {
        // Get configuration
        TileConfiguration config = tileService.getConfiguration(testFolder.getId());
        assertThat(config).isNotNull();
        
        // Get tiles
        List<TileDTO> tiles = tileService.getTiles(testFolder, config);
        assertThat(tiles).hasSize(1);
        
        TileDTO tile = tiles.get(0);
        assertThat(tile.getName()).isEqualTo("Test Article");
        assertThat(tile.getDescription()).isEqualTo("Test article for tile display");
        assertThat(tile.getUrl()).isEqualTo("https://example.com/article");
        assertThat(tile.getDocumentType()).isEqualTo("ARTICLE");
        assertThat(tile.getTags()).containsExactlyInAnyOrder("test", "article");
    }
    
    @Test
    public void testTileServiceSaveConfiguration() {
        TileConfigurationDTO dto = TileConfigurationDTO.builder()
            .folderId(testFolder.getId())
            .groupBySubfolder(false)
            .visibleFields("name,description")
            .colorStrategy("BY_TAG")
            .tileSize("SMALL")
            .showDetailLink(false)
            .showUrlLink(true)
            .sortOrder("TYPE")
            .build();
        
        TileConfiguration saved = tileService.saveConfiguration(dto);
        
        assertThat(saved).isNotNull();
        assertThat(saved.getFolder().getId()).isEqualTo(testFolder.getId());
        assertThat(saved.getGroupBySubfolder()).isFalse();
        assertThat(saved.getVisibleFields()).isEqualTo("name,description");
        assertThat(saved.getColorStrategy()).isEqualTo(TileConfiguration.ColorStrategy.BY_TAG);
        assertThat(saved.getTileSize()).isEqualTo(TileConfiguration.TileSize.SMALL);
    }
    
    @Test
    public void testTileServiceWithCustomColorMappings() throws Exception {
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("ARTICLE", "#FF5733");
        colorMap.put("REPORT", "#33FF57");
        
        String colorMappings = objectMapper.writeValueAsString(colorMap);
        
        TileConfigurationDTO dto = TileConfigurationDTO.builder()
            .folderId(testFolder.getId())
            .colorStrategy("CUSTOM")
            .colorMappings(colorMappings)
            .build();
        
        TileConfiguration saved = tileService.saveConfiguration(dto);
        
        assertThat(saved.getColorMappings()).isEqualTo(colorMappings);
        assertThat(saved.getColorStrategy()).isEqualTo(TileConfiguration.ColorStrategy.CUSTOM);
    }
    
    @Test
    public void testTileServiceGroupBySubfolder() {
        // Create subfolder
        Folder subfolder = Folder.builder()
            .name("SubfolderForGrouping")
            .parentFolder(testFolder)
            .owner(testUser)
            .build();
        subfolder = folderService.save(subfolder);
        testFolder.addChildFolder(subfolder);
        
        // Create document in subfolder
        Document subDoc = Report.builder()
            .name("Report in Subfolder")
            .description("Test report in subfolder")
            .documentType(Document.DocumentType.REPORT)
            .owner(testUser)
            .build();
        subDoc = documentService.save(subDoc);
        folderService.addItemToFolder(subfolder.getId(), subDoc);
        
        // Configure grouping
        TileConfigurationDTO dto = TileConfigurationDTO.builder()
            .folderId(testFolder.getId())
            .groupBySubfolder(true)
            .build();
        
        TileConfiguration config = tileService.saveConfiguration(dto);
        
        // Get tiles
        List<TileDTO> tiles = tileService.getTiles(folderService.findById(testFolder.getId()), config);
        
        // Should have tiles from both main folder and subfolder
        assertThat(tiles).hasSizeGreaterThanOrEqualTo(1);
        
        // Check if grouping is applied
        long groupedTiles = tiles.stream()
            .filter(tile -> tile.getGroupName() != null)
            .count();
        
        assertThat(groupedTiles).isGreaterThan(0);
    }
}
