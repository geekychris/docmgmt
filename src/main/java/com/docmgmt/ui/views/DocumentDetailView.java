package com.docmgmt.ui.views;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.User;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.UserService;
import com.docmgmt.ui.MainLayout;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "document", layout = MainLayout.class)
@PageTitle("Document Details | Document Management System")
public class DocumentDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final DocumentService documentService;
    private final ContentService contentService;
    private final UserService userService;
    
    private Document currentDocument;
    private VerticalLayout documentInfoPanel;
    private VerticalLayout contentPanel;
    private Grid<Content> contentGrid;
    private boolean editMode = false;
    private Button editToggleButton;
    private Checkbox editModeCheckbox;
    private static final int CONTENT_PAGE_SIZE = 10;
    
    @Autowired
    public DocumentDetailView(DocumentService documentService, ContentService contentService, UserService userService) {
        this.documentService = documentService;
        this.contentService = contentService;
        this.userService = userService;
        
        addClassName("document-detail-view");
        setSizeFull();
        setPadding(true);
    }
    
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long documentId) {
        if (documentId != null) {
            // Check for edit query parameter
            Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
            if (params.containsKey("edit") && "true".equalsIgnoreCase(params.get("edit").get(0))) {
                editMode = true;
            }
            loadDocument(documentId);
        } else {
            showError("No document ID provided");
            createBackButton();
        }
    }
    
    private void loadDocument(Long documentId) {
        try {
            currentDocument = documentService.findById(documentId);
            if (currentDocument != null) {
                buildView();
            } else {
                showError("Document not found");
                createBackButton();
            }
        } catch (Exception e) {
            showError("Failed to load document: " + e.getMessage());
            createBackButton();
        }
    }
    
    private void buildView() {
        removeAll();
        
        // Create back button and header
        Button backButton = new Button("Back to Search", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> UI.getCurrent().navigate("search"));
        
        Button backToDocumentsButton = new Button("Back to Documents", new Icon(VaadinIcon.ARROW_LEFT));
        backToDocumentsButton.addClickListener(e -> UI.getCurrent().navigate(""));
        
        // Version picker - finds all versions in hierarchy regardless of name changes
        ComboBox<Document> versionPicker = new ComboBox<>("Version");
        List<Document> allVersions = documentService.findAllVersionsInHierarchy(currentDocument.getId());
        versionPicker.setItems(allVersions);
        versionPicker.setItemLabelGenerator(doc -> 
            "v" + doc.getMajorVersion() + "." + doc.getMinorVersion() + 
            " - " + doc.getName() +
            (doc.getId().equals(currentDocument.getId()) ? " (current)" : ""));
        versionPicker.setValue(currentDocument);
        versionPicker.setWidth("400px");
        versionPicker.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().getId().equals(currentDocument.getId())) {
                // Navigate to the selected version
                UI.getCurrent().navigate("document/" + e.getValue().getId());
            }
        });
        
        // Edit mode toggle
        editModeCheckbox = new Checkbox("Edit Mode");
        editModeCheckbox.setValue(editMode);
        editModeCheckbox.addValueChangeListener(e -> {
            editMode = e.getValue();
            refreshDocumentInfo();
        });
        
        HorizontalLayout headerButtons = new HorizontalLayout(backButton, backToDocumentsButton, versionPicker, editModeCheckbox);
        headerButtons.setSpacing(true);
        headerButtons.setAlignItems(FlexComponent.Alignment.CENTER);
        
        H2 title = new H2("Document Details");
        
        HorizontalLayout header = new HorizontalLayout(headerButtons);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        
        // Create split layout
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        
        // Left side: Document information
        documentInfoPanel = createDocumentInfoPanel();
        splitLayout.addToPrimary(documentInfoPanel);
        
        // Right side: Content grid
        contentPanel = createContentPanel();
        splitLayout.addToSecondary(contentPanel);
        
        splitLayout.setSplitterPosition(50); // 50/50 split
        
        add(header, title, new Hr(), splitLayout);
        expand(splitLayout);
    }
    
    private VerticalLayout createDocumentInfoPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(true);
        panel.setSpacing(true);
        
        H3 infoTitle = new H3("Document Information");
        
        if (editMode) {
            // Editable form
            FormLayout formLayout = createEditableForm();
            
            // Save button
            Button saveButton = new Button("Save Changes", new Icon(VaadinIcon.CHECK));
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            saveButton.addClickListener(e -> saveDocument());
            
            Button cancelButton = new Button("Cancel", new Icon(VaadinIcon.CLOSE));
            cancelButton.addClickListener(e -> {
                editMode = false;
                editModeCheckbox.setValue(false);
                refreshDocumentInfo();
            });
            
            HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
            buttonLayout.setSpacing(true);
            
            panel.add(infoTitle, formLayout, buttonLayout);
        } else {
            // Read-only display using DocumentFieldRenderer
            VerticalLayout infoLayout = new VerticalLayout();
            infoLayout.setSpacing(true);
            infoLayout.setPadding(false);
            
            // Use DocumentFieldRenderer to show all fields (base + type-specific)
            DocumentFieldRenderer.renderReadOnlyFields(currentDocument, infoLayout);
            
            // Add version and timestamps
            addInfoRow(infoLayout, "Version:", 
                currentDocument.getMajorVersion() + "." + currentDocument.getMinorVersion());
            
            if (currentDocument.getParentVersion() != null) {
                addInfoRow(infoLayout, "Parent Version:", 
                    currentDocument.getParentVersion().getMajorVersion() + "." + 
                    currentDocument.getParentVersion().getMinorVersion());
            }
            
            addInfoRow(infoLayout, "Created:", 
                currentDocument.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            addInfoRow(infoLayout, "Modified:", 
                currentDocument.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            panel.add(infoTitle, infoLayout);
        }
        
        return panel;
    }
    
    private FormLayout createEditableForm() {
        FormLayout formLayout = new FormLayout();
        
        TextField nameField = new TextField("Name");
        nameField.setValue(currentDocument.getName() != null ? currentDocument.getName() : "");
        nameField.setWidthFull();
        
        TextArea descriptionField = new TextArea("Description");
        descriptionField.setValue(currentDocument.getDescription() != null ? currentDocument.getDescription() : "");
        descriptionField.setWidthFull();
        descriptionField.setHeight("100px");
        
        TextField keywordsField = new TextField("Keywords");
        keywordsField.setValue(currentDocument.getKeywords() != null ? currentDocument.getKeywords() : "");
        keywordsField.setWidthFull();
        
        TextArea tagsField = new TextArea("Tags (comma separated)");
        if (currentDocument.getTags() != null && !currentDocument.getTags().isEmpty()) {
            tagsField.setValue(String.join(", ", currentDocument.getTags()));
        }
        tagsField.setWidthFull();
        tagsField.setHeight("80px");
        
        ComboBox<User> ownerCombo = new ComboBox<>("Owner");
        ownerCombo.setItems(userService.findAll());
        ownerCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        ownerCombo.setValue(currentDocument.getOwner());
        ownerCombo.setWidthFull();
        
        MultiSelectComboBox<User> authorsCombo = new MultiSelectComboBox<>("Authors");
        authorsCombo.setItems(userService.findAll());
        authorsCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        if (currentDocument.getAuthors() != null) {
            authorsCombo.setValue(currentDocument.getAuthors());
        }
        authorsCombo.setWidthFull();
        
        // Store field references for saving
        nameField.addValueChangeListener(e -> currentDocument.setName(e.getValue()));
        descriptionField.addValueChangeListener(e -> currentDocument.setDescription(e.getValue()));
        keywordsField.addValueChangeListener(e -> currentDocument.setKeywords(e.getValue()));
        tagsField.addValueChangeListener(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                currentDocument.setTags(new HashSet<>());
            } else {
                Set<String> tags = Arrays.stream(e.getValue().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
                currentDocument.setTags(tags);
            }
        });
        ownerCombo.addValueChangeListener(e -> currentDocument.setOwner(e.getValue()));
        authorsCombo.addValueChangeListener(e -> {
            currentDocument.getAuthors().clear();
            if (e.getValue() != null) {
                currentDocument.getAuthors().addAll(e.getValue());
            }
        });
        
        formLayout.add(nameField, descriptionField, keywordsField, tagsField, ownerCombo, authorsCombo);
        
        // Add type-specific editable fields using DocumentFieldRenderer
        DocumentFieldRenderer.renderEditableFields(currentDocument, formLayout, ownerCombo, authorsCombo, userService.findAll());
        
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1)
        );
        
        return formLayout;
    }
    
    private void saveDocument() {
        try {
            documentService.save(currentDocument);
            Notification.show("Document saved successfully", 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            editMode = false;
            editModeCheckbox.setValue(false);
            refreshDocumentInfo();
        } catch (Exception e) {
            Notification.show("Failed to save document: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void refreshDocumentInfo() {
        // Reload document from database
        currentDocument = documentService.findById(currentDocument.getId());
        documentInfoPanel.removeAll();
        
        H3 infoTitle = new H3("Document Information");
        
        if (editMode) {
            FormLayout formLayout = createEditableForm();
            
            Button saveButton = new Button("Save Changes", new Icon(VaadinIcon.CHECK));
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            saveButton.addClickListener(e -> saveDocument());
            
            Button cancelButton = new Button("Cancel", new Icon(VaadinIcon.CLOSE));
            cancelButton.addClickListener(e -> {
                editMode = false;
                editModeCheckbox.setValue(false);
                refreshDocumentInfo();
            });
            
            HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
            buttonLayout.setSpacing(true);
            
            documentInfoPanel.add(infoTitle, formLayout, buttonLayout);
        } else {
            VerticalLayout infoLayout = new VerticalLayout();
            infoLayout.setSpacing(true);
            infoLayout.setPadding(false);
            
            // Use DocumentFieldRenderer to show all fields (base + type-specific)
            DocumentFieldRenderer.renderReadOnlyFields(currentDocument, infoLayout);
            
            // Add version and timestamps
            addInfoRow(infoLayout, "Version:", 
                currentDocument.getMajorVersion() + "." + currentDocument.getMinorVersion());
            
            if (currentDocument.getParentVersion() != null) {
                addInfoRow(infoLayout, "Parent Version:", 
                    currentDocument.getParentVersion().getMajorVersion() + "." + 
                    currentDocument.getParentVersion().getMinorVersion());
            }
            
            addInfoRow(infoLayout, "Created:", 
                currentDocument.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            addInfoRow(infoLayout, "Modified:", 
                currentDocument.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            documentInfoPanel.add(infoTitle, infoLayout);
        }
    }
    
    private void addInfoRow(VerticalLayout layout, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return; // Skip empty values
        }
        
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.START);
        
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px")
            .set("color", "var(--lumo-secondary-text-color)");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("flex", "1");
        
        row.add(labelSpan, valueSpan);
        layout.add(row);
    }
    
    private VerticalLayout createContentPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(true);
        panel.setSpacing(true);
        
        H3 contentTitle = new H3("Associated Content");
        
        // Configure content grid
        contentGrid = new Grid<>(Content.class, false);
        contentGrid.setSizeFull();
        
        // Enable pagination for content
        contentGrid.setPageSize(CONTENT_PAGE_SIZE);
        
        contentGrid.addColumn(Content::getName).setHeader("File Name").setAutoWidth(true).setFlexGrow(1);
        contentGrid.addColumn(Content::getContentType).setHeader("Type").setAutoWidth(true);
        
        // Add rendition type column with badge
        contentGrid.addComponentColumn(content -> {
            Span badge = new Span(content.isPrimary() ? "Primary" : "Secondary");
            if (content.isPrimary()) {
                badge.getElement().getThemeList().add("badge success");
            } else {
                badge.getElement().getThemeList().add("badge");
            }
            return badge;
        }).setHeader("Rendition").setAutoWidth(true);
        
        // Add indexable column with icon
        contentGrid.addComponentColumn(content -> {
            if (content.isIndexable()) {
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
                icon.setColor("var(--lumo-success-color)");
                icon.getElement().setAttribute("title", "Indexable");
                return icon;
            } else {
                Icon icon = new Icon(VaadinIcon.MINUS_CIRCLE);
                icon.setColor("var(--lumo-disabled-text-color)");
                icon.getElement().setAttribute("title", "Not indexable");
                return icon;
            }
        }).setHeader("Indexable").setAutoWidth(true);
        
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
                return formatBytes(content.getContent().length);
            } else if (content.getStoragePath() != null) {
                try {
                    return formatBytes(contentService.getContentBytes(content.getId()).length);
                } catch (IOException e) {
                    return "Error";
                }
            }
            return "0 bytes";
        }).setHeader("Size").setAutoWidth(true);
        
        // Add actions column
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
        
        // Load content for this document
        List<Content> contents = contentService.findBySysObject(currentDocument);
        contentGrid.setItems(contents);
        
        if (contents.isEmpty()) {
            Span emptyMsg = new Span("No content attached to this document");
            emptyMsg.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic")
                .set("text-align", "center")
                .set("padding", "2em");
            panel.add(contentTitle, emptyMsg);
        } else {
            panel.add(contentTitle, contentGrid);
            panel.expand(contentGrid);
        }
        
        return panel;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private void viewContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            String contentType = content.getContentType();
            
            com.vaadin.flow.component.dialog.Dialog viewDialog = new com.vaadin.flow.component.dialog.Dialog();
            viewDialog.setWidth("80%");
            viewDialog.setHeight("80%");
            
            H2 title = new H2("View: " + content.getName());
            
            VerticalLayout contentLayout = new VerticalLayout();
            contentLayout.setSizeFull();
            contentLayout.setPadding(false);
            
            // Display content based on type
            if (contentType != null && contentType.startsWith("text/")) {
                // Display text content with monospace font
                Pre pre = new Pre();
                pre.setText(new String(bytes));
                pre.getStyle()
                    .set("white-space", "pre-wrap")
                    .set("font-family", "monospace")
                    .set("font-size", "12px")
                    .set("padding", "1em")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("overflow", "auto")
                    .set("max-height", "100%");
                contentLayout.add(pre);
                
            } else if (contentType != null && contentType.equals("application/pdf")) {
                // Display PDF using iframe with base64 data URI
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUri = "data:application/pdf;base64," + base64;
                
                com.vaadin.flow.component.html.IFrame iframe = new com.vaadin.flow.component.html.IFrame(dataUri);
                iframe.setSizeFull();
                iframe.getStyle().set("border", "none");
                contentLayout.add(iframe);
                
            } else if (contentType != null && contentType.startsWith("image/")) {
                // Display image
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + contentType + ";base64," + base64;
                
                Image image = new Image(dataUrl, content.getName());
                image.setMaxWidth("100%");
                image.getStyle().set("display", "block").set("margin", "auto");
                contentLayout.add(image);
                contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                
            } else {
                // Unsupported type - show download option
                Span message = new Span("Preview not available for this file type: " + contentType);
                message.getStyle().set("padding", "2em");
                
                Span sizeInfo = new Span("Size: " + formatBytes(bytes.length));
                sizeInfo.getStyle().set("margin-top", "10px");
                
                Button downloadBtn = new Button("Download File", new Icon(VaadinIcon.DOWNLOAD));
                downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                downloadBtn.addClickListener(e -> downloadContent(content));
                
                contentLayout.add(message, sizeInfo, downloadBtn);
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
            
            com.vaadin.flow.server.StreamResource resource = 
                new com.vaadin.flow.server.StreamResource(content.getName(), 
                    () -> new ByteArrayInputStream(bytes));
            
            if (content.getContentType() != null) {
                resource.setContentType(content.getContentType());
            }
            
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
    
    private void createBackButton() {
        Button backButton = new Button("Back to Search", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> UI.getCurrent().navigate("search"));
        add(backButton);
    }
    
    private void showError(String message) {
        H2 errorTitle = new H2("Error");
        Span errorMessage = new Span(message);
        errorMessage.getStyle().set("color", "var(--lumo-error-color)");
        add(errorTitle, errorMessage);
    }
}
