# Extending the Document Management Object Model

The system uses JPA's JOINED table inheritance strategy, allowing you to easily create new entity types that extend `SysObject` or `Document`.

## Inheritance Hierarchy

```
SysObject (base class - supports content and versioning)
├── Document (adds document-specific fields)
│   └── TripReport (adds trip-specific fields)
└── Folder (adds folder-specific fields)
```

## Key Features Inherited

All subclasses automatically get:
- ✅ **Content Management**: Attach files/content
- ✅ **Versioning**: Major and minor versions
- ✅ **Copy-on-Write**: Shared content until modified
- ✅ **Audit Trail**: Created/modified timestamps
- ✅ **Version History**: Parent version tracking

## How to Extend SysObject

### Step 1: Create the Entity

Create a new entity class extending `SysObject` or `Document`:

```java
@Entity
@Table(name = "your_table_name")
@DiscriminatorValue("YOUR_TYPE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class YourEntity extends SysObject {  // or extends Document
    
    // Add your custom fields
    @Column(name = "custom_field")
    private String customField;
    
    // Override copyAttributesTo to copy your custom fields during versioning
    @Override
    protected void copyAttributesTo(SysObject target) {
        super.copyAttributesTo(target);
        
        if (target instanceof YourEntity) {
            YourEntity typedTarget = (YourEntity) target;
            typedTarget.setCustomField(this.getCustomField());
        }
    }
}
```

### Step 2: Create the Repository

```java
@Repository
public interface YourEntityRepository extends BaseSysObjectRepository<YourEntity> {
    // Add custom query methods
    List<YourEntity> findByCustomField(String customField);
}
```

### Step 3: Create the Service

```java
@Service
public class YourEntityService extends AbstractSysObjectService<YourEntity, YourEntityRepository> {
    
    @Autowired
    public YourEntityService(YourEntityRepository repository) {
        super(repository);
    }
    
    // Add custom business logic
    @Transactional(readOnly = true)
    public List<YourEntity> findByCustomField(String customField) {
        return repository.findByCustomField(customField);
    }
}
```

## Example 1: Folder (extends SysObject)

**Purpose**: Organize documents hierarchically

**Custom Attributes**:
- `path`: File system-like path
- `description`: Folder description
- `isPublic`: Public/private flag
- `permissions`: Set of permission strings

**Features**:
- Can contain content (files, documents)
- Supports versioning
- Path-based searching
- Permission management

**Usage**:
```java
Folder folder = Folder.builder()
    .name("Q4 Reports")
    .path("/reports/2024/q4")
    .description("Fourth quarter financial reports")
    .isPublic(false)
    .build();

folder.addPermission("READ");
folder.addPermission("WRITE");
folderService.save(folder);

// Version the folder
Folder v2 = folderService.createMajorVersion(folder.getId());
```

## Example 2: TripReport (extends Document)

**Purpose**: Track business trip reports with budget tracking

**Custom Attributes**:
- `destination`: Trip destination
- `tripStartDate`/`tripEndDate`: Trip dates
- `purpose`: Trip purpose
- `budgetAmount`/`actualAmount`: Budget tracking
- `attendees`: Set of attendee names
- `summary`: Trip summary
- `followUpActions`: Action items

**Features**:
- Inherits all Document features (tags, author, documentType)
- Budget variance calculation
- Trip duration calculation
- Over-budget detection

**Usage**:
```java
TripReport report = TripReport.builder()
    .name("San Francisco Sales Conference")
    .documentType(Document.DocumentType.REPORT)
    .destination("San Francisco, CA")
    .tripStartDate(LocalDate.of(2024, 3, 15))
    .tripEndDate(LocalDate.of(2024, 3, 18))
    .purpose("Annual sales conference and client meetings")
    .budgetAmount(2500.00)
    .actualAmount(2450.00)
    .author("John Doe")
    .build();

report.addAttendee("Jane Smith");
report.addAttendee("Bob Johnson");
report.addTag("travel");
report.addTag("sales");

tripReportService.save(report);

// Check if over budget
boolean overBudget = report.isOverBudget(); // false - under budget!
long duration = report.getTripDurationDays(); // 4 days

// Attach receipts as content
Content receipt = contentService.createContentInDatabase(receiptFile, report);

// Version the report
TripReport v2 = tripReportService.createMajorVersion(report.getId());
```

## Best Practices

### 1. Always Override `copyAttributesTo()`
This ensures your custom fields are copied when creating versions:

```java
@Override
protected void copyAttributesTo(SysObject target) {
    super.copyAttributesTo(target);  // IMPORTANT: Call super first!
    
    if (target instanceof YourEntity) {
        YourEntity typedTarget = (YourEntity) target;
        // Copy all your custom fields
        typedTarget.setCustomField(this.getCustomField());
    }
}
```

### 2. Initialize Collections in Service
Prevent lazy loading issues:

```java
@Override
@Transactional(readOnly = true)
public YourEntity findById(Long id) {
    YourEntity entity = super.findById(id);
    // Touch collections to initialize them
    if (entity.getCustomCollection() != null) {
        entity.getCustomCollection().size();
    }
    if (entity.getContents() != null) {
        entity.getContents().size();
    }
    if (entity.getParentVersion() != null) {
        entity.getParentVersion().getName();
    }
    return entity;
}
```

### 3. Use EAGER Fetching for Small Collections
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "your_collection_table")
private Set<String> yourCollection = new HashSet<>();
```

### 4. Add Business Logic Methods
Encapsulate calculations and logic in the entity:

```java
public Double calculateCustomMetric() {
    // Your business logic here
    return someValue * 1.25;
}

public boolean meetsCondition() {
    return customField != null && customField.length() > 10;
}
```

## Database Schema

The JOINED inheritance strategy creates:
- **sys_object table**: Common fields (id, name, version, timestamps)
- **document table**: Document-specific fields (joins to sys_object)
- **folder table**: Folder-specific fields (joins to sys_object)
- **trip_report table**: TripReport-specific fields (joins to document, which joins to sys_object)

## Testing

Test versioning and content:

```java
// Create and version
YourEntity original = yourService.save(newEntity);
Content content = contentService.createContentInDatabase(file, original);

// Create version - content is cloned
YourEntity v2 = yourService.createMajorVersion(original.getId());

// Both have content initially
assertEquals(1, original.getContents().size());
assertEquals(1, v2.getContents().size());

// Content IDs are different (cloned)
assertNotEquals(
    original.getContents().iterator().next().getId(),
    v2.getContents().iterator().next().getId()
);

// Modify v2 content - copy-on-write
Content newContent = contentService.createContentInDatabase(newFile, v2);

// Original unchanged
assertEquals(1, original.getContents().size());
```

## Summary

The system's extensible architecture allows you to:
1. Create new entity types by extending `SysObject` or `Document`
2. Add custom fields and business logic
3. Automatically inherit content management and versioning
4. Maintain type safety with generics
5. Use Spring Data JPA for repository methods

All without modifying the core framework!
