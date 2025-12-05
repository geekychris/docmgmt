package com.docmgmt.integration;

import com.docmgmt.model.Document;
import com.docmgmt.model.Folder;
import com.docmgmt.model.SysObject;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FolderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for folder hierarchy and SysObject relationships
 */
@SpringBootTest
@Transactional
public class FolderHierarchyIntegrationTest {
    
    @Autowired
    private FolderService folderService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private EntityManager entityManager;
    
    @Test
    public void testCreateFolderHierarchy() {
        // Create root folder
        Folder root = Folder.builder()
            .name("Root")
            .path("/root")
            .description("Root folder")
            .build();
        root = folderService.save(root);
        
        // Create child folders
        Folder child1 = Folder.builder()
            .name("Child1")
            .path("/root/child1")
            .build();
        child1 = folderService.save(child1);
        
        Folder child2 = Folder.builder()
            .name("Child2")
            .path("/root/child2")
            .build();
        child2 = folderService.save(child2);
        
        // Link children to parent
        root = folderService.addChildFolder(root.getId(), child1);
        root = folderService.addChildFolder(root.getId(), child2);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify hierarchy
        Folder loadedRoot = folderService.findById(root.getId());
        assertEquals(2, loadedRoot.getChildFolders().size());
        assertTrue(loadedRoot.isRootFolder());
        assertEquals(0, loadedRoot.getDepth());
        
        // Verify children
        List<Folder> children = folderService.findChildFolders(loadedRoot);
        assertEquals(2, children.size());
        
        for (Folder child : children) {
            assertNotNull(child.getParentFolder());
            assertEquals(loadedRoot.getId(), child.getParentFolder().getId());
            assertFalse(child.isRootFolder());
            assertEquals(1, child.getDepth());
        }
    }
    
    @Test
    public void testAddDocumentsToFolder() {
        // Create folder
        Folder folder = Folder.builder()
            .name("Documents")
            .path("/documents")
            .build();
        folder = folderService.save(folder);
        
        // Create documents
        Document doc1 = Document.builder()
            .name("Doc1")
            .documentType(Document.DocumentType.REPORT)
            .build();
        doc1 = documentService.save(doc1);
        
        Document doc2 = Document.builder()
            .name("Doc2")
            .documentType(Document.DocumentType.MANUAL)
            .build();
        doc2 = documentService.save(doc2);
        
        // Add documents to folder
        folder = folderService.addItemToFolder(folder.getId(), doc1);
        folder = folderService.addItemToFolder(folder.getId(), doc2);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify folder contains documents
        Folder loadedFolder = folderService.findById(folder.getId());
        assertEquals(2, loadedFolder.getItems().size());
        
        // Verify we can get documents of specific type
        Set<Document> documents = loadedFolder.getItemsOfType(Document.class);
        assertEquals(2, documents.size());
    }
    
    @Test
    public void testDocumentInMultipleFolders() {
        // Create folders
        Folder folder1 = Folder.builder()
            .name("Folder1")
            .path("/folder1")
            .build();
        folder1 = folderService.save(folder1);
        
        Folder folder2 = Folder.builder()
            .name("Folder2")
            .path("/folder2")
            .build();
        folder2 = folderService.save(folder2);
        
        // Create document
        Document doc = Document.builder()
            .name("SharedDoc")
            .documentType(Document.DocumentType.ARTICLE)
            .build();
        doc = documentService.save(doc);
        
        // Add document to both folders
        folderService.addItemToFolder(folder1.getId(), doc);
        folderService.addItemToFolder(folder2.getId(), doc);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify document is in both folders
        List<Folder> folders = folderService.findFoldersContaining(doc);
        assertEquals(2, folders.size());
        
        // Verify both folders contain the document
        Folder loaded1 = folderService.findById(folder1.getId());
        Folder loaded2 = folderService.findById(folder2.getId());
        
        final Long docId = doc.getId();
        assertTrue(loaded1.getItems().stream().anyMatch(i -> i.getId().equals(docId)));
        assertTrue(loaded2.getItems().stream().anyMatch(i -> i.getId().equals(docId)));
    }
    
    @Test
    public void testRemoveDocumentFromFolder() {
        // Create folder with document
        Folder folder = Folder.builder()
            .name("TestFolder")
            .path("/test")
            .build();
        folder = folderService.save(folder);
        
        Document doc = Document.builder()
            .name("TestDoc")
            .documentType(Document.DocumentType.CONTRACT)
            .build();
        doc = documentService.save(doc);
        
        folder = folderService.addItemToFolder(folder.getId(), doc);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify document is in folder
        Folder loadedFolder = folderService.findById(folder.getId());
        assertEquals(1, loadedFolder.getItems().size());
        
        // Remove document from folder
        folderService.removeItemFromFolder(folder.getId(), doc);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify document is removed
        loadedFolder = folderService.findById(folder.getId());
        assertEquals(0, loadedFolder.getItems().size());
    }
    
