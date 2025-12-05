package com.docmgmt.ui.views;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.FileStore;
import com.docmgmt.model.SysObject;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Documents | Document Management System")
public class DocumentView extends VerticalLayout {

    private final DocumentService documentService;
    private final ContentService contentService;
    private final FileStoreService fileStoreService;
    
    private Grid<Document> grid;
    private TextField filterText;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button createVersionButton;
    private Button uploadContentButton;
    
    private Dialog documentDialog;
    private Binder<Document> binder;
    private Document currentDocument;
    
    private VerticalLayout contentDetailsPanel;
    private Grid<Content> contentGrid;
    private H2 contentPanelTitle;
    private Checkbox showAllVersionsCheckbox;
    
    private ListDataProvider<Document> dataProvider;
    
    @Autowired
    public DocumentView(DocumentService documentService, ContentService contentService, 
                       FileStoreService fileStoreService) {
        this.documentService = documentService;
        this.contentService = contentService;
        this.fileStoreService = fileStoreService;
        
        addClassName("document-view");
        setSizeFull();
        
        configureGrid();
        configureFilter();
        configureDialog();
        configureContentDetailsPanel();
        
        HorizontalLayout toolbar = createToolbar();
        
        // Create split layout with document grid on left and content details on right
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.addToPrimary(grid);
        splitLayout.addToSecondary(contentDetailsPanel);
        splitLayout.setSplitterPosition(60); // 60% for grid, 40% for details
        
        VerticalLayout mainLayout = new VerticalLayout(toolbar, splitLayout);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.expand(splitLayout);
        
        add(mainLayout);
        
        updateList();
    }
    
