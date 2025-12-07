package com.docmgmt.ui.views;

import com.docmgmt.model.*;
import com.docmgmt.model.Document.DocumentType;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FolderService;
import com.docmgmt.service.UserService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Stream;

@Route(value = "folders", layout = MainLayout.class)
@PageTitle("Folders | Document Management System")
public class FolderView extends VerticalLayout {

    private final FolderService folderService;
    private final DocumentService documentService;
    private final UserService userService;
    
    private TreeGrid<Folder> folderTree;
    private Grid<SysObject> itemsGrid;
    private Folder currentFolder;
    
    private Button createFolderButton;
    private Button createSubfolderButton;
    private Button addDocumentButton;
    private Button linkDocumentButton;
    private Button removeLinkButton;
    
    private H3 currentFolderLabel;
    
    @Autowired
    public FolderView(FolderService folderService, DocumentService documentService, UserService userService) {
        this.folderService = folderService;
        this.documentService = documentService;
        this.userService = userService;
        
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
        
        HorizontalLayout toolbar = new HorizontalLayout(
            createFolderButton, createSubfolderButton, addDocumentButton, linkDocumentButton
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
        
        folderTree.addSelectionListener(event -> {
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
        
        contentsToolbar.add(removeLinkButton);
        
        itemsGrid = new Grid<>(SysObject.class, false);
        itemsGrid.setSizeFull();
        
        itemsGrid.addComponentColumn(item -> {
            Icon icon;
            if (item instanceof Folder) {
                icon = new Icon(VaadinIcon.FOLDER_O);
            } else if (item instanceof Document) {
                icon = new Icon(VaadinIcon.FILE_TEXT_O);
            } else {
                icon = new Icon(VaadinIcon.FILE_O);
            }
            icon.setSize("16px");
            return icon;
        }).setHeader("").setWidth("50px").setFlexGrow(0);
        
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
            removeLinkButton.setEnabled(event.getFirstSelectedItem().isPresent());
        });
        
        panel.add(currentFolderLabel, contentsToolbar, new Hr(), itemsGrid);
        panel.expand(itemsGrid);
        
        return panel;
    }
    
    private void selectFolder(Folder folder) {
        currentFolder = folder;
        currentFolderLabel.setText("Contents of: " + folder.getName());
        
        // Enable buttons
        createSubfolderButton.setEnabled(true);
        addDocumentButton.setEnabled(true);
        linkDocumentButton.setEnabled(true);
        
        // Load folder contents
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
            Folder refreshed = folderService.findById(currentFolder.getId());
            itemsGrid.setItems(refreshed.getItems());
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
}
