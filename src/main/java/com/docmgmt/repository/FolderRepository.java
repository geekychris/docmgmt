package com.docmgmt.repository;

import com.docmgmt.model.Folder;
import com.docmgmt.model.SysObject;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Folder entities
 */
@Repository
public interface FolderRepository extends BaseSysObjectRepository<Folder> {
    
    /**
     * Find folders by path
     * @param path The folder path
     * @return List of folders with the given path
     */
    List<Folder> findByPath(String path);
    
    /**
     * Find folders by path starting with a prefix
     * @param pathPrefix The path prefix
     * @return List of folders with paths starting with the prefix
     */
    List<Folder> findByPathStartingWith(String pathPrefix);
    
    /**
     * Find public folders
     * @param isPublic Whether the folder is public
     * @return List of public or private folders
     */
    List<Folder> findByIsPublic(Boolean isPublic);
    
    /**
     * Find root folders (folders with no parent)
     * @return List of root folders
     */
    List<Folder> findByParentFolderIsNull();
    
    /**
     * Find child folders of a parent folder
     * @param parentFolder The parent folder
     * @return List of child folders
     */
    List<Folder> findByParentFolder(Folder parentFolder);
    
    /**
     * Find folders containing a specific SysObject
     * @param item The SysObject to search for
     * @return List of folders containing the item
     */
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Folder f JOIN f.items i WHERE i = :item")
    List<Folder> findByItemsContaining(@org.springframework.data.repository.query.Param("item") SysObject item);
}
