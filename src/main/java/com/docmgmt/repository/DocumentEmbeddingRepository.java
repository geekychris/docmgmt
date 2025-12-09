package com.docmgmt.repository;

import com.docmgmt.model.Document;
import com.docmgmt.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {
    
    Optional<DocumentEmbedding> findByDocument(Document document);
    
    Optional<DocumentEmbedding> findByDocumentId(Long documentId);
    
    void deleteByDocument(Document document);
}
