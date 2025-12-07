# User Entity and Ownership/Authorship Feature

## Overview

The Document Management System now includes a `User` entity that extends `SysObject`, along with ownership and authorship relationships for all SysObjects. This enables tracking document owners and multiple authors, supporting collaborative document management scenarios.

## User Entity

### Description

`User` represents a user in the document management system. As a subclass of `SysObject`, users inherit versioning capabilities and can themselves have owners and authors (useful for organizational hierarchies or user profile management).

### Database Schema

- **Table Name**: `app_user` (avoiding SQL reserved keyword "user")
- **Discriminator Value**: `USER`
- **Inherits from**: `sys_object` table (JOINED inheritance strategy)

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| username | String | NOT NULL, UNIQUE, max 50 chars | Unique username for the user |
| email | String | NOT NULL, UNIQUE, valid email | User's email address |
| first_name | String | max 100 chars | User's first name (optional) |
| last_name | String | max 100 chars | User's last name (optional) |
| is_active | Boolean | default TRUE | Whether the user account is active |

### Key Features

1. **Unique Identifiers**: Username and email must be unique across all users
2. **Email Validation**: Built-in validation for proper email format
3. **Full Name Support**: `getFullName()` method returns formatted name or username as fallback
4. **Account Status**: Active/inactive flag for user management
5. **Versioning Support**: Can create major/minor versions with unique username/email per version

## Ownership and Authorship

### SysObject Extensions

All `SysObject` entities (Documents, Folders, Users, etc.) now support:

1. **Owner** - A single User who owns the object
2. **Authors** - A collection of Users who are authors of the object

### Database Schema

#### Owner Relationship

- **Type**: Many-to-One
- **Column**: `owner_id` (foreign key to app_user)
- **Nullable**: Yes (owner is optional)
- **Fetch**: Lazy

#### Authors Relationship

- **Type**: Many-to-Many
- **Join Table**: `sys_object_authors`
  - `sys_object_id` (foreign key to sys_object)
  - `user_id` (foreign key to app_user)
- **Fetch**: Lazy

## API Usage

### Creating Users

```java
// Create a basic user
User user = User.builder()
    .name("johndoe")
    .username("johndoe")
    .email("john.doe@example.com")
    .firstName("John")
    .lastName("Doe")
    .isActive(true)
    .build();

User savedUser = userService.createUser(user);
```

### Finding Users

```java
// Find by ID
User user = userService.findById(1L);

// Find by username
Optional<User> user = userService.findByUsername("johndoe");

// Find by email
Optional<User> user = userService.findByEmail("john.doe@example.com");

// Check existence
boolean exists = userService.existsByUsername("johndoe");
boolean exists = userService.existsByEmail("john.doe@example.com");

// Find all users
List<User> users = userService.findAll();
```

### Updating Users

```java
User updatedInfo = User.builder()
    .username("newusername")
    .email("newemail@example.com")
    .firstName("Jonathan")
    .lastName("Doe")
    .isActive(false)
    .build();

User updated = userService.updateUser(userId, updatedInfo);
```

### User Activation/Deactivation

```java
// Deactivate user
User deactivated = userService.deactivateUser(userId);

// Activate user
User activated = userService.activateUser(userId);
```

### Setting Document Owner

```java
User owner = userService.findById(1L);
Document document = documentService.findById(1L);

document.setOwner(owner);
documentService.save(document);
```

### Managing Authors

```java
User author1 = userService.findById(1L);
User author2 = userService.findById(2L);
Document document = documentService.findById(1L);

// Add authors
document.addAuthor(author1);
document.addAuthor(author2);
documentService.save(document);

// Remove author
document.removeAuthor(author1);
documentService.save(document);

// Access authors
Set<User> authors = document.getAuthors();
```

### Combined Owner and Authors

```java
User owner = userService.findByUsername("manager").get();
User author1 = userService.findByUsername("writer1").get();
User author2 = userService.findByUsername("writer2").get();

Document document = Article.builder()
    .name("Collaborative Article")
    .description("Written by multiple authors")
    .build();

document.setOwner(owner);
document.addAuthor(author1);
document.addAuthor(author2);

Document saved = documentService.save(document);
```

## Versioning Behavior

### Document Versioning

When creating a new version of a document:
- Owner is **copied** to the new version
- All authors are **copied** to the new version
- Owner and authors can be modified independently in each version

```java
Document v1 = documentService.findById(1L);
v1.setOwner(user1);
v1.addAuthor(user2);

Document v2 = documentService.createMajorVersion(v1.getId());
// v2 has same owner and authors as v1
assertEquals(user1.getId(), v2.getOwner().getId());
assertTrue(v2.getAuthors().contains(user2));
```

### User Versioning

When creating a new version of a user:
- Username and email are **modified** with version suffix to maintain uniqueness
- First name, last name, and active status are **copied**
- Owner and authors are **copied**

```java
User v1 = userService.findById(1L);
// v1.username = "johndoe", v1.email = "john@example.com"

User v2 = userService.createMajorVersion(v1.getId());
// v2.username = "johndoe_v2.0"
// v2.email = "john_v2.0@example.com"
// v2 major version = 2, minor version = 0
```

## Use Cases

### 1. Document Ownership Tracking

```java
// Find all documents owned by a specific user
User owner = userService.findByUsername("manager").get();
List<Document> ownedDocs = documentService.findAll().stream()
    .filter(doc -> doc.getOwner() != null && doc.getOwner().equals(owner))
    .collect(Collectors.toList());
```

