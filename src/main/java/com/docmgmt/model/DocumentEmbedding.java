package com.docmgmt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores vector embeddings for document similarity search
 */
@Entity
@Table(name = "document_embeddings", indexes = {
    @Index(name = "idx_doc_embedding_doc", columnList = "document_id")
})
public class DocumentEmbedding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;
    
    @Column(name = "embedding", columnDefinition = "TEXT", nullable = false)
    private String embedding; // JSON array of floats
    
    @Column(name = "content_hash", length = 64)
    private String contentHash; // SHA-256 hash to detect content changes
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Document getDocument() {
        return document;
    }
    
    public void setDocument(Document document) {
        this.document = document;
    }
    
    public String getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
