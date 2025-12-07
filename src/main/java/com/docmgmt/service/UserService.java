package com.docmgmt.service;

import com.docmgmt.model.User;
import com.docmgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for User entity operations
 */
@Service
public class UserService extends AbstractSysObjectService<User, UserRepository> {
    
    @Autowired
    public UserService(UserRepository repository) {
        super(repository);
    }
    
    /**
     * Override findAll to ensure collections are initialized
     * @return List of all User entities
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        List<User> users = super.findAll();
        // Initialize contents collections, owner, authors, and parent version
        users.forEach(user -> {
            if (user.getContents() != null) {
                user.getContents().size();
            }
            if (user.getOwner() != null) {
                user.getOwner().getName();
            }
            if (user.getAuthors() != null) {
                user.getAuthors().size();
            }
            // Touch parent version to initialize it
            if (user.getParentVersion() != null) {
                user.getParentVersion().getName();
            }
        });
        return users;
    }
    
    /**
     * Override findById to ensure collections are initialized
     * @param id The user ID
     * @return The found user
     */
    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        User user = super.findById(id);
        // Initialize contents collections, owner, authors, and parent version
        if (user.getContents() != null) {
            user.getContents().size();
        }
        if (user.getOwner() != null) {
            user.getOwner().getName();
        }
        if (user.getAuthors() != null) {
            user.getAuthors().size();
        }
        // Touch parent version to initialize it
        if (user.getParentVersion() != null) {
            user.getParentVersion().getName();
        }
        return user;
    }
    
    /**
     * Find a user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }
    
    /**
     * Find a user by email
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }
    
    /**
     * Check if a username exists
     * @param username the username to check
     * @return true if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }
    
    /**
     * Check if an email exists
     * @param email the email to check
     * @return true if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
    
    /**
     * Create a new user with validation
     * @param user the user to create
     * @return the created user
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public User createUser(User user) {
        if (existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        if (existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        return save(user);
    }
    
    /**
     * Update a user's information
     * @param id the user ID
     * @param updatedUser the updated user information
     * @return the updated user
     * @throws IllegalArgumentException if username or email conflict with another user
     */
    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = findById(id);
        
        // Check username uniqueness if changed
        if (!existingUser.getUsername().equals(updatedUser.getUsername())) {
            if (existsByUsername(updatedUser.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + updatedUser.getUsername());
            }
        }
        
        // Check email uniqueness if changed
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            if (existsByEmail(updatedUser.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + updatedUser.getEmail());
            }
        }
        
        // Update fields
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setIsActive(updatedUser.getIsActive());
        existingUser.setName(updatedUser.getName());
        
        return save(existingUser);
    }
    
    /**
     * Deactivate a user
     * @param id the user ID
     * @return the deactivated user
     */
    @Transactional
    public User deactivateUser(Long id) {
        User user = findById(id);
        user.setIsActive(false);
        return save(user);
    }
    
    /**
     * Activate a user
     * @param id the user ID
     * @return the activated user
     */
    @Transactional
    public User activateUser(Long id) {
        User user = findById(id);
        user.setIsActive(true);
        return save(user);
    }
}
