# Content Upload Enhancement Requirements

## Current State

The DocumentDetailDialog component currently:
- Shows a grid of existing content objects
- Allows viewing content
- Supports AI plugins and field extraction
- Does NOT have an "Upload Content" button

FolderView has an upload dialog (`openUploadContentDialogForFolder`) but it:
- Does NOT allow choosing Primary/Secondary rendition type
- Does NOT allow custom naming (defaults to filename)
- Only supports Database or File Store storage options

## Requirements

Users need to be able to:
1. **Upload content** from the Document Detail Dialog
2. **Choose rendition type**: Primary or Secondary
3. **Provide custom name**: Override the filename with a custom name
4. **Select storage location**: Database or File Store
5. **View updated content**: Grid should refresh after upload

## Implementation Plan

### 1. Add FileStoreService to DocumentDetailDialog

Currently DocumentDetailDialog doesn't have FileStoreService injected. Need to:
- Add FileStoreService parameter to constructor
- Store as instance variable
- Update all places that create DocumentDetailDialog to pass FileStoreService

### 2. Add "Upload Content" Button

In `initializeDialog()` method around line 292 (contentToolbar):
```java
Button uploadContentButton = new Button(\"Upload Content\", new Icon(VaadinIcon.UPLOAD));
uploadContentButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
uploadContentButton.addClickListener(e -> openUploadContentDialog(reloadedDoc));

HorizontalLayout contentToolbar = new HorizontalLayout(
    viewContentButton, uploadContentButton, extractFieldsButton, pluginsMenu);
```

### 3. Create Enhanced Upload Dialog Method

Add new method `openUploadContentDialog(Document document)` with:

**Form Fields**:
- **Content Name** (TextField): Custom name for the content object
  - Placeholder: "e.g., Annual Report 2024"
  - Initially empty (user must provide)
  
- **Rendition Type** (RadioButtonGroup): Primary or Secondary
  - Options: "Primary", "Secondary"
  - Default: "Primary"
  - Help text: "Primary content is the main file. Secondary content is derived (e.g., text extraction from PDF)"

- **Storage Type** (RadioButtonGroup): Database or File Store
  - Options: "Database", "File Store"
  - Default: "Database"
  
- **File Store** (ComboBox): Only visible if "File Store" selected
  - Items: All available file stores
  - Required if storage type is File Store

- **File Upload** (Upload component):
  - Accepted types: PDF, TXT, DOC, DOCX, images, etc.
  - Max files: 1
  - Shows upload status

