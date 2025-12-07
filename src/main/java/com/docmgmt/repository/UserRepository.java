package com.docmgmt.repository;

import com.docmgmt.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entities
 */
@Repository
public interface UserRepository extends BaseSysObjectRepository<User> {
    
    /**
     * Find a user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by email
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a username exists
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if an email exists
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);
}
