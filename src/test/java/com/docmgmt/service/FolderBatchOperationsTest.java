package com.docmgmt.service;

import com.docmgmt.model.Folder;
import com.docmgmt.repository.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for batch operations in FolderService
 */
@ExtendWith(MockitoExtension.class)
public class FolderBatchOperationsTest {

    @Mock
    private FolderRepository folderRepository;

    @InjectMocks
    private FolderService folderService;

    private Folder parentFolder;
    private Folder childFolder1;
    private Folder childFolder2;

    @BeforeEach
    void setUp() {
        parentFolder = Folder.builder()
            .id(1L)
            .name("Parent Folder")
            .build();

        childFolder1 = Folder.builder()
            .id(2L)
            .name("Child Folder 1")
            .build();

        childFolder2 = Folder.builder()
            .id(3L)
            .name("Child Folder 2")
            .build();
    }

    @Test
    void testLinkFoldersToParent_Success() {
        // Arrange
        when(folderRepository.findById(1L)).thenReturn(Optional.of(parentFolder));
        when(folderRepository.findById(2L)).thenReturn(Optional.of(childFolder1));
        when(folderRepository.findById(3L)).thenReturn(Optional.of(childFolder2));
        when(folderRepository.save(any(Folder.class))).thenAnswer(i -> i.getArguments()[0]);

        List<Long> folderIds = Arrays.asList(2L, 3L);

        // Act
        Folder result = folderService.linkFoldersToParent(1L, folderIds);

        // Assert
        assertNotNull(result);
        assertEquals(parentFolder.getId(), result.getId());
        verify(folderRepository, atLeastOnce()).findById(1L);
        verify(folderRepository, times(1)).findById(2L);
        verify(folderRepository, times(1)).findById(3L);
        verify(folderRepository, atLeast(3)).save(any(Folder.class));
    }

    @Test
    void testLinkFoldersToRoot_Success() {
        // Arrange
        when(folderRepository.findById(2L)).thenReturn(Optional.of(childFolder1));
        when(folderRepository.findById(3L)).thenReturn(Optional.of(childFolder2));
        when(folderRepository.save(any(Folder.class))).thenAnswer(i -> i.getArguments()[0]);

        List<Long> folderIds = Arrays.asList(2L, 3L);

        // Act
        Folder result = folderService.linkFoldersToParent(null, folderIds);

        // Assert
        assertNull(result); // Moving to root returns null
        verify(folderRepository, times(1)).findById(2L);
        verify(folderRepository, times(1)).findById(3L);
        verify(folderRepository, times(2)).save(any(Folder.class));
    }

    @Test
    void testLinkFoldersToParent_CircularReference() {
        // Arrange - Set up circular reference: child1 is already parent of parent
        parentFolder.setParentFolder(childFolder1);
        
        when(folderRepository.findById(1L)).thenReturn(Optional.of(parentFolder));
        when(folderRepository.findById(2L)).thenReturn(Optional.of(childFolder1));

        List<Long> folderIds = Arrays.asList(2L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            folderService.linkFoldersToParent(1L, folderIds);
        });
    }

    @Test
    void testUnlinkFoldersFromParent_Success() {
        // Arrange
        childFolder1.setParentFolder(parentFolder);
        childFolder2.setParentFolder(parentFolder);
        
        when(folderRepository.findById(2L)).thenReturn(Optional.of(childFolder1));
        when(folderRepository.findById(3L)).thenReturn(Optional.of(childFolder2));
        when(folderRepository.save(any(Folder.class))).thenAnswer(i -> i.getArguments()[0]);

        List<Long> folderIds = Arrays.asList(2L, 3L);

        // Act
        folderService.unlinkFoldersFromParent(folderIds);

        // Assert
        assertNull(childFolder1.getParentFolder());
        assertNull(childFolder2.getParentFolder());
        verify(folderRepository, times(1)).findById(2L);
        verify(folderRepository, times(1)).findById(3L);
        verify(folderRepository, atLeast(4)).save(any(Folder.class));
    }

    @Test
    void testWouldCreateCircularReference_DirectSelfReference() {
        // Arrange
        when(folderRepository.findById(1L)).thenReturn(Optional.of(parentFolder));

        List<Long> folderIds = Arrays.asList(1L); // Same folder as parent

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            folderService.linkFoldersToParent(1L, folderIds);
        });
    }
}