**Logic**:
```java
private void openUploadContentDialog(Document document) {
    Dialog uploadDialog = new Dialog();
    uploadDialog.setWidth(\"600px\");
    
    H2 title = new H2(\"Upload Content\");
    
    // Content Name field
    TextField nameField = new TextField(\"Content Name\");
    nameField.setPlaceholder(\"e.g., Annual Report 2024\");
    nameField.setRequired(true);
    nameField.setWidthFull();
    
    // Rendition Type
    RadioButtonGroup<String> renditionType = new RadioButtonGroup<>();
    renditionType.setLabel(\"Rendition Type\");
    renditionType.setItems(\"Primary\", \"Secondary\");
    renditionType.setValue(\"Primary\");
    
    Span renditionHelp = new Span(
        \"Primary: Main content file. Secondary: Derived content (e.g., text from PDF).\");
    renditionHelp.getStyle()
        .set(\"font-size\", \"var(--lumo-font-size-s)\")
        .set(\"color\", \"var(--lumo-secondary-text-color)\");
    
    // Storage Type
    RadioButtonGroup<String> storageType = new RadioButtonGroup<>();
    storageType.setLabel(\"Storage Type\");
    storageType.setItems(\"Database\", \"File Store\");
    storageType.setValue(\"Database\");
    
    // File Store Selection
    ComboBox<FileStore> fileStoreCombo = new ComboBox<>(\"File Store\");
    fileStoreCombo.setItems(fileStoreService.findAll());
    fileStoreCombo.setItemLabelGenerator(FileStore::getName);
    fileStoreCombo.setVisible(false);
    fileStoreCombo.setWidthFull();
    
    storageType.addValueChangeListener(e -> {
        boolean isFileStore = \"File Store\".equals(e.getValue());
        fileStoreCombo.setVisible(isFileStore);
        fileStoreCombo.setRequired(isFileStore);
    });
    
    // File Upload
    MemoryBuffer buffer = new MemoryBuffer();
    Upload upload = new Upload(buffer);
    upload.setMaxFiles(1);
    
    Span uploadStatus = new Span();
    uploadStatus.setVisible(false);
    
    upload.addSucceededListener(event -> {
        uploadStatus.setText(\"File ready: \" + event.getFileName());
        uploadStatus.getStyle().set(\"color\", \"var(--lumo-success-color)\");
        uploadStatus.setVisible(true);
        
        // Auto-fill name from filename if empty
        if (nameField.isEmpty()) {
            nameField.setValue(event.getFileName());
        }
    });
    
    // Buttons
    Button cancelButton = new Button(\"Cancel\", e -> uploadDialog.close());
    
    Button saveButton = new Button(\"Upload\", e -> {
        // Validation
        if (nameField.isEmpty()) {
            Notification.show(\"Please provide a content name\", 3000, 
                Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        try {
            if (buffer.getInputStream().available() == 0) {
                Notification.show(\"Please select a file to upload\", 3000,
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Create MultipartFile from buffer
            byte[] fileBytes = buffer.getInputStream().readAllBytes();
            String fileName = nameField.getValue(); // Use custom name!
            String contentType = buffer.getFileData().getMimeType();
            
            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() { return fileName; }
                
                @Override
                public String getOriginalFilename() { return fileName; }
                
                @Override
                public String getContentType() { return contentType; }
                
                @Override
                public boolean isEmpty() { return fileBytes.length == 0; }
                
                @Override
                public long getSize() { return fileBytes.length; }
                
                @Override
                public byte[] getBytes() { return fileBytes; }
                
                @Override
                public java.io.InputStream getInputStream() { 
                    return new java.io.ByteArrayInputStream(fileBytes); 
                }
                
                @Override
                public void transferTo(java.io.File dest) {
                    throw new UnsupportedOperationException();
                }
            };
            
            // Create content based on storage type
            Content content;
            boolean isPrimary = \"Primary\".equals(renditionType.getValue());
            
            if (\"Database\".equals(storageType.getValue())) {
                content = contentService.createContentInDatabase(multipartFile, document);
            } else {
                if (fileStoreCombo.getValue() == null) {
                    Notification.show(\"Please select a file store\", 3000,
                        Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                content = contentService.createContentInFileStore(
                    multipartFile, document, fileStoreCombo.getValue().getId());
            }
            
            // Set rendition type
            content.setPrimary(isPrimary);
            contentService.save(content);
            
            Notification.show(\"Content uploaded successfully as \" + 
                (isPrimary ? \"Primary\" : \"Secondary\") + \" rendition\",
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            uploadDialog.close();
            
            // Refresh the dialog to show new content
            close();
            new DocumentDetailDialog(document, documentService, userService,
                contentService, pluginService, similarityService, 
                fieldExtractionService, fileStoreService).open();
            
        } catch (Exception ex) {
            logger.error(\"Failed to upload content\", ex);
            Notification.show(\"Failed to upload content: \" + ex.getMessage(),
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    
    // Layout
    FormLayout formLayout = new FormLayout();
    formLayout.add(nameField, renditionType, renditionHelp, storageType, 
                   fileStoreCombo, upload, uploadStatus);
    formLayout.setColspan(nameField, 2);
    formLayout.setColspan(renditionHelp, 2);
    formLayout.setColspan(upload, 2);
    formLayout.setColspan(uploadStatus, 2);
    
    HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttonLayout.setWidthFull();
    
    VerticalLayout dialogLayout = new VerticalLayout(
        title, new Hr(), formLayout, buttonLayout);
    dialogLayout.setPadding(true);
    
    uploadDialog.add(dialogLayout);
    uploadDialog.open();
}
```

### 4. Update All DocumentDetailDialog Instantiations

Need to pass FileStoreService wherever DocumentDetailDialog is created:
- In DocumentDetailDialog itself (line 118-119) for version switching
- In TileView.openDocumentDetailDialog (lines 376-384, 391-399)
- In FolderView.openDocumentDetailDialog (lines 1033-1041)
- In SearchView (if it uses DocumentDetailDialog)

### 5. Content Service Methods

Check if ContentService supports setting isPrimary after creation:
- May need to add `content.setPrimary(boolean)` method
- Or create separate methods: `createPrimaryContent()` and `createSecondaryContent()`

## Testing Checklist

- [ ] Upload content with custom name
- [ ] Upload as Primary rendition
- [ ] Upload as Secondary rendition  
- [ ] Upload to Database storage
- [ ] Upload to File Store storage
- [ ] Verify content grid refreshes after upload
- [ ] Verify content selector updates with new content
- [ ] Test from FolderView document dialog
- [ ] Test from TileView document dialog
- [ ] Verify error handling (no file, no name, no file store)

## Future Enhancements

Consider:
- **Parent Rendition Selection**: For secondary renditions, allow selecting which primary content it derives from
- **Batch Upload**: Upload multiple files at once
- **Drag & Drop**: Support drag-and-drop file upload
- **Content Preview**: Show preview before uploading
- **Transformation Options**: Auto-transform PDF to text after upload