### 2. Multi-Author Collaboration

```java
// Create collaborative document
Document article = Article.builder()
    .name("Research Paper")
    .build();

User lead = userService.findByUsername("lead_researcher").get();
User researcher1 = userService.findByUsername("researcher1").get();
User researcher2 = userService.findByUsername("researcher2").get();

article.setOwner(lead);
article.addAuthor(lead);
article.addAuthor(researcher1);
article.addAuthor(researcher2);

documentService.save(article);
```

### 3. Organizational Hierarchy

```java
// Users can have owners (e.g., managers)
User employee = userService.findByUsername("employee1").get();
User manager = userService.findByUsername("manager").get();

employee.setOwner(manager);
userService.save(employee);
```

### 4. Access Control Preparation

The owner and authors relationships provide a foundation for implementing:
- Permission checks (owner has full access)
- Author-based access control
- Sharing and collaboration features
- Audit trails

## Service Layer Enhancements

### UserService

- `createUser(User)` - Create with uniqueness validation
- `updateUser(Long, User)` - Update with conflict checking
- `findByUsername(String)` - Find by username
- `findByEmail(String)` - Find by email
- `existsByUsername(String)` - Check existence
- `existsByEmail(String)` - Check existence
- `activateUser(Long)` - Activate user account
- `deactivateUser(Long)` - Deactivate user account

### DocumentService Updates

All find methods now initialize owner and authors collections:
- `findById(Long)` - Includes owner and authors
- `findAll()` - Includes owner and authors for all documents
- `findAllLatestVersions()` - Includes owner and authors

### UserService Initialization

All find methods now initialize owner and authors collections:
- `findById(Long)` - Includes owner and authors
- `findAll()` - Includes owner and authors for all users

## Testing

### Test Coverage

**UserServiceTest** - 19 tests covering:
- User creation with validation
- Duplicate username/email prevention
- Finding users by various criteria
- User updates with conflict detection
- Activation/deactivation
- Full name generation
- Versioning with unique constraints

**OwnershipAndAuthorsIntegrationTest** - 13 tests covering:
- Setting document owner
- Adding/removing authors
- Multiple authors management
- Owner can also be author
- Version inheritance of owner/authors
- One user owning multiple documents
- One user authoring multiple documents
- Users having owners and authors (organizational hierarchy)

### Running Tests

```bash
# Run all tests
mvn test

# Run User tests only
mvn test -Dtest=UserServiceTest

# Run ownership/authorship tests
mvn test -Dtest=OwnershipAndAuthorsIntegrationTest
```

## Database Migration Notes

### Schema Changes

When deploying to existing systems:

1. **New table**: `app_user` created (extends sys_object)
2. **New column**: `owner_id` added to `sys_object` table
3. **New table**: `sys_object_authors` join table created
4. **Constraints**: Unique constraints on username and email

### Backward Compatibility

- Owner field is **nullable** - existing SysObjects don't require an owner
- Authors collection is **empty by default** - existing SysObjects have no authors
- No data migration required for existing documents

## Performance Considerations

### Lazy Loading

Both owner and authors use `FetchType.LAZY`:
- Only loaded when explicitly accessed
- Service methods initialize these collections proactively
- Prevents N+1 query issues in typical use cases

### Indexes

Automatically created indexes:
- Primary key on `app_user.id`
- Unique index on `app_user.username`
- Unique index on `app_user.email`
- Foreign key indexes on join tables

### Query Optimization

```java
// Efficient: Single query with initialization
Document doc = documentService.findById(1L);
// Owner and authors already initialized

// Inefficient: Would cause lazy loading issues
Document doc = documentRepository.findById(1L).get();
// Owner and authors not initialized, may cause LazyInitializationException
```

## Best Practices

1. **Use Service Methods**: Always use `UserService` and `DocumentService` methods instead of repositories directly
2. **Validate Uniqueness**: Use `createUser()` instead of `save()` for new users to get validation
3. **Handle Optional Owner**: Always check if owner is null before accessing
4. **Bulk Operations**: When working with collections, use service methods that initialize relationships
5. **Version Awareness**: Remember that user versions have modified usernames/emails

## Security Considerations

This implementation provides **data modeling** for ownership and authorship. For production use, consider adding:

1. **Authentication**: Integration with Spring Security
2. **Authorization**: Role-based or owner-based access control
3. **Password Management**: Add password field with encryption
4. **Session Management**: Track user sessions
5. **Audit Logging**: Log owner/author changes

## Future Enhancements

Potential extensions:
- **Groups**: Create user groups for bulk permissions
- **Roles**: Add role-based access control
- **Sharing**: Implement document sharing between users
- **Notifications**: Notify owners/authors of changes
- **Permissions**: Fine-grained permission system
- **Delegation**: Temporary ownership transfer

## Summary

The User entity and ownership/authorship features provide:
- ✅ Complete user management (CRUD operations)
- ✅ Unique username and email constraints
- ✅ Owner relationship for all SysObjects
- ✅ Multi-author support for collaborative documents
- ✅ Version inheritance of owner and authors
- ✅ Lazy loading with proper initialization
- ✅ Comprehensive test coverage (31 new tests, 155 total)
- ✅ Backward compatible with existing data
- ✅ Foundation for future security and permission features
