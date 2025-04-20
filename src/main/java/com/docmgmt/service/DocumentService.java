package com.docmgmt.service;

import com.docmgmt.model.Document;
import com.docmgmt.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for Document entity operations
 */
@Service
public class DocumentService extends AbstractSysObjectService<Document, DocumentRepository> {
    @Autowired
    public DocumentService(DocumentRepository repository) {
        super(repository);
    }
    
    /**
     * Override findAll to ensure tags are initialized
     * @return List of all Document entities
     */
    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        List<Document> documents = super.findAll();
        // Initialize tags collections
        documents.forEach(doc -> doc.getTags().size());
        return documents;
    }
    
    /**
     * Override findById to ensure tags are initialized
     * @param id The document ID
     * @return The found document
     */
    @Override
    @Transactional(readOnly = true)
    public Document findById(Long id) {
        Document document = super.findById(id);
        // Initialize tags collection
        // Initialize tags collection
        document.getTags().size();
        return document;
    }
    
    /**
     * Find documents by document type
     * @param documentType The document type
     * @return List of matching documents
     */
    @Transactional(readOnly = true)
    public List<Document> findByDocumentType(Document.DocumentType documentType) {
        return repository.findByDocumentType(documentType);
    }
    
    /**
     * Find documents by author
     * @param author The author name
     * @return List of matching documents
     */
    @Transactional(readOnly = true)
    public List<Document> findByAuthor(String author) {
        return repository.findByAuthor(author);
    }
    
    /**
     * Find documents containing a specific tag
     * @param tag The tag to search for
     * @return List of matching documents
     */
    @Transactional(readOnly = true)
    public List<Document> findByTag(String tag) {
        return repository.findByTagsContaining(tag);
    }
    
    /**
     * Find documents with keywords containing the search term
     * @param searchTerm The search term
     * @return List of matching documents
     */
    @Transactional(readOnly = true)
    public List<Document> findByKeywords(String searchTerm) {
        return repository.findByKeywordsContaining(searchTerm);
    }
    
    /**
     * Find the latest version of a document by type and name
     * @param documentType The document type
     * @param name The document name
     * @return Optional containing the document if found
     */
    @Transactional(readOnly = true)
    public Optional<Document> findLatestVersionByTypeAndName(Document.DocumentType documentType, String name) {
        return repository.findByDocumentTypeAndNameOrderByMajorVersionDescMinorVersionDesc(documentType, name);
    }
}

