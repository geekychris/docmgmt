package com.docmgmt.ui.views;

import com.docmgmt.model.Document;
import com.docmgmt.service.DocumentService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
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
    
    private Grid<Document> grid;
    private TextField filterText;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button createVersionButton;
    
    private Dialog documentDialog;
    private Binder<Document> binder;
    private Document currentDocument;
    
    private ListDataProvider<Document> dataProvider;
    
    @Autowired
    public DocumentView(DocumentService documentService) {
        this.documentService = documentService;
        
        addClassName("document-view");
        setSizeFull();
        
        configureGrid();
        configureFilter();
        configureDialog();
        
        HorizontalLayout toolbar = createToolbar();
        
        add(toolbar, grid);
        
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
        
        // Create the toolbar layout
        HorizontalLayout toolbar = new HorizontalLayout(
            filterText, addButton, editButton, deleteButton, createVersionButton
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
    
    /**
     * Updates the document list in the grid by fetching fresh data from the service.
     * This method is called after CRUD operations to refresh the UI.
     */
    private void updateList() {
        try {
            // Get the latest versions of all documents
            List<Document> documents = documentService.findAllLatestVersions();
            
            // Update the data provider
            dataProvider = DataProvider.ofCollection(documents);
            grid.setDataProvider(dataProvider);
            
            // Apply current filter if any
            filterGrid();
            
            // Reset button states since selection is cleared
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            createVersionButton.setEnabled(false);
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
