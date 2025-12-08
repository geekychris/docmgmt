package com.docmgmt.ui.views;

import com.docmgmt.model.*;
import com.docmgmt.model.Document.DocumentType;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.service.FolderService;
import com.docmgmt.service.UserService;
import com.docmgmt.ui.MainLayout;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "folders", layout = MainLayout.class)
@PageTitle("Folders | Document Management System")
public class FolderView extends VerticalLayout {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FolderView.class);

    private final FolderService folderService;
    private final DocumentService documentService;
    private final UserService userService;
    private final ContentService contentService;
    private final FileStoreService fileStoreService;
    
    private TreeGrid<Folder> folderTree;
    private Grid<SysObject> itemsGrid;
    private Folder currentFolder;
    
    private Button createFolderButton;
    private Button createSubfolderButton;
    private Button addDocumentButton;
    private Button linkDocumentButton;
    private Button removeLinkButton;
    private Button transformButton;
    private Button transformFolderButton;
    private Button moveFoldersButton;
    private Button moveToRootButton;
    
    private H3 currentFolderLabel;
    
    @Autowired
    public FolderView(FolderService folderService, DocumentService documentService, UserService userService, 
                     ContentService contentService, FileStoreService fileStoreService) {
        this.folderService = folderService;
        this.documentService = documentService;
        this.userService = userService;
        this.contentService = contentService;
        this.fileStoreService = fileStoreService;
        
        addClassName("folder-view");
        setSizeFull();
        
        H2 title = new H2("Folder Browser");
        
        HorizontalLayout toolbar = createToolbar();
        
        // Create split view: folder tree on left, contents on right
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        
        VerticalLayout treePanel = createTreePanel();
        treePanel.setWidth("40%");
        
        VerticalLayout contentsPanel = createContentsPanel();
        contentsPanel.setWidth("60%");
        
        mainContent.add(treePanel, contentsPanel);
        mainContent.expand(treePanel, contentsPanel);
        
        add(title, toolbar, mainContent);
        expand(mainContent);
        
        refreshFolderTree();
    }
    
    private HorizontalLayout createToolbar() {
        createFolderButton = new Button("New Root Folder", new Icon(VaadinIcon.FOLDER_ADD));
        createFolderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createFolderButton.addClickListener(e -> openCreateFolderDialog(null));
        
        createSubfolderButton = new Button("New Subfolder", new Icon(VaadinIcon.FOLDER_ADD));
        createSubfolderButton.setEnabled(false);
        createSubfolderButton.addClickListener(e -> {
            if (currentFolder != null) {
                openCreateFolderDialog(currentFolder);
            }
        });
        
        addDocumentButton = new Button("New Document Here", new Icon(VaadinIcon.FILE_ADD));
        addDocumentButton.setEnabled(false);
        addDocumentButton.addClickListener(e -> {
            if (currentFolder != null) {
                openCreateDocumentDialog();
            }
        });
        
        linkDocumentButton = new Button("Link Existing Document", new Icon(VaadinIcon.LINK));
        linkDocumentButton.setEnabled(false);
        linkDocumentButton.addClickListener(e -> {
            if (currentFolder != null) {
                openLinkDocumentDialog();
            }
        });
        
        moveFoldersButton = new Button("Move Selected", new Icon(VaadinIcon.FOLDER_OPEN));
        moveFoldersButton.setEnabled(false);
        moveFoldersButton.addClickListener(e -> openMoveFoldersDialog());
        
        moveToRootButton = new Button("Move to Root", new Icon(VaadinIcon.LEVEL_UP));
        moveToRootButton.setEnabled(false);
        moveToRootButton.addClickListener(e -> moveSelectedToRoot());
        
        HorizontalLayout toolbar = new HorizontalLayout(
            createFolderButton, createSubfolderButton, addDocumentButton, linkDocumentButton,
            new Hr(), moveFoldersButton, moveToRootButton
        );
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(true);
        toolbar.setSpacing(true);
        
        return toolbar;
    }
    
    private VerticalLayout createTreePanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        
        H3 treeTitle = new H3("Folder Hierarchy");
        treeTitle.getStyle().set("margin", "0");
        
        folderTree = new TreeGrid<>();
        folderTree.setSizeFull();
        folderTree.setSelectionMode(Grid.SelectionMode.SINGLE);
        
        folderTree.addHierarchyColumn(Folder::getName)
            .setHeader("Folder Name")
            .setAutoWidth(true)
            .setFlexGrow(1);
        
        folderTree.addColumn(folder -> {
            if (folder.getItems() != null) {
                return folder.getItems().size() + " items";
            }
            return "0 items";
        }).setHeader("Contents").setAutoWidth(true);
        
        folderTree.addColumn(folder -> folder.getOwner() != null ? folder.getOwner().getUsername() : "-")
            .setHeader("Owner").setAutoWidth(true);
        
        folderTree.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        
        // Handle multi-selection
        folderTree.addSelectionListener(event -> {
            // Update toolbar buttons based on selection
            boolean hasSelection = !event.getAllSelectedItems().isEmpty();
            moveFoldersButton.setEnabled(hasSelection);
            moveToRootButton.setEnabled(hasSelection);
            
            // Single selection updates the current folder for content view
            event.getFirstSelectedItem().ifPresent(this::selectFolder);
        });
        
        panel.add(treeTitle, new Hr(), folderTree);
        panel.expand(folderTree);
        
        return panel;
    }
    
    private VerticalLayout createContentsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        
        currentFolderLabel = new H3("Select a folder");
        currentFolderLabel.getStyle().set("margin", "0");
        
        HorizontalLayout contentsToolbar = new HorizontalLayout();
        contentsToolbar.setSpacing(true);
        
        removeLinkButton = new Button("Remove from Folder", new Icon(VaadinIcon.UNLINK));
        removeLinkButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        removeLinkButton.setEnabled(false);
        removeLinkButton.addClickListener(e -> removeSelectedItem());
        
        transformButton = new Button("Transform to Text", new Icon(VaadinIcon.FILE_TEXT));
        transformButton.setEnabled(false);
        transformButton.addClickListener(e -> transformSelectedToText());
        
        transformFolderButton = new Button("Transform Folder (Recursive)", new Icon(VaadinIcon.ARCHIVE));
        transformFolderButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        transformFolderButton.setEnabled(false);
        transformFolderButton.addClickListener(e -> transformFolderRecursively());
        
        contentsToolbar.add(transformButton, transformFolderButton, removeLinkButton);
        
        itemsGrid = new Grid<>(SysObject.class, false);
        itemsGrid.setSizeFull();
        
        itemsGrid.addComponentColumn(item -> {
            Icon icon;
            if (item instanceof Folder) {
                icon = new Icon(VaadinIcon.FOLDER);
                icon.setColor("var(--lumo-warning-color)"); // Gold/yellow color for folders
            } else if (item instanceof Document) {
                icon = new Icon(VaadinIcon.FILE_TEXT);
                icon.setColor("var(--lumo-primary-color)"); // Blue color for documents
            } else {
                icon = new Icon(VaadinIcon.FILE);
                icon.setColor("var(--lumo-contrast-60pct)");
            }
            icon.setSize("20px");
            return icon;
        }).setHeader("").setWidth("60px").setFlexGrow(0);
        
        itemsGrid.addColumn(SysObject::getName).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        
        itemsGrid.addColumn(item -> {
            if (item instanceof Folder) {
                return "Folder";
            } else if (item instanceof Document) {
                Document doc = (Document) item;
                return doc.getDocumentType() != null ? doc.getDocumentType().toString() : "Document";
            }
            return "SysObject";
        }).setHeader("Type").setAutoWidth(true);
        
        itemsGrid.addColumn(item -> 
            item.getMajorVersion() + "." + item.getMinorVersion()
        ).setHeader("Version").setAutoWidth(true);
        
        itemsGrid.addColumn(item -> item.getOwner() != null ? item.getOwner().getUsername() : "-")
            .setHeader("Owner").setAutoWidth(true);
        
        itemsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        
        itemsGrid.addSelectionListener(event -> {
            boolean hasSelection = event.getFirstSelectedItem().isPresent();
            removeLinkButton.setEnabled(hasSelection);
            
            // Only enable transform for documents (not folders)
            transformButton.setEnabled(hasSelection && 
                event.getFirstSelectedItem().filter(item -> item instanceof Document).isPresent());
        });
        
        // Add double-click listener to open document details
        itemsGrid.addItemDoubleClickListener(event -> {
            SysObject item = event.getItem();
            if (item instanceof Document) {
                openDocumentDetailDialog((Document) item);
            }
        });
        
        panel.add(currentFolderLabel, contentsToolbar, new Hr(), itemsGrid);
        panel.expand(itemsGrid);
        
        return panel;
    }
    
    private void selectFolder(Folder folder) {
        currentFolder = folder;
        currentFolderLabel.setText("Contents of: " + folder.getName());
        createSubfolderButton.setEnabled(true);
        addDocumentButton.setEnabled(true);
        linkDocumentButton.setEnabled(true);
        transformFolderButton.setEnabled(true);
        refreshFolderContents();
    }
    
    private void refreshFolderTree() {
        folderTree.setDataProvider(new AbstractBackEndHierarchicalDataProvider<Folder, Void>() {
            @Override
            public int getChildCount(HierarchicalQuery<Folder, Void> query) {
                Folder parent = query.getParent();
                if (parent == null) {
                    return folderService.findRootFolders().size();
                } else {
                    return folderService.findChildFolders(parent).size();
                }
            }
            
            @Override
            public boolean hasChildren(Folder folder) {
                return folderService.findChildFolders(folder).size() > 0;
            }
            
            @Override
            protected Stream<Folder> fetchChildrenFromBackEnd(HierarchicalQuery<Folder, Void> query) {
                Folder parent = query.getParent();
                if (parent == null) {
                    return folderService.findRootFolders().stream();
                } else {
                    return folderService.findChildFolders(parent).stream();
                }
            }
        });
    }
    
    private void refreshFolderContents() {
        if (currentFolder != null) {
            // Use service method that eagerly loads all relationships within transaction
            Folder refreshed = folderService.findByIdWithRelationships(currentFolder.getId());
            
            // Combine child folders and direct document items (excluding folders from items)
            List<SysObject> contents = new java.util.ArrayList<>();
            
            // Add child folders (already initialized by service)
            if (refreshed.getChildFolders() != null) {
                contents.addAll(refreshed.getChildFolders());
            }
            
            // Add only non-folder items (documents) (already initialized by service)
            if (refreshed.getItems() != null) {
                for (SysObject item : refreshed.getItems()) {
                    if (!(item instanceof Folder)) {
                        contents.add(item);
                    }
                }
            }
            
            itemsGrid.setItems(contents);
        } else {
            itemsGrid.setItems();
        }
    }
    
    private void openCreateFolderDialog(Folder parentFolder) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        
        H2 title = new H2(parentFolder == null ? "Create Root Folder" : "Create Subfolder");
        
        TextField nameField = new TextField("Folder Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        
        TextField pathField = new TextField("Path");
        pathField.setWidthFull();
        
        TextArea descField = new TextArea("Description");
        descField.setWidthFull();
        descField.setHeight("100px");
        
        ComboBox<User> ownerCombo = new ComboBox<>("Owner");
        ownerCombo.setItems(userService.findAll());
        ownerCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        ownerCombo.setPlaceholder("Select owner...");
        ownerCombo.setClearButtonVisible(true);
        ownerCombo.setWidthFull();
        
        MultiSelectComboBox<User> authorsCombo = new MultiSelectComboBox<>("Authors");
        authorsCombo.setItems(userService.findAll());
        authorsCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        authorsCombo.setPlaceholder("Search and select authors...");
        authorsCombo.setClearButtonVisible(true);
        authorsCombo.setWidthFull();
        
        FormLayout formLayout = new FormLayout(nameField, pathField, descField, ownerCombo, authorsCombo);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Create", e -> {
            if (nameField.isEmpty()) {
                Notification.show("Folder name is required", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                Folder folder = Folder.builder()
                    .name(nameField.getValue())
                    .path(pathField.getValue())
                    .description(descField.getValue())
                    .owner(ownerCombo.getValue())
                    .build();
                
                // Add authors
                if (authorsCombo.getValue() != null && !authorsCombo.getValue().isEmpty()) {
                    authorsCombo.getValue().forEach(folder::addAuthor);
                }
                
                folder = folderService.save(folder);
                
                if (parentFolder != null) {
                    folderService.addChildFolder(parentFolder.getId(), folder);
                }
                
                refreshFolderTree();
                dialog.close();
                
                Notification.show("Folder created successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                Notification.show("Error creating folder: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, saveButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(title, new Hr(), formLayout, buttons);
        layout.setPadding(false);
        layout.setSpacing(false);
        
        dialog.add(layout);
        dialog.open();
    }
    
    private void openCreateDocumentDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        
        H2 title = new H2("Create Document in " + currentFolder.getName());
        
        TextField nameField = new TextField("Document Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        
        ComboBox<Document.DocumentType> typeCombo = new ComboBox<>("Document Type");
        typeCombo.setItems(Document.DocumentType.values());
        typeCombo.setRequired(true);
        typeCombo.setWidthFull();
        
        TextArea descField = new TextArea("Description");
        descField.setWidthFull();
        descField.setHeight("100px");
        
        ComboBox<User> ownerCombo = new ComboBox<>("Owner");
        ownerCombo.setItems(userService.findAll());
        ownerCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        ownerCombo.setPlaceholder("Select owner...");
        ownerCombo.setClearButtonVisible(true);
        ownerCombo.setWidthFull();
        
        MultiSelectComboBox<User> docAuthorsCombo = new MultiSelectComboBox<>("Authors");
        docAuthorsCombo.setItems(userService.findAll());
        docAuthorsCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        docAuthorsCombo.setPlaceholder("Search and select authors...");
        docAuthorsCombo.setClearButtonVisible(true);
        docAuthorsCombo.setWidthFull();
        
        FormLayout formLayout = new FormLayout(nameField, typeCombo, descField, ownerCombo, docAuthorsCombo);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Create", e -> {
            if (nameField.isEmpty() || typeCombo.isEmpty()) {
                Notification.show("Name and type are required", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                // Create the appropriate document subclass based on type
                Document doc = createDocumentByType(typeCombo.getValue(), 
                    nameField.getValue(), descField.getValue());
                
                // Set owner and authors
                doc.setOwner(ownerCombo.getValue());
                if (docAuthorsCombo.getValue() != null && !docAuthorsCombo.getValue().isEmpty()) {
                    docAuthorsCombo.getValue().forEach(doc::addAuthor);
                }
                
                doc = documentService.save(doc);
                folderService.addItemToFolder(currentFolder.getId(), doc);
                
                refreshFolderContents();
                dialog.close();
                
                Notification.show("Document created and added to folder", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                Notification.show("Error creating document: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, saveButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(title, new Hr(), formLayout, buttons);
        layout.setPadding(false);
        layout.setSpacing(false);
        
        dialog.add(layout);
        dialog.open();
    }
    
    private void openLinkDocumentDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        
        H2 title = new H2("Link Document to " + currentFolder.getName());
        
        ComboBox<Document> documentCombo = new ComboBox<>("Select Document");
        documentCombo.setItems(documentService.findAllLatestVersions());
        documentCombo.setItemLabelGenerator(doc -> 
            doc.getName() + " v" + doc.getMajorVersion() + "." + doc.getMinorVersion()
        );
        documentCombo.setWidthFull();
        
        Span helpText = new Span("Select an existing document to link to this folder. The document can exist in multiple folders.");
        helpText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        helpText.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button linkButton = new Button("Link", e -> {
            if (documentCombo.isEmpty()) {
                Notification.show("Please select a document", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                folderService.addItemToFolder(currentFolder.getId(), documentCombo.getValue());
                refreshFolderContents();
                dialog.close();
                
                Notification.show("Document linked to folder", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                Notification.show("Error linking document: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        linkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, linkButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(title, new Hr(), documentCombo, helpText, buttons);
        layout.setPadding(true);
        
        dialog.add(layout);
        dialog.open();
    }
    
    private void removeSelectedItem() {
        itemsGrid.getSelectedItems().stream().findFirst().ifPresent(item -> {
            try {
                folderService.removeItemFromFolder(currentFolder.getId(), item);
                refreshFolderContents();
                
                Notification.show("Item removed from folder", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                Notification.show("Error removing item: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }
    
    /**
     * Create a document subclass based on the document type
     */
    private Document createDocumentByType(DocumentType type, String name, String description) {
        Document doc;
        switch (type) {
            case ARTICLE:
                doc = Article.builder().build();
                break;
            case REPORT:
                doc = Report.builder().build();
                break;
            case CONTRACT:
                doc = Contract.builder().build();
                break;
            case MANUAL:
                doc = Manual.builder().build();
                break;
            case PRESENTATION:
                doc = Presentation.builder().build();
                break;
            case TRIP_REPORT:
                doc = TripReport.builder().build();
                break;
            default:
                // Default to Article for OTHER type
                doc = Article.builder().build();
                break;
        }
        
        // Set common fields
        doc.setName(name);
        doc.setDescription(description);
        
        return doc;
    }
    
    /**
     * Open dialog to select destination folder for selected folders
     */
    private void openMoveFoldersDialog() {
        var selectedFolders = folderTree.getSelectedItems();
        if (selectedFolders.isEmpty()) {
            Notification.show("Please select folders to move", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        
        H2 title = new H2("Move " + selectedFolders.size() + " folder(s)");
        
        Span helpText = new Span("Select a destination folder. The selected folders will become children of the destination.");
        helpText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        helpText.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        // Create a TreeGrid for selecting destination folder
        TreeGrid<Folder> destinationTree = new TreeGrid<>();
        destinationTree.setHeight("400px");
        destinationTree.setSelectionMode(Grid.SelectionMode.SINGLE);
        
        destinationTree.addHierarchyColumn(Folder::getName)
            .setHeader("Destination Folder")
            .setAutoWidth(true)
            .setFlexGrow(1);
        
        destinationTree.addColumn(folder -> folder.getOwner() != null ? folder.getOwner().getUsername() : "-")
            .setHeader("Owner")
            .setAutoWidth(true);
        
        // Set up the same lazy-loading data provider
        destinationTree.setDataProvider(new AbstractBackEndHierarchicalDataProvider<Folder, Void>() {
            @Override
            public int getChildCount(HierarchicalQuery<Folder, Void> query) {
                Folder parent = query.getParent();
                if (parent == null) {
                    return folderService.findRootFolders().size();
                } else {
                    return folderService.findChildFolders(parent).size();
                }
            }
            
            @Override
            public boolean hasChildren(Folder folder) {
                return folderService.findChildFolders(folder).size() > 0;
            }
            
            @Override
            protected Stream<Folder> fetchChildrenFromBackEnd(HierarchicalQuery<Folder, Void> query) {
                Folder parent = query.getParent();
                if (parent == null) {
                    return folderService.findRootFolders().stream();
                } else {
                    return folderService.findChildFolders(parent).stream();
                }
            }
        });
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button moveButton = new Button("Move Here", e -> {
            Folder destination = destinationTree.asSingleSelect().getValue();
            if (destination == null) {
                Notification.show("Please select a destination folder", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            
            try {
                List<Long> folderIds = selectedFolders.stream()
                    .map(Folder::getId)
                    .collect(java.util.stream.Collectors.toList());
                
                folderService.linkFoldersToParent(destination.getId(), folderIds);
                
                refreshFolderTree();
                folderTree.deselectAll();
                dialog.close();
                
                Notification.show(selectedFolders.size() + " folder(s) moved to " + destination.getName(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (IllegalArgumentException ex) {
                Notification.show("Cannot move: " + ex.getMessage(), 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification.show("Error moving folders: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        moveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, moveButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(title, helpText, new Hr(), destinationTree, buttons);
        layout.setPadding(true);
        
        dialog.add(layout);
        dialog.open();
    }
    
    /**
     * Move selected folders to root level
     */
    private void moveSelectedToRoot() {
        var selectedFolders = folderTree.getSelectedItems();
        if (selectedFolders.isEmpty()) {
            Notification.show("Please select folders to move", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        
        try {
            List<Long> folderIds = selectedFolders.stream()
                .map(Folder::getId)
                .collect(java.util.stream.Collectors.toList());
            
            folderService.linkFoldersToParent(null, folderIds);
            
            refreshFolderTree();
            folderTree.deselectAll();
            
            Notification.show(selectedFolders.size() + " folder(s) moved to root level", 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception ex) {
            Notification.show("Error moving folders: " + ex.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    /**
     * Open document detail dialog with content viewing
     */
    private void openDocumentDetailDialog(Document document) {
        // Reload the document with contents eagerly loaded
        Document reloadedDoc = documentService.findById(document.getId());
        
        // Initialize contents collection
        if (reloadedDoc.getContents() != null) {
            reloadedDoc.getContents().size(); // Force initialization
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
        dialog.setHeight("80vh");
        
        H2 title = new H2("Document: " + reloadedDoc.getName());
        
        // Version picker - finds all versions in hierarchy regardless of name changes
        ComboBox<Document> versionPicker = new ComboBox<>("Version");
        List<Document> allVersions = documentService.findAllVersionsInHierarchy(reloadedDoc.getId());
        versionPicker.setItems(allVersions);
        versionPicker.setItemLabelGenerator(doc -> 
            "v" + doc.getMajorVersion() + "." + doc.getMinorVersion() + 
            " - " + doc.getName() +
            (doc.getId().equals(reloadedDoc.getId()) ? " (current)" : ""));
        versionPicker.setValue(reloadedDoc);
        versionPicker.setWidthFull();
        versionPicker.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().getId().equals(reloadedDoc.getId())) {
                dialog.close();
                openDocumentDetailDialog(e.getValue());
            }
        });
        
        // Create dynamic container for document fields
        VerticalLayout documentFieldsContainer = new VerticalLayout();
        documentFieldsContainer.setPadding(false);
        documentFieldsContainer.setSpacing(true);
        
        // Use DocumentFieldRenderer to show all fields (base + type-specific)
        DocumentFieldRenderer.renderReadOnlyFields(reloadedDoc, documentFieldsContainer);
        
        // Add version info
        HorizontalLayout versionRow = new HorizontalLayout();
        versionRow.setWidthFull();
        versionRow.setSpacing(true);
        Span versionLabel = new Span("Version:");
        versionLabel.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px")
            .set("color", "var(--lumo-secondary-text-color)");
        Span versionValue = new Span(reloadedDoc.getMajorVersion() + "." + reloadedDoc.getMinorVersion());
        versionRow.add(versionLabel, versionValue);
        documentFieldsContainer.add(versionRow);
        
        // Content list
        H3 contentTitle = new H3("Content Objects");
        contentTitle.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
        
        Grid<com.docmgmt.model.Content> contentGrid = new Grid<>(com.docmgmt.model.Content.class, false);
        contentGrid.setHeight("200px");
        
        contentGrid.addColumn(content -> content.getName())
            .setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        contentGrid.addColumn(content -> content.getContentType() != null ? content.getContentType() : "-")
            .setHeader("Type").setAutoWidth(true);
        contentGrid.addColumn(content -> content.isPrimary() ? "Primary" : "Secondary")
            .setHeader("Rendition").setAutoWidth(true);
        contentGrid.addColumn(content -> {
            if (content.isStoredInDatabase()) return "Database";
            if (content.isStoredInFileStore()) return "FileStore: " + content.getFileStore().getName();
            return "Unknown";
        }).setHeader("Storage").setAutoWidth(true);
        
        // Load content objects
        if (reloadedDoc.getContents() != null) {
            contentGrid.setItems(reloadedDoc.getContents());
        }
        
        // Content action buttons
        Button viewContentButton = new Button("View Content", new Icon(VaadinIcon.EYE));
        viewContentButton.setEnabled(false);
        viewContentButton.addClickListener(e -> {
            contentGrid.asSingleSelect().getOptionalValue().ifPresent(content -> {
                viewContent(content);
            });
        });
        
        Button uploadContentButton = new Button("Upload Content", new Icon(VaadinIcon.UPLOAD));
        uploadContentButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        uploadContentButton.addClickListener(e -> {
            dialog.close();
            openUploadContentDialogForFolder(reloadedDoc, dialog);
        });
        
        Button transformContentButton = new Button("Transform PDF", new Icon(VaadinIcon.MAGIC));
        transformContentButton.setEnabled(false);
        transformContentButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        transformContentButton.addClickListener(e -> {
            contentGrid.asSingleSelect().getOptionalValue().ifPresent(content -> {
                if (content.isPrimary() && "application/pdf".equals(content.getContentType())) {
                    try {
                        contentService.transformAndAddRendition(content.getId(), "text/plain");
                        Notification.show("PDF transformed to text", 3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        // Refresh content grid
                        Document refreshedDoc = documentService.findById(reloadedDoc.getId());
                        if (refreshedDoc.getContents() != null) {
                            refreshedDoc.getContents().size();
                        }
                        contentGrid.setItems(refreshedDoc.getContents());
                    } catch (Exception ex) {
                        Notification.show("Transform failed: " + ex.getMessage(), 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
            });
        });
        
        contentGrid.addSelectionListener(event -> {
            boolean hasSelection = event.getFirstSelectedItem().isPresent();
            viewContentButton.setEnabled(hasSelection);
            
            // Enable transform only for primary PDF content
            transformContentButton.setEnabled(hasSelection && 
                event.getFirstSelectedItem()
                    .filter(c -> c.isPrimary() && "application/pdf".equals(c.getContentType()))
                    .isPresent());
        });
        
        HorizontalLayout contentToolbar = new HorizontalLayout(
            viewContentButton, uploadContentButton, transformContentButton);
        
        // Version control buttons
        Button createMajorVersionButton = new Button("Create Major Version", new Icon(VaadinIcon.PLUS_CIRCLE));
        createMajorVersionButton.addClickListener(e -> {
            try {
                documentService.createMajorVersion(reloadedDoc.getId());
                Notification.show("Major version created", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshFolderContents();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Failed to create version: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button createMinorVersionButton = new Button("Create Minor Version", new Icon(VaadinIcon.PLUS_CIRCLE_O));
        createMinorVersionButton.addClickListener(e -> {
            try {
                documentService.createMinorVersion(reloadedDoc.getId());
                Notification.show("Minor version created", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshFolderContents();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Failed to create version: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        HorizontalLayout versionButtons = new HorizontalLayout(createMajorVersionButton, createMinorVersionButton);
        versionButtons.setSpacing(true);
        
        // Dialog buttons
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(closeButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title, versionPicker, new Hr(), 
            documentFieldsContainer,
            new Hr(),
            contentTitle,
            contentGrid,
            contentToolbar,
            new Hr(),
            versionButtons,
            buttons
        );
        layout.setPadding(true);
        layout.setSpacing(true);
        
        dialog.add(layout);
        dialog.open();
    }
    
    /**
     * View content in a new dialog
     */
    private void viewContent(com.docmgmt.model.Content content) {
        Dialog viewDialog = new Dialog();
        viewDialog.setWidth("90vw");
        viewDialog.setHeight("90vh");
        
        H2 title = new H2("Content: " + content.getName());
        
        VerticalLayout contentView = new VerticalLayout();
        contentView.setSizeFull();
        contentView.setSpacing(false);
        contentView.setPadding(false);
        
        // Info bar
        HorizontalLayout infoBar = new HorizontalLayout();
        infoBar.setWidthFull();
        infoBar.setPadding(true);
        infoBar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        
        Span typeSpan = new Span("Type: " + (content.getContentType() != null ? content.getContentType() : "Unknown"));
        Span storageSpan = new Span("Storage: " + (content.isStoredInDatabase() ? "Database" : "FileStore"));
        infoBar.add(typeSpan, storageSpan);
        
        // Content viewer area
        VerticalLayout viewerArea = new VerticalLayout();
        viewerArea.setSizeFull();
        viewerArea.setPadding(true);
        
        try {
            byte[] contentBytes = contentService.getContentBytes(content.getId());
            String contentType = content.getContentType();
            
            if (contentType != null && contentType.startsWith("text/")) {
                // Display text content
                String textContent = new String(contentBytes, java.nio.charset.StandardCharsets.UTF_8);
                com.vaadin.flow.component.html.Pre pre = new com.vaadin.flow.component.html.Pre(textContent);
                pre.getStyle()
                    .set("white-space", "pre-wrap")
                    .set("font-family", "monospace")
                    .set("padding", "10px")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("overflow", "auto");
                viewerArea.add(pre);
                
            } else if (contentType != null && contentType.equals("application/pdf")) {
                // Display PDF using iframe or embed
                String base64 = java.util.Base64.getEncoder().encodeToString(contentBytes);
                String dataUri = "data:application/pdf;base64," + base64;
                
                com.vaadin.flow.component.html.IFrame iframe = new com.vaadin.flow.component.html.IFrame(dataUri);
                iframe.setSizeFull();
                iframe.getStyle().set("border", "none");
                viewerArea.add(iframe);
                
            } else if (contentType != null && contentType.startsWith("image/")) {
                // Display image
                String base64 = java.util.Base64.getEncoder().encodeToString(contentBytes);
                String dataUri = "data:" + contentType + ";base64," + base64;
                
                com.vaadin.flow.component.html.Image image = new com.vaadin.flow.component.html.Image(dataUri, "Content image");
                image.setMaxWidth("100%");
                image.getStyle().set("display", "block").set("margin", "auto");
                viewerArea.add(image);
                
            } else {
                // Unsupported content type
                Span unsupportedMsg = new Span("Content type not supported for inline viewing: " + contentType);
                unsupportedMsg.getStyle().set("color", "var(--lumo-error-text-color)");
                
                Span sizeInfo = new Span("Size: " + formatBytes((long) contentBytes.length));
                sizeInfo.getStyle().set("margin-top", "10px");
                
                // Add download button
                Button downloadButton = new Button("Download", new Icon(VaadinIcon.DOWNLOAD));
                downloadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                downloadButton.addClickListener(e -> {
                    // Create a download link
                    com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource(
                        content.getName(),
                        () -> new java.io.ByteArrayInputStream(contentBytes)
                    );
                    
                    com.vaadin.flow.component.html.Anchor downloadLink = new com.vaadin.flow.component.html.Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);
                    downloadLink.getElement().executeJs("this.click();");
                });
                
                viewerArea.add(unsupportedMsg, sizeInfo, downloadButton);
            }
            
        } catch (Exception e) {
            Span errorMsg = new Span("Error loading content: " + e.getMessage());
            errorMsg.getStyle().set("color", "var(--lumo-error-text-color)");
            viewerArea.add(errorMsg);
        }
        
        contentView.add(infoBar, viewerArea);
        contentView.expand(viewerArea);
        
        Button closeButton = new Button("Close", e -> viewDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout footer = new HorizontalLayout(closeButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();
        footer.setPadding(true);
        
        VerticalLayout layout = new VerticalLayout(title, contentView, footer);
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.expand(contentView);
        
        viewDialog.add(layout);
        viewDialog.open();
    }
    
    /**
     * Transform selected document's PDF content to text
     */
    private void transformSelectedToText() {
        itemsGrid.getSelectedItems().stream()
            .filter(item -> item instanceof Document)
            .findFirst()
            .ifPresent(item -> {
                Document document = (Document) item;
                
                // Reload with contents
                Document reloadedDoc = documentService.findById(document.getId());
                if (reloadedDoc.getContents() != null) {
                    reloadedDoc.getContents().size(); // Force init
                }
                
                // Find PDF content
                java.util.List<com.docmgmt.model.Content> pdfContents = reloadedDoc.getContents().stream()
                    .filter(c -> "application/pdf".equals(c.getContentType()) && c.isPrimary())
                    .toList();
                
                if (pdfContents.isEmpty()) {
                    Notification.show("No PDF content found in this document", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                
                // Transform each PDF
                int success = 0;
                int failed = 0;
                StringBuilder errors = new StringBuilder();
                
                for (com.docmgmt.model.Content pdfContent : pdfContents) {
                    try {
                        contentService.transformAndAddRendition(pdfContent.getId(), "text/plain");
                        success++;
                    } catch (Exception e) {
                        logger.warn("Failed to transform content {} in document {}: {}", 
                            pdfContent.getId(), document.getName(), e.getMessage());
                        failed++;
                        if (errors.length() < 200) { // Limit error message length
                            errors.append("\n- ").append(e.getMessage());
                        }
                    }
                }
                
                if (success > 0) {
                    Notification.show("Transformed " + success + " PDF(s) to text", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                
                if (failed > 0) {
                    String message = "Failed to transform " + failed + " PDF(s)";
                    if (errors.length() > 0) {
                        message += errors.toString();
                    }
                    Notification notification = Notification.show(message, 
                        5000, Notification.Position.BOTTOM_START);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
    }
    
    /**
     * Transform all PDFs in the current folder and all subfolders recursively
     */
    private void transformFolderRecursively() {
        if (currentFolder == null) {
            return;
        }
        
        // Confirm with user
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Transform Folder Recursively");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Span("This will transform all PDF documents in \"" + currentFolder.getName() + 
            "\" and all subfolders to text renditions."));
        content.add(new Span("This may take a while for large folders."));
        
        Button confirmButton = new Button("Transform All", e -> {
            confirmDialog.close();
            performRecursiveTransform(currentFolder);
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> confirmDialog.close());
        
        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        content.add(buttons);
        
        confirmDialog.add(content);
        confirmDialog.open();
    }
    
    /**
     * Recursively transform all PDFs in a folder and its subfolders
     */
    private void performRecursiveTransform(Folder folder) {
        Notification.show("Starting transformation of folder: " + folder.getName(), 
            3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        
        // Process in background to avoid blocking UI
        new Thread(() -> {
            try {
                int[] counts = new int[2]; // [success, failed]
                transformFolderRecursive(folder, counts);
                
                // Update UI from UI thread
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (counts[0] > 0) {
                        Notification.show("Successfully transformed " + counts[0] + " PDF(s) to text" +
                            (counts[1] > 0 ? " (" + counts[1] + " failed)" : ""), 
                            5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } else if (counts[1] > 0) {
                        Notification.show("Failed to transform " + counts[1] + " PDF(s)", 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    } else {
                        Notification.show("No PDFs found to transform", 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                    }
                }));
            } catch (Exception e) {
                logger.error("Error during recursive transformation", e);
                getUI().ifPresent(ui -> ui.access(() -> {
                    Notification.show("Error during transformation: " + e.getMessage(), 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }));
            }
        }).start();
    }
    
    /**
     * Helper method to recursively transform PDFs in a folder
     */
    private void transformFolderRecursive(Folder folder, int[] counts) {
        // Reload folder with items
        Folder reloadedFolder = folderService.findById(folder.getId());
        if (reloadedFolder.getItems() != null) {
            reloadedFolder.getItems().size(); // Force init
        }
        
        if (reloadedFolder.getItems() != null) {
            for (SysObject item : reloadedFolder.getItems()) {
                if (item instanceof Document) {
                    Document doc = documentService.findById(item.getId());
                    if (doc.getContents() != null) {
                        doc.getContents().size(); // Force init
                        
                        // Find and transform PDF content
                        for (com.docmgmt.model.Content content : doc.getContents()) {
                            if ("application/pdf".equals(content.getContentType()) && content.isPrimary()) {
                                try {
                                    contentService.transformAndAddRendition(content.getId(), "text/plain");
                                    counts[0]++;
                                    logger.info("Transformed PDF in document: {}", doc.getName());
                                } catch (Exception e) {
                                    counts[1]++;
                                    logger.warn("Failed to transform PDF in document {}: {}", 
                                        doc.getName(), e.getMessage());
                                }
                            }
                        }
                    }
                } else if (item instanceof Folder) {
                    // Recursively process subfolders
                    transformFolderRecursive((Folder) item, counts);
                }
            }
        }
        
        // Also check child folders
        if (reloadedFolder.getChildFolders() != null) {
            reloadedFolder.getChildFolders().size(); // Force init
            for (Folder childFolder : reloadedFolder.getChildFolders()) {
                transformFolderRecursive(childFolder, counts);
            }
        }
    }
    
    /**
     * Open upload content dialog for a document from folder view
     */
    private void openUploadContentDialogForFolder(Document document, Dialog parentDialog) {
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
            if (isFileStore && fileStoreCombo.getValue() == null) {
                fileStoreCombo.getListDataView().getItems().findFirst().ifPresent(fileStoreCombo::setValue);
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
        Button cancelButton = new Button("Cancel", e -> {
            uploadDialog.close();
            parentDialog.open();
        });
        
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
                refreshFolderContents();
                // Reopen parent dialog to show updated content
                parentDialog.open();
                
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
     * Format bytes to human readable string
     */
    private String formatBytes(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
