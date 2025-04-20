package com.docmgmt.ui.views;

import com.docmgmt.model.Content;
import com.docmgmt.model.FileStore;
import com.docmgmt.model.SysObject;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Route(value = "content", layout = MainLayout.class)
@PageTitle("Content | Document Management System")
public class ContentView extends VerticalLayout {

    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private final ContentService contentService;
    private final FileStoreService fileStoreService;
    
    private Grid<Content> grid;
    private Button deleteButton;
    private Button downloadButton;
    private Button editButton;
    
    private Dialog contentDialog;
    private Binder<Content> binder;
    private Content currentContent;
    
    private ListDataProvider<Content> dataProvider;
    
    @Autowired
    public ContentView(ContentService contentService, FileStoreService fileStoreService) {
        this.contentService = contentService;
        this.fileStoreService = fileStoreService;
        
        addClassName("content-view");
        setSizeFull();
        
        configureGrid();
        
        VerticalLayout toolbar = createToolbar();
        
        add(toolbar, grid);
        
        updateList();
    }
    
    private void configureGrid() {
        grid = new Grid<>(Content.class, false);
        grid.addClassNames("content-grid");
        grid.setSizeFull();
        
        // Add columns to the grid
        grid.addColumn(Content::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Content::getContentType).setHeader("Content Type").setSortable(true);
        
        // File store column 
        grid.addColumn(content -> content.getFileStore() != null 
            ? content.getFileStore().getName() : "Database")
            .setHeader("Storage Location").setSortable(true);
        
        // Size column with formatter
        grid.addColumn(content -> {
            try {
                return formatFileSize(content.getSize());
            } catch (IOException e) {
                return "Error";
            }
        }).setHeader("Size").setSortable(true);
        
        // Creation/modification dates
        grid.addColumn(content -> content.getCreatedAt().format(DATE_FORMATTER))
            .setHeader("Created").setSortable(true);
        grid.addColumn(content -> content.getModifiedAt().format(DATE_FORMATTER))
            .setHeader("Modified").setSortable(true);
        
        // Document column
        grid.addColumn(content -> content.getSysObject() != null 
            ? content.getSysObject().getName() : "None")
            .setHeader("Document").setSortable(true);
        
        // Configure grid styling and behavior
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            boolean hasSelection = event.getValue() != null;
            deleteButton.setEnabled(hasSelection);
            downloadButton.setEnabled(hasSelection);
            editButton.setEnabled(hasSelection);
        });
    }
    
    private VerticalLayout createToolbar() {
        // Create upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        
        upload.setMaxFileSize(MAX_FILE_SIZE);
        upload.setAcceptedFileTypes("image/*", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt");
        
        // Create filestore selection for uploads
        ComboBox<FileStore> fileStoreSelect = new ComboBox<>();
        fileStoreSelect.setLabel("Storage Location");
        fileStoreSelect.setPlaceholder("Select where to store content");
        fileStoreSelect.setItemLabelGenerator(FileStore::getName);
        
        try {

            List<FileStore> activeFileStores = fileStoreService.findAllActive();
            fileStoreSelect.setItems(activeFileStores);
            
            // Add a "Database Storage" option
            Span databaseOption = new Span("Database Storage (default)");
            fileStoreSelect.setRenderer(new ComponentRenderer<>(fileStore -> 
                fileStore == null ? databaseOption : new Span(fileStore.getName())));
        } catch (Exception e) {
            Notification.show("Failed to load file stores: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        
        // Configure file upload component
        upload.addSucceededListener(event -> {
            try {
                // Create a new content object
                Content content = new Content();
                content.setName(event.getFileName());
                content.setContentType(event.getMIMEType());
                
                // Read file content
                InputStream inputStream = buffer.getInputStream();
                byte[] fileData = IOUtils.toByteArray(inputStream);
                
                // Set the file store if selected
                FileStore selectedFileStore = fileStoreSelect.getValue();
                if (selectedFileStore != null) {
                    content.setFileStore(selectedFileStore);
                    content.setStoragePath(generateStoragePath(event.getFileName()));
                }
                
                // Save the content
                content.setContentBytes(fileData);
                contentService.save(content);
                
                updateList();
                Notification.show("Upload successful: " + event.getFileName(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Upload failed: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        upload.addFailedListener(event -> {
            Notification.show("Upload failed: " + event.getReason(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        
        // Configure action buttons
        downloadButton = new Button("Download", new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.setEnabled(false);
        downloadButton.addClickListener(e -> {
            Content content = grid.asSingleSelect().getValue();
            if (content != null) {
                downloadContent(content);
            }
        });
        
        editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
        editButton.setEnabled(false);
        editButton.addClickListener(e -> {
            Content content = grid.asSingleSelect().getValue();
            if (content != null) {
                openContentDialog(content);
            }
        });
        
        deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setEnabled(false);
        deleteButton.addClickListener(e -> {
            Content content = grid.asSingleSelect().getValue();
            if (content != null) {
                confirmDelete(content);
            }
        });
        
        // Create upload section
        H2 uploadTitle = new H2("Upload Content");
        uploadTitle.getStyle().set("margin-top", "0");
        
        HorizontalLayout uploadControls = new HorizontalLayout(fileStoreSelect, upload);
        uploadControls.setWidthFull();
        uploadControls.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        
        VerticalLayout uploadSection = new VerticalLayout(uploadTitle, uploadControls);
        uploadSection.setPadding(true);
        uploadSection.setSpacing(false);
        
        // Create actions section
        HorizontalLayout actions = new HorizontalLayout(downloadButton, editButton, deleteButton);
        actions.setWidthFull();
        actions.setPadding(true);
        
        // Combine all in a toolbar
        VerticalLayout toolbar = new VerticalLayout(uploadSection, new Hr(), actions);
        toolbar.setPadding(false);
        
        return toolbar;
    }
    
    private void openContentDialog(Content content) {
        currentContent = content;
        
        contentDialog = new Dialog();
        contentDialog.setWidth("500px");
        
        H2 title = new H2("Edit Content");
        
        // Create form fields
        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        
        TextField contentTypeField = new TextField("Content Type");
        contentTypeField.setWidthFull();
        
        ComboBox<FileStore> fileStoreSelect = new ComboBox<>("Storage Location");
        fileStoreSelect.setItemLabelGenerator(FileStore::getName);
        fileStoreSelect.setWidthFull();
        
        try {
            List<FileStore> activeFileStores = fileStoreService.findAllActive();
            fileStoreSelect.setItems(activeFileStores);
            
            // Add null option for database storage
            Span databaseOption = new Span("Database Storage");
            fileStoreSelect.setRenderer(new ComponentRenderer<>(fileStore -> 
                fileStore == null ? databaseOption : new Span(fileStore.getName())));
        } catch (Exception e) {
            Notification.show("Failed to load file stores: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        
        // Create the form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, contentTypeField, fileStoreSelect);
        
        // Create buttons for the dialog
        Button cancelButton = new Button("Cancel", e -> contentDialog.close());
        Button saveButton = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                saveContent();
                contentDialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setPadding(true);
        buttonLayout.setWidthFull();
        
        // Set up the binder
        binder = new BeanValidationBinder<>(Content.class);
        
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(Content::getName, Content::setName);
        
        binder.forField(contentTypeField)
            .bind(Content::getContentType, Content::setContentType);

        binder.forField(fileStoreSelect)
            .bind(Content::getFileStore, (content2, fileStore) -> {
                // If changing from database to file store or between file stores,
                // we need to handle the content data transfer
                boolean wasInDatabase = content2.isStoredInDatabase();
                FileStore oldFileStore = content2.getFileStore();

                content2.setFileStore(fileStore);
                
                if (fileStore != null && (wasInDatabase || !fileStore.equals(oldFileStore))) {
                    // We need a storage path if using a file store
                    if (content2.getStoragePath() == null || content2.getStoragePath().isEmpty()) {
                        content2.setStoragePath(generateStoragePath(content2.getName()));
                    }
                }
            });
        
        // Read the content into the form
        binder.readBean(content);
        
        // Add components to the dialog
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        contentDialog.add(dialogLayout);
        contentDialog.open();
    }
    
    private void saveContent() {
        if (currentContent != null && binder.writeBeanIfValid(currentContent)) {
            try {
                contentService.save(currentContent);
                updateList();
                Notification.show("Content saved successfully.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to save content: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                e.printStackTrace();
            }
        }
    }
    
    private void confirmDelete(Content content) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Content");
        dialog.setText("Are you sure you want to delete '" + content.getName() + "'? This action cannot be undone.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(event -> {
            try {
                contentService.delete(content.getId());
                updateList();
                Notification.show("Content deleted.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to delete content: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                e.printStackTrace();
            }
        });
        
        dialog.open();
    }
    
    private void downloadContent(Content content) {
        try {
            // Create a stream resource from the content bytes
            StreamResource streamResource = new StreamResource(
                content.getName(),
                () -> {
                    try {
                        byte[] bytes = content.getContentBytes();
                        return new ByteArrayInputStream(bytes);
                    } catch (IOException e) {
                        Notification.show("Failed to download content: " + e.getMessage(), 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        e.printStackTrace();
                        return new ByteArrayInputStream(new byte[0]);
                    }
                }
            );
            
            // Set the content type if available
            if (content.getContentType() != null && !content.getContentType().isEmpty()) {
                streamResource.setContentType(content.getContentType());
            }
            
            // Create an anchor (invisible link) that will handle the download
            Anchor downloadLink = new Anchor(streamResource, "");
            downloadLink.getElement().setAttribute("download", true);
            
            // Add the link to the page, click it, and then remove it
            add(downloadLink);
            downloadLink.getElement().executeJs("this.click()");
            
            // Use a separate thread to remove the download link after a delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    getUI().ifPresent(ui -> ui.access(() -> remove(downloadLink)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (Exception e) {
            Notification.show("Failed to download content: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }
    
    /**
     * Formats a file size in bytes to a human-readable string (KB, MB, etc.)
     * @param bytes The size in bytes
     * @return A formatted string representation of the size
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(bytes / Math.pow(1024, exp)) + " " + pre + "B";
    }
    
    /**
     * Generates a unique storage path for a file in a file store
     * @param fileName The original file name
     * @return A unique path string
     */
    private String generateStoragePath(String fileName) {
        String uuid = UUID.randomUUID().toString();
        String extension = "";
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = fileName.substring(lastDotIndex);
        }
        
        // Create a path with year/month structure for better organization
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        
        return year + "/" + month + "/" + uuid + extension;
    }
    
    /**
     * Updates the content list in the grid by fetching fresh data from the service.
     * This method is called after CRUD operations to refresh the UI.
     */
    private void updateList() {
        try {
            // Get all content
            List<Content> contents = contentService.findAll();
            
            // Update the data provider
            dataProvider = DataProvider.ofCollection(contents);
            grid.setDataProvider(dataProvider);
            
            // Reset button states since selection is cleared
            deleteButton.setEnabled(false);
            downloadButton.setEnabled(false);
            editButton.setEnabled(false);
        } catch (Exception e) {
            Notification.show("Failed to load content list: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Log the error for server-side tracking
            e.printStackTrace();
        }
    }
}
