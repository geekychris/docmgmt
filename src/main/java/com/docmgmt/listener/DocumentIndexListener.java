package com.docmgmt.listener;

import com.docmgmt.model.Document;
import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.service.DocumentService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

/**
 * JPA entity listener that automatically reindexes documents when they are
 * created, updated, or deleted. This ensures the search index stays synchronized
 * with the database without requiring manual intervention.
 */
@Component
public class DocumentIndexListener {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexListener.class);
    
    private static LuceneIndexService indexService;
    private static DocumentService documentService;
    
    /**
     * Spring-managed setter for dependency injection
     * Uses static field to work with JPA entity listeners
     */
    @Autowired
    public void setIndexService(LuceneIndexService indexService) {
        DocumentIndexListener.indexService = indexService;
    }
    
    /**
     * Spring-managed setter for document service dependency injection
     */
    @Autowired
    public void setDocumentService(DocumentService documentService) {
        DocumentIndexListener.documentService = documentService;
    }
    
    /**
     * Called after a document is persisted (created)
     * Automatically indexes the new document
     */
    @PostPersist
    public void onPostPersist(Document document) {
        reindexDocument(document, "created");
    }
    
    /**
     * Called after a document is updated
     * Automatically reindexes the document with its new content/attributes
     */
    @PostUpdate
    public void onPostUpdate(Document document) {
        reindexDocument(document, "updated");
    }
    
    /**
     * Called after a document is removed
     * Automatically removes the document from the search index
     */
    @PostRemove
    public void onPostRemove(Document document) {
        if (indexService != null && document.getId() != null) {
            try {
                indexService.removeDocument(document.getId());
                logger.debug("Document removed from index: {} (ID: {})", document.getName(), document.getId());
            } catch (IOException e) {
                logger.error("Failed to remove document from index: {} (ID: {})", 
                    document.getName(), document.getId(), e);
            }
        }
    }
    
    /**
     * Helper method to reindex a document
     * Defers indexing until after transaction commit to ensure all data is persisted
     * Uses document ID instead of entity to avoid lazy initialization issues
     */
    private void reindexDocument(Document document, String operation) {
        if (indexService != null && documentService != null && document.getId() != null) {
            final Long documentId = document.getId();
            final String documentName = document.getName();
            
            // Register a synchronization to index after transaction commit
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            // Reload the document in a new transaction to ensure all lazy relationships are available
                            Document freshDocument = documentService.findById(documentId);
                            indexService.indexDocument(freshDocument);
                            logger.debug("Document automatically reindexed after {}: {} (ID: {})", 
                                operation, documentName, documentId);
                        } catch (Exception e) {
                            logger.error("Failed to reindex document after {}: {} (ID: {})", 
                                operation, documentName, documentId, e);
                        }
                    }
                });
            } else {
                // Fallback if no transaction is active (shouldn't happen in normal usage)
                try {
                    Document freshDocument = documentService.findById(documentId);
                    indexService.indexDocument(freshDocument);
                    logger.debug("Document automatically reindexed after {} (no transaction): {} (ID: {})", 
                        operation, documentName, documentId);
                } catch (Exception e) {
                    logger.error("Failed to reindex document after {} (no transaction): {} (ID: {})", 
                        operation, documentName, documentId, e);
                }
            }
        }
    }
}