    private void configureGrid() {
        grid = new Grid<>(Document.class, false);
        grid.addClassNames("document-grid");
        grid.setSizeFull();
        
        // Add columns to the grid
        grid.addColumn(Document::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Document::getDocumentType).setHeader("Type").setSortable(true);
        grid.addColumn(Document::getDescription).setHeader("Description");
        grid.addColumn(Document::getAuthor).setHeader("Author").setSortable(true);
        grid.addColumn(doc -> formatTags(doc.getTags())).setHeader("Tags");
        grid.addColumn(doc -> doc.getMajorVersion() + "." + doc.getMinorVersion())
            .setHeader("Version").setSortable(true);
        grid.addColumn(doc -> {
            if (doc.getParentVersion() != null) {
                SysObject parent = doc.getParentVersion();
                return parent.getMajorVersion() + "." + parent.getMinorVersion();
            }
            return "-";
        }).setHeader("Parent Ver.").setSortable(false);
        grid.addColumn(doc -> doc.getContents() != null ? doc.getContents().size() : 0)
            .setHeader("Content Items").setSortable(true);
        grid.addColumn(doc -> doc.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .setHeader("Last Modified").setSortable(true);
        
        // Configure grid styling and behavior
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            try {
                boolean hasSelection = event.getValue() != null;
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                createVersionButton.setEnabled(hasSelection);
                uploadContentButton.setEnabled(hasSelection);
                
                // Update content details panel
                if (hasSelection) {
                    showContentForDocument(event.getValue());
                } else {
                    clearContentDetails();
                }
            } catch (Exception e) {
                // Log error but don't disrupt the UI
                e.printStackTrace();
                Notification.show("Error in selection handling: " + e.getMessage(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }
    
    private String formatTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(", ", tags);
    }
    
    private void configureFilter() {
        filterText = new TextField();
        filterText.setPlaceholder("Filter by name, description, or author...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> filterGrid());
    }
    
    private void filterGrid() {
        if (dataProvider != null) {
            dataProvider.setFilter(document -> {
                String filter = filterText.getValue().toLowerCase();
                if (filter.isEmpty()) {
                    return true;
                }
                
                boolean nameMatches = document.getName().toLowerCase().contains(filter);
                boolean descMatches = document.getDescription() != null && 
                                     document.getDescription().toLowerCase().contains(filter);
                boolean authorMatches = document.getAuthor() != null && 
                                       document.getAuthor().toLowerCase().contains(filter);
                boolean tagsMatch = document.getTags() != null && 
                                   document.getTags().stream()
                                   .anyMatch(tag -> tag.toLowerCase().contains(filter));
                
                return nameMatches || descMatches || authorMatches || tagsMatch;
            });
        }
    }
    
    private HorizontalLayout createToolbar() {
        // Configure checkbox for showing all versions
        showAllVersionsCheckbox = new Checkbox("Show All Versions");
        showAllVersionsCheckbox.setValue(false);
        showAllVersionsCheckbox.addValueChangeListener(e -> updateList());
        
        // Configure buttons
        addButton = new Button("Add Document", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openDocumentDialog(new Document()));
        
        editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
        editButton.setEnabled(false);
        editButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                openDocumentDialog(document);
            }
        });
        
        deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setEnabled(false);
        deleteButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                confirmDelete(document);
            }
        });
        
        createVersionButton = new Button("Create Version", new Icon(VaadinIcon.FILE_CODE));
        createVersionButton.setEnabled(false);
        createVersionButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                openVersionDialog(document);
            }
        });
        
        uploadContentButton = new Button("Upload Content", new Icon(VaadinIcon.UPLOAD));
        uploadContentButton.setEnabled(false);
        uploadContentButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                openUploadContentDialog(document);
            }
        });
        
        // Create the toolbar layout
        HorizontalLayout toolbar = new HorizontalLayout(
            filterText, showAllVersionsCheckbox, addButton, editButton, deleteButton, createVersionButton, uploadContentButton
        );
        
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.expand(filterText); // Make the filter take up available space
        
        return toolbar;
    }
    
    private void configureDialog() {
        documentDialog = new Dialog();
        documentDialog.setWidth("600px");
        
        // Create the binder for the document form
        binder = new BeanValidationBinder<>(Document.class);
    }
    
    private void openDocumentDialog(Document document) {
        currentDocument = document;
        
        documentDialog.removeAll();
        
        // Create dialog title based on whether we are creating or editing
        H2 title = new H2(document.getId() == null ? "Create New Document" : "Edit Document");
        
        // Create form fields
        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        
        ComboBox<Document.DocumentType> typeCombo = new ComboBox<>("Document Type");
        typeCombo.setItems(Document.DocumentType.values());
        typeCombo.setRequired(true);
        
        TextArea descriptionField = new TextArea("Description");
        descriptionField.setHeight("100px");
        
        TextField authorField = new TextField("Author");
        
        TextArea tagsField = new TextArea("Tags (comma separated)");
        tagsField.setHeight("80px");
        
        TextField keywordsField = new TextField("Keywords");
        
        // Create the form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, typeCombo, descriptionField, authorField, tagsField, keywordsField);
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Set column spans for full-width fields
        formLayout.setColspan(descriptionField, 2);
        formLayout.setColspan(tagsField, 2);
        formLayout.setColspan(keywordsField, 2);
        
        // Create buttons for the dialog
        Button cancelButton = new Button("Cancel", e -> documentDialog.close());
        Button saveButton = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                saveDocument();
                documentDialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setPadding(true);
        
        // Set up the binder
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(Document::getName, Document::setName);
        
        binder.forField(typeCombo)
            .asRequired("Document type is required")
            .bind(Document::getDocumentType, Document::setDocumentType);
        
        binder.forField(descriptionField)
            .bind(Document::getDescription, Document::setDescription);
        
        binder.forField(authorField)
            .bind(Document::getAuthor, Document::setAuthor);
        
        // Handling tags as a string in the UI but as a Set<String> in the model
        binder.forField(tagsField)
            .bind(
                doc -> doc.getTags() != null ? String.join(", ", doc.getTags()) : "",
                (doc, value) -> {
                    if (value == null || value.trim().isEmpty()) {
                        doc.setTags(new HashSet<>());
                    } else {
                        Set<String> tags = Arrays.stream(value.split(","))
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .collect(Collectors.toSet());
                        doc.setTags(tags);
                    }
                }
            );
        
        binder.forField(keywordsField)
            .bind(Document::getKeywords, Document::setKeywords);
        
        // Read the document into the form
        binder.readBean(document);
        
        // Add components to the dialog
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        documentDialog.add(dialogLayout);
        documentDialog.open();
    }
    
    private void saveDocument() {
        if (currentDocument != null && binder.writeBeanIfValid(currentDocument)) {
            try {
                documentService.save(currentDocument);
                updateList();
                Notification.show("Document saved successfully.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to save document: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
    
    private void confirmDelete(Document document) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Document");
        dialog.setText("Are you sure you want to delete '" + document.getName() + "'? This action cannot be undone.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(event -> {
            try {
                documentService.delete(document.getId());
                updateList();
                Notification.show("Document deleted.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to delete document: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        dialog.open();
    }
    
    private void openVersionDialog(Document document) {
        Dialog versionDialog = new Dialog();
        versionDialog.setWidth("400px");
        
        H2 title = new H2("Create New Version");
        
        // Add a label showing the current version
        Span currentVersionLabel = new Span("Current version: " + document.getMajorVersion() + "." + document.getMinorVersion());
        currentVersionLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        ComboBox<String> versionTypeCombo = new ComboBox<>("Version Type");
        versionTypeCombo.setItems("Major Version", "Minor Version");
        versionTypeCombo.setValue("Minor Version");
        versionTypeCombo.setRequired(true);
        
        Button cancelButton = new Button("Cancel", e -> versionDialog.close());
        Button createButton = new Button("Create", e -> {
            if (versionTypeCombo.getValue() == null) {
                Notification.show("Please select a version type", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                if ("Major Version".equals(versionTypeCombo.getValue())) {
                    documentService.createMajorVersion(document.getId());
                } else {
                    documentService.createMinorVersion(document.getId());
                }
                updateList();
                Notification.show("New version created.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                versionDialog.close();
            } catch (Exception ex) {
                Notification.show("Failed to create version: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        // Create dialog layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(currentVersionLabel, versionTypeCombo);
        formLayout.setColspan(currentVersionLabel, 2);
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, createButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        versionDialog.add(dialogLayout);
        versionDialog.open();
    }
    
    private void configureContentDetailsPanel() {
        contentDetailsPanel = new VerticalLayout();
        contentDetailsPanel.setSizeFull();
        contentDetailsPanel.setPadding(true);
        contentDetailsPanel.setSpacing(true);
        
        contentPanelTitle = new H2("Content");
        contentPanelTitle.getStyle().set("margin-top", "0");
        
        // Configure content grid
        contentGrid = new Grid<>(Content.class, false);
        contentGrid.setSizeFull();
        contentGrid.addColumn(Content::getName).setHeader("File Name").setAutoWidth(true);
        contentGrid.addColumn(Content::getContentType).setHeader("Type").setAutoWidth(true);
        contentGrid.addColumn(content -> {
            if (content.isStoredInDatabase()) {
                return "Database";
            } else if (content.isStoredInFileStore()) {
                return content.getFileStore().getName();
            }
            return "Unknown";
        }).setHeader("Storage").setAutoWidth(true);
        contentGrid.addColumn(content -> {
            if (content.getContent() != null) {
                return content.getContent().length + " bytes";
            } else if (content.getStoragePath() != null) {
                try {
                    return contentService.getContentBytes(content.getId()).length + " bytes";
                } catch (IOException e) {
                    return "Error";
                }
            }
            return "0 bytes";
        }).setHeader("Size").setAutoWidth(true);
        
        // Add actions column with view/download buttons
        contentGrid.addComponentColumn(content -> {
            Button viewButton = new Button(new Icon(VaadinIcon.EYE));
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewButton.addClickListener(e -> viewContent(content));
            viewButton.getElement().setAttribute("title", "View content");
            
            Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
            downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downloadButton.addClickListener(e -> downloadContent(content));
            downloadButton.getElement().setAttribute("title", "Download content");
            
            HorizontalLayout actions = new HorizontalLayout(viewButton, downloadButton);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        contentGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        
        Span emptyState = new Span("Select a document to view its content");
        emptyState.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-style", "italic")
            .set("text-align", "center")
            .set("padding", "2em");
        
        contentDetailsPanel.add(contentPanelTitle, contentGrid);
        contentDetailsPanel.expand(contentGrid);
    }
    
    private void showContentForDocument(Document document) {
        if (document == null) {
            clearContentDetails();
            return;
        }
        
        contentPanelTitle.setText("Content for: " + document.getName());
        
        // Fetch content for the document
        List<Content> contents = contentService.findBySysObject(document);
        contentGrid.setItems(contents);
        contentGrid.setVisible(!contents.isEmpty());
        
        if (contents.isEmpty()) {
            // Show empty state
            Span emptyMsg = new Span("No content attached to this document");
            emptyMsg.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic");
            // Note: Could add empty state component here if needed
        }
    }
    
    private void clearContentDetails() {
        contentPanelTitle.setText("Content");
        contentGrid.setItems();
    }
    
    private void viewContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            String contentType = content.getContentType();
            
            Dialog viewDialog = new Dialog();
            viewDialog.setWidth("80%");
            viewDialog.setHeight("80%");
            
            H2 title = new H2("View: " + content.getName());
            
            VerticalLayout contentLayout = new VerticalLayout();
            contentLayout.setSizeFull();
            contentLayout.setPadding(false);
            
            // Display content based on type
            if (contentType != null && contentType.startsWith("text/")) {
                TextArea textArea = new TextArea();
                textArea.setValue(new String(bytes));
                textArea.setReadOnly(true);
                textArea.setSizeFull();
                contentLayout.add(textArea);
            } else if (contentType != null && contentType.startsWith("image/")) {
                // For images, create a data URL
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + contentType + ";base64," + base64;
                
                com.vaadin.flow.component.html.Image image = 
                    new com.vaadin.flow.component.html.Image(dataUrl, content.getName());
                image.setMaxWidth("100%");
                image.getStyle().set("display", "block").set("margin", "auto");
                contentLayout.add(image);
                contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            } else {
                Span message = new Span("Preview not available for this file type. Size: " + bytes.length + " bytes");
                message.getStyle().set("padding", "2em");
                
                Button downloadBtn = new Button("Download File", new Icon(VaadinIcon.DOWNLOAD));
                downloadBtn.addClickListener(e -> downloadContent(content));
                
                contentLayout.add(message, downloadBtn);
                contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            }
            
            Button closeButton = new Button("Close", e -> viewDialog.close());
            closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            HorizontalLayout buttonLayout = new HorizontalLayout(closeButton);
            buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            buttonLayout.setWidthFull();
            buttonLayout.setPadding(true);
            
            VerticalLayout dialogLayout = new VerticalLayout(title, new Hr(), contentLayout, buttonLayout);
            dialogLayout.setSizeFull();
            dialogLayout.setPadding(false);
            dialogLayout.setSpacing(false);
            dialogLayout.expand(contentLayout);
            
            viewDialog.add(dialogLayout);
            viewDialog.open();
            
        } catch (Exception e) {
            Notification.show("Failed to view content: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            
            // Create a stream resource for download
            com.vaadin.flow.server.StreamResource resource = 
                new com.vaadin.flow.server.StreamResource(content.getName(), 
                    () -> new ByteArrayInputStream(bytes));
            
            // Set content type if available
            if (content.getContentType() != null) {
                resource.setContentType(content.getContentType());
            }
            
            // Trigger download using anchor
            com.vaadin.flow.component.html.Anchor download = 
                new com.vaadin.flow.component.html.Anchor(resource, "");
            download.getElement().setAttribute("download", true);
            download.getElement().getStyle().set("display", "none");
            
            getUI().ifPresent(ui -> {
                ui.getElement().appendChild(download.getElement());
                ui.getPage().executeJs("$0.click()", download.getElement());
                ui.getElement().removeChild(download.getElement());
            });
            
            Notification.show("Downloading " + content.getName(), 
                2000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception e) {
            Notification.show("Failed to download content: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void openUploadContentDialog(Document document) {
        Dialog uploadDialog = new Dialog();
        uploadDialog.setWidth("500px");
        
        H2 title = new H2("Upload Content");
        
        Span docInfo = new Span("Document: " + document.getName() + " (v" + 
                               document.getMajorVersion() + "." + document.getMinorVersion() + ")");
        docInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        // Storage type selection
        RadioButtonGroup<String> storageType = new RadioButtonGroup<>();
        storageType.setLabel("Storage Type");
        storageType.setItems("Database", "File Store");
        storageType.setValue("Database");
        
        // File store selection (initially hidden)
        ComboBox<FileStore> fileStoreCombo = new ComboBox<>("File Store");
        fileStoreCombo.setItems(fileStoreService.findAll());
        fileStoreCombo.setItemLabelGenerator(FileStore::getName);
        fileStoreCombo.setVisible(false);
        
        storageType.addValueChangeListener(e -> {
            boolean isFileStore = "File Store".equals(e.getValue());
            fileStoreCombo.setVisible(isFileStore);
            if (isFileStore && fileStoreCombo.getValue() == null && !fileStoreCombo.getListDataView().getItems().findFirst().isEmpty()) {
                fileStoreCombo.setValue(fileStoreCombo.getListDataView().getItems().findFirst().get());
            }
        });
        
        // File upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes(
            "application/pdf", ".pdf",
            "text/plain", ".txt",
            "application/msword", ".doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "image/*"
        );
        
        Span uploadStatus = new Span();
        uploadStatus.setVisible(false);
        
        upload.addSucceededListener(event -> {
            uploadStatus.setText("File uploaded: " + event.getFileName());
            uploadStatus.getStyle().set("color", "var(--lumo-success-color)");
            uploadStatus.setVisible(true);
        });
        
        upload.addFileRejectedListener(event -> {
            uploadStatus.setText("File rejected: " + event.getErrorMessage());
            uploadStatus.getStyle().set("color", "var(--lumo-error-color)");
            uploadStatus.setVisible(true);
        });
        
        // Buttons
        Button cancelButton = new Button("Cancel", e -> uploadDialog.close());
        Button saveButton = new Button("Upload", e -> {
            try {
                if (buffer.getInputStream().available() == 0) {
                    Notification.show("Please select a file to upload", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Create a custom MultipartFile from the buffer
                byte[] fileBytes = buffer.getInputStream().readAllBytes();
                String fileName = buffer.getFileName();
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
                    public InputStream getInputStream() { 
                        return new ByteArrayInputStream(fileBytes); 
                    }
                    
                    @Override
                    public void transferTo(File dest) throws IllegalStateException {
                        throw new UnsupportedOperationException();
                    }
                };
                
                Content content;
                if ("Database".equals(storageType.getValue())) {
                    content = contentService.createContentInDatabase(multipartFile, document);
                } else {
                    if (fileStoreCombo.getValue() == null) {
                        Notification.show("Please select a file store", 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    content = contentService.createContentInFileStore(
                        multipartFile, document, fileStoreCombo.getValue().getId()
                    );
                }
                
                Notification.show("Content uploaded successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                uploadDialog.close();
                
                // Refresh content panel to show new content
                showContentForDocument(document);
                
            } catch (Exception ex) {
                Notification.show("Failed to upload content: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        // Layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(docInfo, storageType, fileStoreCombo, upload, uploadStatus);
        formLayout.setColspan(docInfo, 2);
        formLayout.setColspan(upload, 2);
        formLayout.setColspan(uploadStatus, 2);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        uploadDialog.add(dialogLayout);
        uploadDialog.open();
    }
    
    /**
     * Updates the document list in the grid by fetching fresh data from the service.
     * This method is called after CRUD operations to refresh the UI.
     */
    private void updateList() {
        try {
            // Get documents based on checkbox state
            List<Document> documents;
            if (showAllVersionsCheckbox.getValue()) {
                documents = documentService.findAll();
            } else {
                documents = documentService.findAllLatestVersions();
            }
            
            // Update the data provider
            dataProvider = DataProvider.ofCollection(documents);
            grid.setDataProvider(dataProvider);
            
            // Apply current filter if any
            filterGrid();
            
            // Reset button states since selection is cleared
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            createVersionButton.setEnabled(false);
            uploadContentButton.setEnabled(false);
        } catch (Exception e) {
            Notification.show("Failed to load documents: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Log the error for server-side tracking
            e.printStackTrace();
        }
    }
    
    /**
     * Handles error reporting in a consistent way across the application.
     * 
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    private void showError(String operation, Exception error) {
        // Log the error
        error.printStackTrace();
        
        // Show user-friendly notification
        Notification.show(
            operation + " failed: " + error.getMessage(),
            3000, 
            Notification.Position.BOTTOM_START
        ).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
