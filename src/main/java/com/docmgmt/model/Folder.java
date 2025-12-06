package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Folder entity - extends SysObject directly
 * Can contain other SysObjects (documents, folders, etc.)
 */
@Entity
@Table(name = "folder")
@DiscriminatorValue("FOLDER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true, exclude = {"parentFolder", "childFolders", "items"})
@ToString(callSuper = true, exclude = {"parentFolder", "childFolders", "items"})
public class Folder extends SysObject {
    
    @Column(name = "path", columnDefinition = "TEXT")
    private String path;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "folder_permissions", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<String> permissions = new HashSet<>();
    
    // Parent folder for hierarchical structure
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;
    
    // Child folders
    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Folder> childFolders = new HashSet<>();
    
    // SysObjects contained in this folder (many-to-many)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "folder_sysobjects",
        joinColumns = @JoinColumn(name = "folder_id"),
        inverseJoinColumns = @JoinColumn(name = "sysobject_id")
    )
    @Builder.Default
    private Set<SysObject> items = new HashSet<>();
    
    /**
     * Add a permission to the folder
     * @param permission The permission to add (e.g., "READ", "WRITE", "DELETE")
     * @return this folder for method chaining
     */
    public Folder addPermission(String permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
        return this;
    }
    
    /**
     * Remove a permission from the folder
     * @param permission The permission to remove
     * @return this folder for method chaining
     */
    public Folder removePermission(String permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
        return this;
    }
    
    /**
     * Add a child folder to this folder
     * @param child The child folder to add
     * @return this folder for method chaining
     */
    public Folder addChildFolder(Folder child) {
        if (childFolders == null) {
            childFolders = new HashSet<>();
        }
        childFolders.add(child);
        child.setParentFolder(this);
        return this;
    }
    
    /**
     * Remove a child folder from this folder
     * @param child The child folder to remove
     * @return this folder for method chaining
     */
    public Folder removeChildFolder(Folder child) {
        if (childFolders != null) {
            childFolders.remove(child);
            child.setParentFolder(null);
        }
        return this;
    }
    
    /**
     * Add a SysObject (document, folder, etc.) to this folder
     * @param item The SysObject to add
     * @return this folder for method chaining
     */
    public Folder addItem(SysObject item) {
        if (items == null) {
            items = new HashSet<>();
        }
        items.add(item);
        return this;
    }
    
    /**
     * Remove a SysObject from this folder
     * @param item The SysObject to remove
     * @return this folder for method chaining
     */
    public Folder removeItem(SysObject item) {
        if (items != null) {
            items.remove(item);
        }
        return this;
    }
    
    /**
     * Get all items of a specific type
     * @param type The class type to filter by
     * @return Set of items of the specified type
     */
    public <T extends SysObject> Set<T> getItemsOfType(Class<T> type) {
        if (items == null) {
            return new HashSet<>();
        }
        return items.stream()
            .filter(type::isInstance)
            .map(type::cast)
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Get the full path of this folder including all parent folders
     * @return The full hierarchical path
     */
    public String getFullPath() {
        if (parentFolder == null) {
            return "/" + getName();
        }
        return parentFolder.getFullPath() + "/" + getName();
    }
    
    /**
     * Check if this folder is a root folder (has no parent)
     * @return true if root folder, false otherwise
     */
    public boolean isRootFolder() {
        return parentFolder == null;
    }
    
    /**
     * Get the depth of this folder in the hierarchy (root = 0)
     * @return The depth level
     */
    public int getDepth() {
        if (parentFolder == null) {
            return 0;
        }
        return parentFolder.getDepth() + 1;
    }
    
    /**
     * Copy attributes to target entity
     * Overrides the method in SysObject to include Folder-specific attributes
     * @param target The target entity
     */
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof Folder) {
            Folder folderTarget = (Folder) target;
            folderTarget.setPath(this.getPath());
            folderTarget.setDescription(this.getDescription());
            folderTarget.setIsPublic(this.getIsPublic());
            folderTarget.setParentFolder(this.getParentFolder());
            
            // Copy permissions
            if (this.getPermissions() != null && !this.getPermissions().isEmpty()) {
                folderTarget.setPermissions(new HashSet<>(this.getPermissions()));
            }
            
            // Note: Child folders and items are NOT copied during versioning
            // This is intentional - versions start with empty relationships
        }
    }
}
