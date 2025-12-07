package com.docmgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "sys_object")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "object_type")
@DiscriminatorValue("BASE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(exclude = {"contents", "parentVersion", "owner", "authors"})
@ToString(exclude = {"contents", "parentVersion", "owner", "authors"})
public class SysObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    @Column(nullable = false, name = "major_version")
    @Builder.Default
    private Integer majorVersion = 1;

    @Column(nullable = false, name = "minor_version")
    @Builder.Default
    private Integer minorVersion = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_version_id")
    private SysObject parentVersion;

    @OneToMany(mappedBy = "sysObject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Content> contents = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "sys_object_authors",
        joinColumns = @JoinColumn(name = "sys_object_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> authors = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    @Builder.Default
    private LocalDateTime modifiedAt = LocalDateTime.now();
    /**
     * Add an author to this SysObject
     * @param author The user to add as author
     * @return this object for method chaining
     */
    public SysObject addAuthor(User author) {
        getAuthors().add(author);
        return this;
    }

    /**
     * Remove an author from this SysObject
     * @param author The user to remove as author
     * @return this object for method chaining
     */
    public SysObject removeAuthor(User author) {
        getAuthors().remove(author);
        return this;
    }

    /**
     * Add content to this SysObject
     * @param content The content to add
     * @return this object for method chaining
     */
    public SysObject addContent(Content content) {
        getContents().add(content);
        content.setSysObject(this);
        return this;
    }

    /**
     * Remove content from this SysObject
     * @param content The content to remove
     * @return this object for method chaining
     */
    public SysObject removeContent(Content content) {
        getContents().remove(content);
        content.setSysObject(null);
        return this;
    }
    /**
     * Creates a new major version of this SysObject
     * Increments the major version and resets minor version to 0
     * Copies all attributes but creates a reference to the parent
     * @return a new version of this SysObject
     */
    public SysObject createMajorVersion() {
        SysObject newVersion;
        try {
            newVersion = this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new version", e);
        }
        
        copyAttributesTo(newVersion);
        newVersion.setId(null);
        newVersion.setParentVersion(this);
        newVersion.setMajorVersion(this.getMajorVersion() + 1);
        newVersion.setMinorVersion(0);
        newVersion.setCreatedAt(null);
        newVersion.setModifiedAt(null);
        
        // Share the content objects
        for (Content content : this.getContents()) {
            // Create a clone of the content for the new version
            Content sharedContent = content.createCloneForSysObject(newVersion);
            newVersion.addContent(sharedContent);
        }
        
        return newVersion;
    }

    /**
     * Creates a new minor version of this SysObject
     * Updates only the minor version
     * Copies all attributes but creates a reference to the parent
     * @return a new version of this SysObject
     */
    public SysObject createMinorVersion() {
        SysObject newVersion;
        try {
            newVersion = this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new version", e);
        }
        
        copyAttributesTo(newVersion);
        newVersion.setId(null);
        newVersion.setParentVersion(this);
        newVersion.setMinorVersion(this.getMinorVersion() + 1);
        newVersion.setCreatedAt(null);
        newVersion.setModifiedAt(null);
        
        // Share the content objects
        for (Content content : this.getContents()) {
            // Create a clone of the content for the new version
            Content sharedContent = content.createCloneForSysObject(newVersion);
            newVersion.addContent(sharedContent);
        }
        
        return newVersion;
    }

    /**
     * Copy common attributes to another SysObject
     * This method should be overridden by subclasses to copy their specific attributes
     * @param target the target SysObject to copy attributes to
     */
    protected void copyAttributesTo(SysObject target) {
        target.setName(this.getName());
        target.setMajorVersion(this.getMajorVersion());
        target.setMinorVersion(this.getMinorVersion());
        target.setOwner(this.getOwner());
        // Copy authors
        for (User author : this.getAuthors()) {
            target.addAuthor(author);
        }
        // Note: Contents are handled separately in createMajorVersion and createMinorVersion methods
    }
}

