package com.docmgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "file_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = "contents")
@ToString(exclude = "contents")
public class FileStore {

    public enum Status {
        ACTIVE, INACTIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Root path is required")
    @Column(name = "root_path", nullable = false)
    private String rootPath;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy = "fileStore", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @JsonIgnore
    private Set<Content> contents = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    @Builder.Default
    private LocalDateTime modifiedAt = LocalDateTime.now();

    /**
     * Checks if this file store is active
     * @return true if the status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }

    /**
     * Gets the full path for a given relative storage path
     * @param relativePath the relative path within this file store
     * @return the full absolute path on the file system
     */
    public String getFullPath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return rootPath;
        }
        
        return rootPath + (rootPath.endsWith("/") ? "" : "/") + relativePath;
    }
}

