package com.docmgmt.service;

import com.docmgmt.model.User;
import com.docmgmt.repository.UserRepository;
import com.docmgmt.util.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com", "John", "Doe");

        // When
        User savedUser = userService.createUser(user);

        // Then
        assertNotNull(savedUser.getId());
        assertEquals("johndoe", savedUser.getUsername());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        assertTrue(savedUser.isAccountActive());
    }

    @Test
    void testCreateUserWithDuplicateUsername() {
        // Given
        User user1 = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        userService.createUser(user1);

        User user2 = TestDataBuilder.createUser(null, "johndoe", "different@example.com");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(user2);
        });
        assertTrue(exception.getMessage().contains("Username already exists"));
    }

    @Test
    void testCreateUserWithDuplicateEmail() {
        // Given
        User user1 = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        userService.createUser(user1);

        User user2 = TestDataBuilder.createUser(null, "janedoe", "john@example.com");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(user2);
        });
        assertTrue(exception.getMessage().contains("Email already exists"));
    }

    @Test
    void testFindById() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        User savedUser = userService.createUser(user);

        // When
        User foundUser = userService.findById(savedUser.getId());

        // Then
        assertNotNull(foundUser);
        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals("johndoe", foundUser.getUsername());
    }

    @Test
    void testFindByIdNotFound() {
        // When/Then
        assertThrows(EntityNotFoundException.class, () -> {
            userService.findById(999L);
        });
    }

    @Test
    void testFindByUsername() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        userService.createUser(user);

        // When
        Optional<User> foundUser = userService.findByUsername("johndoe");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("johndoe", foundUser.get().getUsername());
    }

    @Test
    void testFindByUsernameNotFound() {
        // When
        Optional<User> foundUser = userService.findByUsername("nonexistent");

        // Then
        assertFalse(foundUser.isPresent());
    }

    @Test
    void testFindByEmail() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        userService.createUser(user);

        // When
        Optional<User> foundUser = userService.findByEmail("john@example.com");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("john@example.com", foundUser.get().getEmail());
    }

    @Test
    void testExistsByUsername() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        userService.createUser(user);

        // When/Then
        assertTrue(userService.existsByUsername("johndoe"));
        assertFalse(userService.existsByUsername("nonexistent"));
    }

    @Test
    void testExistsByEmail() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        userService.createUser(user);

        // When/Then
        assertTrue(userService.existsByEmail("john@example.com"));
        assertFalse(userService.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testFindAll() {
        // Given
        User user1 = TestDataBuilder.createUser(null, "user1", "user1@example.com");
        User user2 = TestDataBuilder.createUser(null, "user2", "user2@example.com");
        User user3 = TestDataBuilder.createUser(null, "user3", "user3@example.com");
        userService.createUser(user1);
        userService.createUser(user2);
        userService.createUser(user3);

        // When
        List<User> users = userService.findAll();

        // Then
        assertEquals(3, users.size());
    }

    @Test
    void testUpdateUser() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com", "John", "Doe");
        User savedUser = userService.createUser(user);

        // When
        User updatedUser = User.builder()
                .username("johndoe2")
                .email("john.doe@example.com")
                .firstName("Jonathan")
                .lastName("Doe")
                .name("johndoe2")
                .isActive(false)
                .build();
        User result = userService.updateUser(savedUser.getId(), updatedUser);

        // Then
        assertEquals("johndoe2", result.getUsername());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("Jonathan", result.getFirstName());
        assertFalse(result.isAccountActive());
    }

    @Test
    void testUpdateUserWithConflictingUsername() {
        // Given
        User user1 = TestDataBuilder.createUser(null, "user1", "user1@example.com");
        User user2 = TestDataBuilder.createUser(null, "user2", "user2@example.com");
        userService.createUser(user1);
        User savedUser2 = userService.createUser(user2);

        // When/Then
        User updatedUser = User.builder()
                .username("user1")  // Try to use user1's username
                .email("user2@example.com")
                .name("user1")
                .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(savedUser2.getId(), updatedUser);
        });
    }

    @Test
    void testDeactivateUser() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        User savedUser = userService.createUser(user);
        assertTrue(savedUser.isAccountActive());

        // When
        User deactivatedUser = userService.deactivateUser(savedUser.getId());

        // Then
        assertFalse(deactivatedUser.isAccountActive());
    }

    @Test
    void testActivateUser() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        user.setIsActive(false);
        User savedUser = userService.save(user);
        assertFalse(savedUser.isAccountActive());

        // When
        User activatedUser = userService.activateUser(savedUser.getId());

        // Then
        assertTrue(activatedUser.isAccountActive());
    }

    @Test
    void testGetFullName() {
        // Test with both first and last name
        User user1 = TestDataBuilder.createUser(null, "user1", "user1@example.com", "John", "Doe");
        assertEquals("John Doe", user1.getFullName());

        // Test with only first name
        User user2 = TestDataBuilder.createUser(null, "user2", "user2@example.com", "John", null);
        assertEquals("John", user2.getFullName());

        // Test with only last name
        User user3 = TestDataBuilder.createUser(null, "user3", "user3@example.com", null, "Doe");
        assertEquals("Doe", user3.getFullName());

        // Test with no names (should return username)
        User user4 = TestDataBuilder.createUser(null, "user4", "user4@example.com", null, null);
        assertEquals("user4", user4.getFullName());
    }

    @Test
    void testDeleteUser() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com");
        User savedUser = userService.createUser(user);
        Long userId = savedUser.getId();

        // When
        userService.delete(userId);

        // Then
        assertThrows(EntityNotFoundException.class, () -> {
            userService.findById(userId);
        });
    }

    @Test
    void testCreateMajorVersion() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com", "John", "Doe");
        User savedUser = userService.save(user);

        // When
        User newVersion = userService.createMajorVersion(savedUser.getId());

        // Then
        assertNotNull(newVersion.getId());
        assertNotEquals(savedUser.getId(), newVersion.getId());
        assertEquals(2, newVersion.getMajorVersion());
        assertEquals(0, newVersion.getMinorVersion());
        assertEquals(savedUser.getId(), newVersion.getParentVersion().getId());
        // Username and email are versioned to maintain uniqueness
        assertEquals("johndoe_v2.0", newVersion.getUsername());
        assertEquals("john_v2.0@example.com", newVersion.getEmail());
        assertEquals("John", newVersion.getFirstName());
        assertEquals("Doe", newVersion.getLastName());
    }

    @Test
    void testCreateMinorVersion() {
        // Given
        User user = TestDataBuilder.createUser(null, "johndoe", "john@example.com", "John", "Doe");
        User savedUser = userService.save(user);

        // When
        User newVersion = userService.createMinorVersion(savedUser.getId());

        // Then
        assertNotNull(newVersion.getId());
        assertNotEquals(savedUser.getId(), newVersion.getId());
        assertEquals(1, newVersion.getMajorVersion());
        assertEquals(1, newVersion.getMinorVersion());
        assertEquals(savedUser.getId(), newVersion.getParentVersion().getId());
        // Username and email are versioned to maintain uniqueness
        assertEquals("johndoe_v1.1", newVersion.getUsername());
        assertEquals("john_v1.1@example.com", newVersion.getEmail());
        assertEquals("John", newVersion.getFirstName());
        assertEquals("Doe", newVersion.getLastName());
    }
}
