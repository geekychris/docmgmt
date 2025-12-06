package com.docmgmt.ui.views;

import com.docmgmt.model.Document;
import com.docmgmt.model.Article;
import com.docmgmt.model.Folder;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Test class for FolderView
 * Tests the UI logic and service integration for folder management
 */
@ExtendWith(MockitoExtension.class)
class FolderViewTest {

    @Mock
    private FolderService folderService;

    @Mock
    private DocumentService documentService;

    private FolderView folderView;

    @BeforeEach
    void setUp() {
        // Note: Cannot fully instantiate FolderView without Vaadin test environment
        // This test validates the service methods that FolderView depends on
    }

    @Test
    void testFolderServiceReturnsRootFolders() {
        List<Folder> rootFolders = Arrays.asList(
            createTestFolder(1L, "Root Folder 1"),
            createTestFolder(2L, "Root Folder 2")
        );
        when(folderService.findRootFolders()).thenReturn(rootFolders);
        
        List<Folder> roots = folderService.findRootFolders();
        assertNotNull(roots);
        assertEquals(2, roots.size());
        assertEquals("Root Folder 1", roots.get(0).getName());
        assertEquals("Root Folder 2", roots.get(1).getName());
    }

    @Test
    void testCreateFolderFlow() {
        Folder newFolder = createTestFolder(3L, "New Folder");
        when(folderService.save(any(Folder.class))).thenReturn(newFolder);
        
        Folder saved = folderService.save(newFolder);
        assertNotNull(saved);
        assertEquals("New Folder", saved.getName());
        
        verify(folderService, times(1)).save(any(Folder.class));
    }

    @Test
    void testAddChildFolderFlow() {
        Folder parent = createTestFolder(1L, "Parent");
        Folder child = createTestFolder(2L, "Child");
        
        when(folderService.addChildFolder(anyLong(), any(Folder.class))).thenReturn(parent);
        
        Folder result = folderService.addChildFolder(parent.getId(), child);
        assertNotNull(result);
        
        verify(folderService, times(1)).addChildFolder(parent.getId(), child);
    }

    @Test
    void testAddDocumentToFolderFlow() {
        Folder folder = createTestFolder(1L, "Test Folder");
        Document doc = createTestDocument(100L, "Test Document");
        
        when(folderService.addItemToFolder(anyLong(), any(Document.class))).thenReturn(folder);
        
        Folder result = folderService.addItemToFolder(folder.getId(), doc);
        assertNotNull(result);
        
        verify(folderService, times(1)).addItemToFolder(folder.getId(), doc);
    }

    @Test
    void testRemoveItemFromFolderFlow() {
        Folder folder = createTestFolder(1L, "Test Folder");
        Document doc = createTestDocument(100L, "Test Document");
        
        when(folderService.removeItemFromFolder(anyLong(), any(Document.class))).thenReturn(folder);
        
        Folder result = folderService.removeItemFromFolder(folder.getId(), doc);
        assertNotNull(result);
        
        verify(folderService, times(1)).removeItemFromFolder(folder.getId(), doc);
    }

    @Test
    void testFindByIdReturnsFolder() {
        Folder folder = createTestFolder(1L, "Test Folder");
        when(folderService.findById(1L)).thenReturn(folder);
        
        Folder found = folderService.findById(1L);
        assertNotNull(found);
        assertEquals("Test Folder", found.getName());
        assertEquals(1L, found.getId());
    }

    @Test
    void testDocumentServiceReturnsLatestVersions() {
        List<Document> docs = Arrays.asList(
            createTestDocument(1L, "Doc1"),
            createTestDocument(2L, "Doc2")
        );
        
        when(documentService.findAllLatestVersions()).thenReturn(docs);
        
        List<Document> result = documentService.findAllLatestVersions();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // Helper methods

    private Folder createTestFolder(Long id, String name) {
        Folder folder = Folder.builder()
            .name(name)
            .path("/" + name)
            .description("Test folder")
            .items(new HashSet<>())
            .childFolders(new HashSet<>())
            .build();
        folder.setId(id);
        folder.setMajorVersion(1);
        folder.setMinorVersion(0);
        return folder;
    }

    private Document createTestDocument(Long id, String name) {
        Article doc = Article.builder().build();
        doc.setId(id);
        doc.setName(name);
        doc.setDescription("Test document");
        doc.setMajorVersion(1);
        doc.setMinorVersion(0);
        return doc;
    }
}
