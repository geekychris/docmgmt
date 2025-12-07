package com.docmgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents a user in the document management system.
 * Users can be owners and authors of SysObjects.
 */
@Entity
@Table(name = "app_user", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
@DiscriminatorValue("USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class User extends SysObject {

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Get the user's full name
     * @return full name or username if names not provided
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    /**
     * Check if user account is active
     * @return true if user is active
     */
    public boolean isAccountActive() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        if (target instanceof User) {
            User userTarget = (User) target;
            // Note: At this point, target has the OLD version numbers copied from source
            // The actual version will be set later in createMajorVersion/createMinorVersion
            // So we just copy the base username/email without version suffix
            // The version suffix will be added when the version is finalized
            userTarget.setUsername(this.username);
            userTarget.setEmail(this.email);
            userTarget.setFirstName(this.firstName);
            userTarget.setLastName(this.lastName);
            userTarget.setIsActive(this.isActive);
        }
    }

    @Override
    public SysObject createMajorVersion() {
        User newVersion = (User) super.createMajorVersion();
        // Append version suffix to maintain uniqueness
        String versionSuffix = "_v" + newVersion.getMajorVersion() + "." + newVersion.getMinorVersion();
        newVersion.setUsername(this.username + versionSuffix);
        newVersion.setEmail(this.email.replace("@", versionSuffix + "@"));
        return newVersion;
    }

    @Override
    public SysObject createMinorVersion() {
        User newVersion = (User) super.createMinorVersion();
        // Append version suffix to maintain uniqueness
        String versionSuffix = "_v" + newVersion.getMajorVersion() + "." + newVersion.getMinorVersion();
        newVersion.setUsername(this.username + versionSuffix);
        newVersion.setEmail(this.email.replace("@", versionSuffix + "@"));
        return newVersion;
    }
}
