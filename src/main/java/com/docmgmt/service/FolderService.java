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
}