    @Test
    public void testDeepHierarchy() {
        // Create deep hierarchy: root -> level1 -> level2 -> level3
        Folder root = Folder.builder()
            .name("Root")
            .path("/")
            .build();
        root = folderService.save(root);
        
        Folder level1 = Folder.builder()
            .name("Level1")
            .path("/level1")
            .build();
        level1 = folderService.save(level1);
        root = folderService.addChildFolder(root.getId(), level1);
        
        Folder level2 = Folder.builder()
            .name("Level2")
            .path("/level1/level2")
            .build();
        level2 = folderService.save(level2);
        level1 = folderService.addChildFolder(level1.getId(), level2);
        
        Folder level3 = Folder.builder()
            .name("Level3")
            .path("/level1/level2/level3")
            .build();
        level3 = folderService.save(level3);
        level2 = folderService.addChildFolder(level2.getId(), level3);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify depths
        Folder loadedRoot = folderService.findById(root.getId());
        Folder loadedLevel1 = folderService.findById(level1.getId());
        Folder loadedLevel2 = folderService.findById(level2.getId());
        Folder loadedLevel3 = folderService.findById(level3.getId());
        
        assertEquals(0, loadedRoot.getDepth());
        assertEquals(1, loadedLevel1.getDepth());
        assertEquals(2, loadedLevel2.getDepth());
        assertEquals(3, loadedLevel3.getDepth());
        
        // Verify full paths
        assertTrue(loadedRoot.getFullPath().endsWith("Root"));
        assertTrue(loadedLevel1.getFullPath().contains("Level1"));
        assertTrue(loadedLevel2.getFullPath().contains("Level2"));
        assertTrue(loadedLevel3.getFullPath().contains("Level3"));
    }
    
    @Test
    public void testGetFolderHierarchy() {
        // Create hierarchy
        Folder root = Folder.builder()
            .name("Root")
            .path("/root")
            .build();
        root = folderService.save(root);
        
        Folder child1 = Folder.builder()
            .name("Child1")
            .path("/root/child1")
            .build();
        child1 = folderService.save(child1);
        
        Folder child2 = Folder.builder()
            .name("Child2")
            .path("/root/child2")
            .build();
        child2 = folderService.save(child2);
        
        Folder grandchild = Folder.builder()
            .name("Grandchild")
            .path("/root/child1/grandchild")
            .build();
        grandchild = folderService.save(grandchild);
        
        root = folderService.addChildFolder(root.getId(), child1);
        root = folderService.addChildFolder(root.getId(), child2);
        child1 = folderService.addChildFolder(child1.getId(), grandchild);
        
        entityManager.flush();
        entityManager.clear();
        
        // Get entire hierarchy
        List<Folder> hierarchy = folderService.getFolderHierarchy(root.getId());
        
        // Should contain all 4 folders
        assertEquals(4, hierarchy.size());
        
        // Root should be first
        assertEquals("Root", hierarchy.get(0).getName());
    }
    
    @Test
    public void testFindRootFolders() {
        // Create multiple root folders
        Folder root1 = Folder.builder()
            .name("Root1")
            .path("/root1")
            .build();
        root1 = folderService.save(root1);
        
        Folder root2 = Folder.builder()
            .name("Root2")
            .path("/root2")
            .build();
        root2 = folderService.save(root2);
        
        // Create a child folder
        Folder child = Folder.builder()
            .name("Child")
            .path("/root1/child")
            .build();
        child = folderService.save(child);
        folderService.addChildFolder(root1.getId(), child);
        
        entityManager.flush();
        entityManager.clear();
        
        // Find root folders
        List<Folder> rootFolders = folderService.findRootFolders();
        
        // Should find only the 2 root folders, not the child
        assertTrue(rootFolders.size() >= 2);
        assertTrue(rootFolders.stream().anyMatch(f -> f.getName().equals("Root1")));
        assertTrue(rootFolders.stream().anyMatch(f -> f.getName().equals("Root2")));
        assertFalse(rootFolders.stream().anyMatch(f -> f.getName().equals("Child")));
    }
    
    @Test
    public void testMixedItemTypes() {
        // Create folder
        Folder folder = Folder.builder()
            .name("Mixed")
            .path("/mixed")
            .build();
        folder = folderService.save(folder);
        
        // Create subfolder
        Folder subfolder = Folder.builder()
            .name("Subfolder")
            .path("/mixed/sub")
            .build();
        subfolder = folderService.save(subfolder);
        
        // Create document
        Document doc = Document.builder()
            .name("Doc")
            .documentType(Document.DocumentType.OTHER)
            .build();
        doc = documentService.save(doc);
        
        // Add both to folder
        folder = folderService.addItemToFolder(folder.getId(), subfolder);
        folder = folderService.addItemToFolder(folder.getId(), doc);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify folder contains both types
        Folder loadedFolder = folderService.findById(folder.getId());
        assertEquals(2, loadedFolder.getItems().size());
        
        // Filter by type
        Set<Folder> folders = loadedFolder.getItemsOfType(Folder.class);
        Set<Document> documents = loadedFolder.getItemsOfType(Document.class);
        
        assertEquals(1, folders.size());
        assertEquals(1, documents.size());
        assertEquals("Subfolder", folders.iterator().next().getName());
        assertEquals("Doc", documents.iterator().next().getName());
    }
}
