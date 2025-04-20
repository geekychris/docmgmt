package com.docmgmt.repository;

import com.docmgmt.model.Document;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Document entity
 */
@Repository
public interface DocumentRepository extends BaseSysObjectRepository<Document> {
    
    /**
     * Find documents by document type
     * @param documentType The document type
     * @return List of matching documents
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.tags WHERE d.documentType = :documentType")
    List<Document> findByDocumentType(@Param("documentType") Document.DocumentType documentType);
    
    /**
     * Find documents by author
     * @param author The author name
     * @return List of matching documents
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.tags WHERE d.author = :author")
    List<Document> findByAuthor(@Param("author") String author);
    
    /**
     * Find documents containing a specific tag
     * @param tag The tag to search for
     * @return List of matching documents
     */
    @Query("SELECT DISTINCT d FROM Document d JOIN FETCH d.tags t WHERE t = :tag")
    List<Document> findByTagsContaining(@Param("tag") String tag);
    
    /**
     * Find documents with keywords containing the search term
     * @param searchTerm The search term
     * @return List of matching documents
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.tags WHERE d.keywords LIKE %:searchTerm%")
    List<Document> findByKeywordsContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Find documents by document type and name and latest version
     * @param documentType The document type
     * @param name The document name
     * @return Optional containing the matching document if found
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.tags " +
           "WHERE d.documentType = :documentType AND d.name = :name " +
           "ORDER BY d.majorVersion DESC, d.minorVersion DESC")
    Optional<Document> findByDocumentTypeAndNameOrderByMajorVersionDescMinorVersionDesc(
            @Param("documentType") Document.DocumentType documentType, @Param("name") String name);
}

