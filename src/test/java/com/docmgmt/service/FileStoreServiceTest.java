package com.docmgmt.service;

import com.docmgmt.model.Content;
import com.docmgmt.model.FileStore;
import com.docmgmt.repository.ContentRepository;
import com.docmgmt.repository.FileStoreRepository;
import com.docmgmt.util.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileStoreServiceTest {

    @Mock
    private FileStoreRepository fileStoreRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private FileStoreService fileStoreService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void initMocks() {
        // No setup needed - Mockito extension handles mock initialization
    }

    @Test
    void findAll_shouldReturnAllFileStores() {
        // Arrange
        FileStore fileStore1 = TestDataBuilder.createFileStore(1L, "store1", "/path/1", FileStore.Status.ACTIVE);
        FileStore fileStore2 = TestDataBuilder.createFileStore(2L, "store2", "/path/2", FileStore.Status.INACTIVE);
        when(fileStoreRepository.findAll()).thenReturn(Arrays.asList(fileStore1, fileStore2));

        // Act
        List<FileStore> result = fileStoreService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("store1", "store2");
        verify(fileStoreRepository, times(1)).findAll();
    }

    @Test
    void findAllActive_shouldReturnOnlyActiveFileStores() {
        // Arrange
        FileStore fileStore1 = TestDataBuilder.createFileStore(1L, "store1", "/path/1", FileStore.Status.ACTIVE);
        when(fileStoreRepository.findAllActive()).thenReturn(Collections.singletonList(fileStore1));

        // Act
        List<FileStore> result = fileStoreService.findAllActive();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("store1");
        assertThat(result.get(0).getStatus()).isEqualTo(FileStore.Status.ACTIVE);
        verify(fileStoreRepository, times(1)).findAllActive();
    }

    @Test
    void findById_whenExists_shouldReturnFileStore() {
        // Arrange
        Long id = 1L;
        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", "/test/path", FileStore.Status.ACTIVE);
        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));

        // Act
        FileStore result = fileStoreService.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("test-store");
        verify(fileStoreRepository, times(1)).findById(id);
    }

    @Test
    void findById_whenNotExists_shouldThrowException() {
        // Arrange
        Long id = 999L;
        when(fileStoreRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileStoreService.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("FileStore not found with ID: " + id);
        verify(fileStoreRepository, times(1)).findById(id);
    }

    @Test
    void save_validFileStore_shouldSaveAndReturn() throws IOException {
        // Arrange
        FileStore fileStore = TestDataBuilder.createFileStore(null, "new-store", tempDir.toString(), FileStore.Status.ACTIVE);
        when(fileStoreRepository.save(any(FileStore.class))).thenReturn(fileStore);
        when(fileStoreRepository.existsByName(fileStore.getName())).thenReturn(false);

        // Act
        FileStore result = fileStoreService.save(fileStore);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("new-store");
        assertThat(result.getRootPath()).isEqualTo(tempDir.toString());
        verify(fileStoreRepository, times(1)).save(fileStore);
    }

    @Test
    void save_withExistingName_shouldThrowException() {
        // Arrange
        FileStore fileStore = TestDataBuilder.createFileStore(null, "existing-store", "/test/path", FileStore.Status.ACTIVE);
        when(fileStoreRepository.existsByName(fileStore.getName())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fileStoreService.save(fileStore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FileStore with name 'existing-store' already exists");
        verify(fileStoreRepository, never()).save(any(FileStore.class));
    }

    @Test
    void delete_withNoContent_shouldDelete() {
        // Arrange
        Long id = 1L;
        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", "/test/path", FileStore.Status.ACTIVE);
        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));
        when(contentRepository.findByFileStore(fileStore)).thenReturn(Collections.emptyList());

        // Act
        fileStoreService.delete(id);

        // Assert
        verify(fileStoreRepository, times(1)).delete(fileStore);
    }

    @Test
    void delete_withContent_shouldThrowException() {
        // Arrange
        Long id = 1L;
        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", "/test/path", FileStore.Status.ACTIVE);
        Content content = TestDataBuilder.createFileStoreContent(1L, "test-content", "text/plain", null, fileStore);
        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));
        when(contentRepository.findByFileStore(fileStore)).thenReturn(Collections.singletonList(content));

        // Act & Assert
        assertThatThrownBy(() -> fileStoreService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete FileStore with ID " + id + " because it has 1 content objects");
        verify(fileStoreRepository, never()).delete(any(FileStore.class));
    }

    @Test
    void activate_validFileStore_shouldActivate() throws IOException {
        // Arrange
        Long id = 1L;
        File testDir = tempDir.toFile();
        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", testDir.getAbsolutePath(), FileStore.Status.INACTIVE);
        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));
        when(fileStoreRepository.save(any(FileStore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FileStore result = fileStoreService.activate(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FileStore.Status.ACTIVE);
        verify(fileStoreRepository, times(1)).save(fileStore);
    }

    @Test
    void deactivate_shouldDeactivate() {
        // Arrange
        Long id = 1L;
        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", "/test/path", FileStore.Status.ACTIVE);
        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));
        when(fileStoreRepository.save(any(FileStore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FileStore result = fileStoreService.deactivate(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FileStore.Status.INACTIVE);
        verify(fileStoreRepository, times(1)).save(fileStore);
    }

    @Test
    void getAvailableSpace_shouldReturnSpace() throws IOException {
        // Arrange
        Long id = 1L;
        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", tempDir.toString(), FileStore.Status.ACTIVE);
        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));

        // Act
        long space = fileStoreService.getAvailableSpace(id);

        // Assert
        assertThat(space).isGreaterThan(0);
    }

//    @Test
//    void hasEnoughSpace_withEnoughSpace_shouldReturnTrue() throws IOException {
//        // Arrange
//        Long id = 1L;
//        FileStore fileStore = TestDataBuilder.createFileStore(id, "test-store", tempDir.toString(), FileStore.Status.ACTIVE);
//        when(fileStoreRepository.findById(id)).thenReturn(Optional.of(fileStore));
//
//        // Act
//        boolean result = fileStoreService.hasEnoughSpace(id, 1024); // 1

}