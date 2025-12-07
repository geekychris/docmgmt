package com.docmgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
@Entity
@Table(name = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = {"sysObject", "fileStore", "parentRendition", "secondaryRenditions"})
@ToString(exclude = {"sysObject", "fileStore", "parentRendition", "secondaryRenditions"})
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Column(name = "content")
    private byte[] content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_store_id")
    @JsonIgnore
    private FileStore fileStore;

    @Column(name = "storage_path")
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sys_object_id")
    @JsonIgnore
    private SysObject sysObject;

    // Rendition support
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_rendition_id")
    @JsonIgnore
    private Content parentRendition;

    @OneToMany(mappedBy = "parentRendition", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private java.util.List<Content> secondaryRenditions = new java.util.ArrayList<>();

    @Column(name = "is_indexable", nullable = false)
    @Builder.Default
    private boolean isIndexable = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    @Builder.Default
    private LocalDateTime modifiedAt = LocalDateTime.now();
    /**
     * Checks if this is a secondary rendition
     * @return true if this is a secondary rendition (has a parent), false otherwise
     */
    public boolean isSecondaryRendition() {
        return !isPrimary && parentRendition != null;
    }

    /**
     * Adds a secondary rendition to this content
     * @param secondary the secondary rendition to add
     */
    public void addSecondaryRendition(Content secondary) {
        if (secondary != null) {
            secondary.setParentRendition(this);
            secondary.setPrimary(false);
            secondaryRenditions.add(secondary);
        }
    }

    /**
     * Removes a secondary rendition from this content
     * @param secondary the secondary rendition to remove
     */
    public void removeSecondaryRendition(Content secondary) {
        if (secondary != null) {
            secondaryRenditions.remove(secondary);
            secondary.setParentRendition(null);
        }
    }

    /**
     * Removes all secondary renditions from this content
     */
    public void removeAllSecondaryRenditions() {
        for (Content secondary : new java.util.ArrayList<>(secondaryRenditions)) {
            removeSecondaryRendition(secondary);
        }
    }

    /**
     * Checks if the content is stored in the database
     * @return true if content is stored in the database, false otherwise
     */
    public boolean isStoredInDatabase() {
        return fileStore == null;
    }

    /**
     * Checks if the content is stored in a file store
     * @return true if content is stored in a file store, false otherwise
     */
    public boolean isStoredInFileStore() {
        return fileStore != null && storagePath != null && !storagePath.isEmpty();
    }

    /**
     * Gets the content bytes either from the database or from the file store
     * @return the content bytes
     * @throws IOException if there's an error reading from the file system
     */
    public byte[] getContentBytes() throws IOException {
        if (isStoredInDatabase()) {
            return content;
        } else if (isStoredInFileStore()) {
            Path filePath = Paths.get(fileStore.getFullPath(storagePath));
            return Files.readAllBytes(filePath);
        }
        return new byte[0];
    }

    /**
     * Sets the content bytes. If a file store is configured, writes to the file system
     * @param bytes the content bytes to store
     * @throws IOException if there's an error writing to the file system
     */
    public void setContentBytes(byte[] bytes) throws IOException {
        if (fileStore == null) {
            this.content = bytes;
        } else {
            if (storagePath == null || storagePath.isEmpty()) {
                throw new IllegalStateException("Storage path must be set when using a file store");
            }
            
            // Ensure directory exists
            Path filePath = Paths.get(fileStore.getFullPath(storagePath));
            Files.createDirectories(filePath.getParent());
            
            // Write content to file
            Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Set content to null to save database space
            this.content = null;
        }
    }
    
    /**
     * Cleans up storage when changing storage locations
     * This method should be called before changing from one storage type to another
     * Recursively removes empty parent directories up to the filestore root
     * @throws IOException if there's an error accessing the file system
     */
    public void cleanupStorage() throws IOException {
        if (isStoredInFileStore()) {
            Path filePath = Paths.get(fileStore.getFullPath(storagePath));
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                
                // Clean up empty parent directories recursively
                // For hierarchical structure like aa/bb/cc/dd/file.ext
                // We want to remove dd, cc, bb, aa if they're empty
                Path rootPath = Paths.get(fileStore.getRootPath());
                Path currentDir = filePath.getParent();
                
                while (currentDir != null && !currentDir.equals(rootPath)) {
                    try {
                        // Check if directory is empty
                        if (Files.exists(currentDir) && Files.isDirectory(currentDir)) {
                            try (var dirStream = Files.list(currentDir)) {
                                if (dirStream.findAny().isEmpty()) {
                                    Files.delete(currentDir);
                                    // Move up to parent directory
                                    currentDir = currentDir.getParent();
                                } else {
                                    // Directory not empty, stop cleanup
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        // If we can't clean up a directory, log and continue
                        // Don't fail the entire operation
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get the size of the content in bytes
     * @return the size in bytes or 0 if content not available
     * @throws IOException if there's an error accessing the file system
     */
    public long getSize() throws IOException {
        if (isStoredInDatabase() && content != null) {
            return content.length;
        } else if (isStoredInFileStore()) {
            Path filePath = Paths.get(fileStore.getFullPath(storagePath));
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        }
        return 0;
    }

    /**
     * Creates a clone of this content object
     * @return a new content object with the same properties but no ID
     */
    public Content createClone() {
        Content clone = Content.builder()
                .name(this.name)
                .contentType(this.contentType)
                .content(this.content)
                .fileStore(this.fileStore)
                .storagePath(this.storagePath)
                .isPrimary(this.isPrimary)
                .isIndexable(this.isIndexable)
                .build();
        
        // Don't clone the SysObject reference - this should be set by caller
        // Don't clone parent/secondary renditions - these should be managed separately
        // This prevents accidental sharing of content between different objects
        
        return clone;
    }
    
    /**
     * Creates a clone of this content object and associates it with a new SysObject
     * @param targetSysObject the SysObject to associate with the clone
     * @return a new content object with the same properties but no ID, associated with the target SysObject
     */
    public Content createCloneForSysObject(SysObject targetSysObject) {
        Content clone = createClone();
        if (targetSysObject != null) {
            clone.setSysObject(targetSysObject);
        }
        return clone;
    }
}
