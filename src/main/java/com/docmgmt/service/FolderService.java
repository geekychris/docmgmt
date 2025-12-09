package com.docmgmt.service;

import com.docmgmt.model.Folder;
import com.docmgmt.model.SysObject;
import com.docmgmt.repository.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for Folder entity operations
 */
@Service
public class FolderService extends AbstractSysObjectService<Folder, FolderRepository> {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FolderService.class);
    
    @Autowired
    public FolderService(FolderRepository repository) {
        super(repository);
    }
    
    /**
     * Find folders by path
     * @param path The folder path
     * @return List of folders with the given path
     */
    @Transactional(readOnly = true)
    public List<Folder> findByPath(String path) {
        return repository.findByPath(path);
    }
    
    /**
     * Find folders by path starting with a prefix
     * @param pathPrefix The path prefix
     * @return List of folders with paths starting with the prefix
     */
    @Transactional(readOnly = true)
    public List<Folder> findByPathStartingWith(String pathPrefix) {
        return repository.findByPathStartingWith(pathPrefix);
    }
    
    /**
     * Find public folders
     * @param isPublic Whether the folder is public
     * @return List of public or private folders
     */
    @Transactional(readOnly = true)
    public List<Folder> findByIsPublic(Boolean isPublic) {
        return repository.findByIsPublic(isPublic);
    }
    
    /**
     * Find root folders (folders with no parent)
     * @return List of root folders
     */
    @Transactional(readOnly = true)
    public List<Folder> findRootFolders() {
        List<Folder> folders = repository.findByParentFolderIsNull();
        // Initialize lazy collections
        folders.forEach(folder -> {
            if (folder.getChildFolders() != null) {
                folder.getChildFolders().size();
            }
            if (folder.getItems() != null) {
                folder.getItems().size();
            }
        });
        return folders;
    }
    
    /**
     * Find child folders of a parent folder
     * @param parentFolder The parent folder
     * @return List of child folders
     */
    @Transactional(readOnly = true)
    public List<Folder> findChildFolders(Folder parentFolder) {
        List<Folder> folders = repository.findByParentFolder(parentFolder);
        // Initialize lazy collections
        folders.forEach(folder -> {
            if (folder.getChildFolders() != null) {
                folder.getChildFolders().size();
            }
            if (folder.getItems() != null) {
                folder.getItems().size();
            }
        });
        return folders;
    }
    
    /**
     * Find folders containing a specific SysObject
     * @param item The SysObject to search for
     * @return List of folders containing the item
     */
    @Transactional(readOnly = true)
    public List<Folder> findFoldersContaining(SysObject item) {
        return repository.findByItemsContaining(item);
    }
    
    /**
     * Add a SysObject to a folder
     * @param folderId The folder ID
     * @param item The SysObject to add
     * @return The updated folder
     */
    @Transactional
    public Folder addItemToFolder(Long folderId, SysObject item) {
        Folder folder = findById(folderId);
        folder.addItem(item);
        return repository.save(folder);
    }
    
    /**
     * Remove a SysObject from a folder
     * @param folderId The folder ID
     * @param item The SysObject to remove
     * @return The updated folder
     */
    @Transactional
    public Folder removeItemFromFolder(Long folderId, SysObject item) {
        Folder folder = findById(folderId);
        folder.removeItem(item);
        return repository.save(folder);
    }
    
    /**
     * Add a child folder to a parent folder
     * @param parentId The parent folder ID
     * @param child The child folder
     * @return The updated parent folder
     */
    @Transactional
    public Folder addChildFolder(Long parentId, Folder child) {
        Folder parent = findById(parentId);
        parent.addChildFolder(child);
        return repository.save(parent);
    }
    
    /**
     * Link multiple folders to a parent folder (or root if parentId is null)
     * @param parentId The parent folder ID (null for root)
     * @param folderIds The list of folder IDs to link
     * @return The updated parent folder (or null if moving to root)
     */
    @Transactional
    public Folder linkFoldersToParent(Long parentId, List<Long> folderIds) {
        Folder parent = parentId != null ? findById(parentId) : null;
        
        for (Long folderId : folderIds) {
            Folder folder = findById(folderId);
            
            // Prevent circular references
            if (parent != null && wouldCreateCircularReference(folder, parent)) {
                throw new IllegalArgumentException(
                    "Cannot move folder '" + folder.getName() + "' - would create circular reference"
                );
            }
            
            // Unlink from current parent if any
            if (folder.getParentFolder() != null) {
                Folder oldParent = folder.getParentFolder();
                oldParent.removeChildFolder(folder);
                repository.save(oldParent);
            }
            
            // Link to new parent
            folder.setParentFolder(parent);
            if (parent != null) {
                parent.addChildFolder(folder);
            }
            repository.save(folder);
        }
        
        return parent != null ? repository.save(parent) : null;
    }
    
    /**
     * Unlink multiple folders from their parent (move to root)
     * @param folderIds The list of folder IDs to unlink
     */
    @Transactional
    public void unlinkFoldersFromParent(List<Long> folderIds) {
        for (Long folderId : folderIds) {
            Folder folder = findById(folderId);
            
            if (folder.getParentFolder() != null) {
                Folder parent = folder.getParentFolder();
                parent.removeChildFolder(folder);
                repository.save(parent);
            }
            
            folder.setParentFolder(null);
            repository.save(folder);
        }
    }
    
    /**
     * Check if linking child to parent would create a circular reference
     * @param child The folder to be made a child
     * @param parent The prospective parent
     * @return true if this would create a circular reference
     */
    private boolean wouldCreateCircularReference(Folder child, Folder parent) {
        if (child.getId().equals(parent.getId())) {
            return true;
        }
        
        Folder current = parent.getParentFolder();
        while (current != null) {
            if (current.getId().equals(child.getId())) {
                return true;
            }
            current = current.getParentFolder();
        }
        
        return false;
    }
    
    /**
     * Get all folders in the hierarchy starting from a root folder
     * @param rootId The root folder ID
     * @return List of all folders in the hierarchy
     */
    @Transactional(readOnly = true)
    public List<Folder> getFolderHierarchy(Long rootId) {
        Folder root = findById(rootId);
        List<Folder> hierarchy = new ArrayList<>();
        collectFolderHierarchy(root, hierarchy);
        return hierarchy;
    }
    
    /**
     * Recursively collect all folders in a hierarchy
     * @param folder The current folder
     * @param result The list to collect folders into
     */
    private void collectFolderHierarchy(Folder folder, List<Folder> result) {
        result.add(folder);
        if (folder.getChildFolders() != null) {
            // Initialize child folders
            folder.getChildFolders().size();
            for (Folder child : folder.getChildFolders()) {
                collectFolderHierarchy(child, result);
            }
        }
    }
    
    /**
     * Override findAll to ensure permissions are initialized
     * @return List of all folders
     */
    @Override
    @Transactional(readOnly = true)
    public List<Folder> findAll() {
        List<Folder> folders = super.findAll();
        // Initialize all lazy collections
        folders.forEach(folder -> {
            if (folder.getPermissions() != null) {
                folder.getPermissions().size();
            }
            if (folder.getContents() != null) {
                folder.getContents().size();
            }
            if (folder.getChildFolders() != null) {
                folder.getChildFolders().size();
            }
            if (folder.getItems() != null) {
                folder.getItems().size();
            }
            // Touch parent version to initialize it
            if (folder.getParentVersion() != null) {
                folder.getParentVersion().getName();
            }
        });
        return folders;
    }
    
    /**
     * Override findById to ensure permissions are initialized
     * @param id The folder ID
     * @return The found folder
     */
    @Override
    @Transactional(readOnly = true)
    public Folder findById(Long id) {
        Folder folder = super.findById(id);
        // Initialize permissions and contents collections
        if (folder.getPermissions() != null) {
            folder.getPermissions().size();
        }
        if (folder.getContents() != null) {
            folder.getContents().size();
        }
        // Initialize childFolders and items
        if (folder.getChildFolders() != null) {
            folder.getChildFolders().size();
        }
        if (folder.getItems() != null) {
            folder.getItems().size();
        }
        // Touch parent version to initialize it
        if (folder.getParentVersion() != null) {
            folder.getParentVersion().getName();
        }
        return folder;
    }
    
    /**
     * Find folder by ID with all UI-needed relationships eagerly loaded
     * This method ensures owner, authors, and all collections are initialized
     * to prevent LazyInitializationException in the UI layer
     * @param id The folder ID
     * @return The folder with all relationships initialized
     */
    @Transactional(readOnly = true)
    public Folder findByIdWithRelationships(Long id) {
        Folder folder = findById(id);
        
        // Initialize owner
        if (folder.getOwner() != null) {
            folder.getOwner().getUsername();
        }
        
        // Initialize folder's own authors
        if (folder.getAuthors() != null) {
            folder.getAuthors().size();
            for (com.docmgmt.model.User author : folder.getAuthors()) {
                author.getUsername();
            }
        }
        
        // Initialize all child folders and their owners
        if (folder.getChildFolders() != null) {
            for (Folder child : folder.getChildFolders()) {
                if (child.getOwner() != null) {
                    child.getOwner().getUsername();
                }
            }
        }
        
        // Initialize all items (documents) and their owners/authors
        if (folder.getItems() != null) {
            for (SysObject item : folder.getItems()) {
                if (item.getOwner() != null) {
                    item.getOwner().getUsername();
                }
                if (item instanceof com.docmgmt.model.Document) {
                    com.docmgmt.model.Document doc = (com.docmgmt.model.Document) item;
                    if (doc.getAuthors() != null) {
                        doc.getAuthors().size();
                        for (com.docmgmt.model.User author : doc.getAuthors()) {
                            author.getUsername();
                        }
                    }
                }
            }
        }
        
        return folder;
    }
    
    /**
     * Update folder properties including authors
     * This method ensures all updates happen within a single transaction
     * @param folderId The folder ID
     * @param name The new name
     * @param path The new path
     * @param description The new description
     * @param owner The new owner
     * @param authors The new set of authors
     * @return The updated folder
     */
    @Transactional
    public Folder updateFolder(Long folderId, String name, String path, String description, 
                              com.docmgmt.model.User owner, java.util.Set<com.docmgmt.model.User> authors) {
        Folder folder = findById(folderId);
        
        folder.setName(name);
        folder.setPath(path);
        folder.setDescription(description);
        folder.setOwner(owner);
        
        // Update authors within transaction
        // Initialize the collection first
        if (folder.getAuthors() != null) {
            folder.getAuthors().size(); // Force initialization
            folder.getAuthors().clear();
        }
        if (authors != null && !authors.isEmpty()) {
            logger.info("Adding {} authors to folder {}", authors.size(), folder.getName());
            authors.forEach(author -> {
                logger.debug("Adding author: {}", author.getUsername());
                folder.addAuthor(author);
            });
        } else {
            logger.info("No authors to add to folder {}", folder.getName());
        }
        
        return save(folder);
    }
}
