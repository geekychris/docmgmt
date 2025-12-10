package com.docmgmt.ui.views;

import com.docmgmt.dto.FieldSuggestionDTO;
import com.docmgmt.model.*;
import com.docmgmt.ui.components.DocumentDetailDialog;
import com.docmgmt.model.Document.DocumentType;
import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.DocumentFieldExtractionService;
import com.docmgmt.service.DocumentSimilarityService;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.service.FolderService;
import com.docmgmt.service.UserService;
import com.docmgmt.dto.PluginInfoDTO;
import com.docmgmt.plugin.PluginService;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.plugin.PluginException;
import com.docmgmt.ui.components.PluginExecutionDialog;
import com.docmgmt.ui.components.PluginResultDialog;
import com.docmgmt.transformer.TransformerRegistry;
import com.docmgmt.ui.MainLayout;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.docmgmt.ui.util.ColorPickerUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.menubar.MenuBar;
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
import com.vaadin.flow.component.splitlayout.SplitLayout;
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
import java.util.concurrent.CompletableFuture;
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
    private final TransformerRegistry transformerRegistry;
    private final DocumentFieldExtractionService fieldExtractionService;
    private final PluginService pluginService;
    private final DocumentSimilarityService similarityService;
    private final LuceneIndexService luceneIndexService;
    private final com.docmgmt.service.TileService tileService;
    
    private TreeGrid<Folder> folderTree;
    private Grid<SysObject> itemsGrid;
    private Folder currentFolder;
    
    private Button createFolderButton;
    private Button createSubfolderButton;
    private Button addDocumentButton;
    private Button editFolderButton;
    private Button removeLinkButton;
    private Button transformButton;
    private Button transformFolderButton;
    private MenuBar actionsMenu;
    private MenuBar viewMenu;
    private MenuBar toolsMenu;
    private MenuItem linkDocumentItem;
    private MenuItem moveFoldersItem;
    private MenuItem moveToRootItem;
    private MenuItem rebuildIndexItem;
    private MenuItem batchExtractFieldsItem;
    private MenuItem importDirectoryItem;
    private MenuItem viewTilesItem;
    private MenuItem configureTilesItem;
    
    private H3 currentFolderLabel;
    
    @Autowired
    public FolderView(FolderService folderService, DocumentService documentService, UserService userService, 
                     ContentService contentService, FileStoreService fileStoreService,
                     TransformerRegistry transformerRegistry, DocumentFieldExtractionService fieldExtractionService,
                     PluginService pluginService, DocumentSimilarityService similarityService,
                     LuceneIndexService luceneIndexService, com.docmgmt.service.TileService tileService) {
        this.folderService = folderService;
        this.documentService = documentService;
        this.userService = userService;
        this.contentService = contentService;
        this.fileStoreService = fileStoreService;
        this.transformerRegistry = transformerRegistry;
        this.fieldExtractionService = fieldExtractionService;
        this.pluginService = pluginService;
        this.similarityService = similarityService;
        this.luceneIndexService = luceneIndexService;
        this.tileService = tileService;
        
        addClassName("folder-view");
        setSizeFull();
        
        H2 title = new H2("Folder Browser");
        
        HorizontalLayout toolbar = createToolbar();
        
        // Create split view: folder tree on left, contents on right
        SplitLayout mainContent = new SplitLayout();
        mainContent.setSizeFull();
        mainContent.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        mainContent.setSplitterPosition(30); // 30% for tree, 70% for contents
        
        VerticalLayout treePanel = createTreePanel();
        VerticalLayout contentsPanel = createContentsPanel();
        
        mainContent.addToPrimary(treePanel);
        mainContent.addToSecondary(contentsPanel);
        
        add(title, toolbar, mainContent);
        expand(mainContent);
        
        refreshFolderTree();
    }
    
    private HorizontalLayout createToolbar() {
        // Primary action buttons - always visible
        createFolderButton = new Button("New Root", new Icon(VaadinIcon.FOLDER_ADD));
        createFolderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createFolderButton.addClickListener(e -> openCreateFolderDialog(null));
        
        createSubfolderButton = new Button("New Subfolder", new Icon(VaadinIcon.FOLDER_ADD));
        createSubfolderButton.setEnabled(false);
        createSubfolderButton.addClickListener(e -> {
            if (currentFolder != null) {
                openCreateFolderDialog(currentFolder);
            }
        });
        
        addDocumentButton = new Button("New Document", new Icon(VaadinIcon.FILE_ADD));
        addDocumentButton.setEnabled(false);
        addDocumentButton.addClickListener(e -> {
            if (currentFolder != null) {
                openCreateDocumentDialog();
            }
        });
        
        editFolderButton = new Button(new Icon(VaadinIcon.EDIT));
        editFolderButton.setEnabled(false);
        editFolderButton.setTooltipText("Edit Folder");
        editFolderButton.addClickListener(e -> {
            if (currentFolder != null) {
                openEditFolderDialog(currentFolder);
            }
        });
        
        // Actions menu
        actionsMenu = new MenuBar();
        MenuItem actionsMenuItem = actionsMenu.addItem("Actions");
        actionsMenuItem.addComponentAsFirst(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        SubMenu actionsSubMenu = actionsMenuItem.getSubMenu();
        
        linkDocumentItem = actionsSubMenu.addItem("Link Existing Document", e -> {
            if (currentFolder != null) {
                openLinkDocumentDialog();
            }
        });
        linkDocumentItem.addComponentAsFirst(new Icon(VaadinIcon.LINK));
        linkDocumentItem.setEnabled(false);
        
        moveFoldersItem = actionsSubMenu.addItem("Move Selected", e -> openMoveFoldersDialog());
        moveFoldersItem.addComponentAsFirst(new Icon(VaadinIcon.FOLDER_OPEN));
        moveFoldersItem.setEnabled(false);
        
        moveToRootItem = actionsSubMenu.addItem("Move to Root", e -> moveSelectedToRoot());
        moveToRootItem.addComponentAsFirst(new Icon(VaadinIcon.LEVEL_UP));
        moveToRootItem.setEnabled(false);
        
        // View menu
        viewMenu = new MenuBar();
        MenuItem viewMenuItem = viewMenu.addItem("View");
        viewMenuItem.addComponentAsFirst(new Icon(VaadinIcon.EYE));
        SubMenu viewSubMenu = viewMenuItem.getSubMenu();
        
        viewTilesItem = viewSubMenu.addItem("View as Tiles", e -> {
            if (currentFolder != null) {
                getUI().ifPresent(ui -> ui.navigate("tiles/" + currentFolder.getName()));
            }
        });
        viewTilesItem.addComponentAsFirst(new Icon(VaadinIcon.GRID_SMALL));
        viewTilesItem.setEnabled(false);
        
        configureTilesItem = viewSubMenu.addItem("Configure Tiles", e -> {
            if (currentFolder != null) {
                openTileConfigurationDialog();
            }
        });
        configureTilesItem.addComponentAsFirst(new Icon(VaadinIcon.COG));
        configureTilesItem.setEnabled(false);
        
        // Tools menu
        toolsMenu = new MenuBar();
        MenuItem toolsMenuItem = toolsMenu.addItem("Tools");
        toolsMenuItem.addComponentAsFirst(new Icon(VaadinIcon.TOOLS));
        SubMenu toolsSubMenu = toolsMenuItem.getSubMenu();
        
        rebuildIndexItem = toolsSubMenu.addItem("Rebuild Index", e -> openRebuildIndexDialog());
        rebuildIndexItem.addComponentAsFirst(new Icon(VaadinIcon.REFRESH));
        rebuildIndexItem.setEnabled(false);
        
        batchExtractFieldsItem = toolsSubMenu.addItem("AI Extract Fields (Batch)", e -> batchExtractFields());
        batchExtractFieldsItem.addComponentAsFirst(new Icon(VaadinIcon.LIGHTBULB));
        batchExtractFieldsItem.setEnabled(false);
        
        importDirectoryItem = toolsSubMenu.addItem("Import from Directory", e -> openImportDirectoryDialog());
        importDirectoryItem.addComponentAsFirst(new Icon(VaadinIcon.DOWNLOAD));
        importDirectoryItem.setEnabled(false);
        
        HorizontalLayout toolbar = new HorizontalLayout(
            createFolderButton, createSubfolderButton, addDocumentButton, editFolderButton,
            actionsMenu, viewMenu, toolsMenu
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
            .setResizable(true)
            .setAutoWidth(true)
            .setFlexGrow(1);
        
        folderTree.addColumn(folder -> {
            if (folder.getItems() != null) {
                return folder.getItems().size() + " items";
            }
            return "0 items";
        }).setHeader("Contents").setResizable(true).setAutoWidth(true);
        
        folderTree.addColumn(folder -> {
            try {
                return folder.getOwner() != null ? folder.getOwner().getUsername() : "-";
            } catch (org.hibernate.LazyInitializationException e) {
                return "-";
            }
        }).setHeader("Owner").setResizable(true).setAutoWidth(true);
        
        folderTree.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        
        // Handle multi-selection
        folderTree.addSelectionListener(event -> {
            // Update toolbar buttons based on selection
            boolean hasSelection = !event.getAllSelectedItems().isEmpty();
            moveFoldersItem.setEnabled(hasSelection);
            moveToRootItem.setEnabled(hasSelection);
            
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
        
        itemsGrid.addColumn(SysObject::getName).setHeader("Name").setResizable(true).setAutoWidth(true).setFlexGrow(1);
        
        itemsGrid.addColumn(item -> {
            if (item instanceof Folder) {
                return "Folder";
            } else if (item instanceof Document) {
                Document doc = (Document) item;
                return doc.getDocumentType() != null ? doc.getDocumentType().toString() : "Document";
            }
            return "SysObject";
        }).setHeader("Type").setResizable(true).setAutoWidth(true);
        
        itemsGrid.addColumn(item -> 
            item.getMajorVersion() + "." + item.getMinorVersion()
        ).setHeader("Version").setResizable(true).setAutoWidth(true);
        
        itemsGrid.addColumn(item -> item.getOwner() != null ? item.getOwner().getUsername() : "-")
            .setHeader("Owner").setResizable(true).setAutoWidth(true);
        
        // Add Actions column with View and Edit buttons
        itemsGrid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            if (item instanceof Document) {
                Document doc = (Document) item;
                
                Button viewButton = new Button(new Icon(VaadinIcon.EYE));
                viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                viewButton.getElement().setAttribute("title", "View Details");
                viewButton.addClickListener(e -> openDocumentDetailDialog(doc, false));
                
                Button editButton = new Button(new Icon(VaadinIcon.EDIT));
                editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                editButton.getElement().setAttribute("title", "Edit Document");
                editButton.addClickListener(e -> openDocumentDetailDialog(doc, true));
                
                actions.add(viewButton, editButton);
            }
            
            return actions;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);
        
        itemsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        
        itemsGrid.addSelectionListener(event -> {
            boolean hasSelection = event.getFirstSelectedItem().isPresent();
            removeLinkButton.setEnabled(hasSelection);
            
            // Only enable transform for documents (not folders)
            transformButton.setEnabled(hasSelection && 
                event.getFirstSelectedItem().filter(item -> item instanceof Document).isPresent());
            
            // Enable batch extract for multiple documents selected
            long docCount = itemsGrid.getSelectedItems().stream()
                .filter(item -> item instanceof Document)
                .count();
            batchExtractFieldsItem.setEnabled(docCount > 0);
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
        editFolderButton.setEnabled(true);
        addDocumentButton.setEnabled(true);
        linkDocumentItem.setEnabled(true);
        viewTilesItem.setEnabled(true);
        configureTilesItem.setEnabled(true);
        transformFolderButton.setEnabled(true);
        rebuildIndexItem.setEnabled(true);
        importDirectoryItem.setEnabled(true);
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
        
        TextField urlField = new TextField("URL");
        urlField.setPlaceholder("https://example.com");
        urlField.setWidthFull();
        
        ComboBox<String> colorCombo = ColorPickerUtil.createColorPicker(null);
        
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
        
        FormLayout formLayout = new FormLayout(nameField, pathField, descField, urlField, colorCombo, ownerCombo, authorsCombo);
        
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
                    .url(urlField.getValue())
                    .color(colorCombo.getValue())
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
    
    private void openEditFolderDialog(Folder folder) {
        // Reload folder with all relationships to avoid lazy initialization issues
        Folder managedFolder = folderService.findByIdWithRelationships(folder.getId());
        
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        
        H2 title = new H2("Edit Folder: " + managedFolder.getName());
        
        TextField nameField = new TextField("Folder Name");
        nameField.setValue(managedFolder.getName() != null ? managedFolder.getName() : "");
        nameField.setRequired(true);
        nameField.setWidthFull();
        
        TextField pathField = new TextField("Path");
        pathField.setValue(managedFolder.getPath() != null ? managedFolder.getPath() : "");
        pathField.setWidthFull();
        
        TextArea descField = new TextArea("Description");
        descField.setValue(managedFolder.getDescription() != null ? managedFolder.getDescription() : "");
        descField.setWidthFull();
        descField.setHeight("100px");
        
        TextField urlField = new TextField("URL");
        urlField.setValue(managedFolder.getUrl() != null ? managedFolder.getUrl() : "");
        urlField.setPlaceholder("https://example.com");
        urlField.setWidthFull();
        
        ComboBox<String> colorCombo = ColorPickerUtil.createColorPicker(managedFolder.getColor());
        
        ComboBox<User> ownerCombo = new ComboBox<>("Owner");
        ownerCombo.setItems(userService.findAll());
        ownerCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        ownerCombo.setValue(managedFolder.getOwner());
        ownerCombo.setPlaceholder("Select owner...");
        ownerCombo.setClearButtonVisible(true);
        ownerCombo.setWidthFull();
        
        MultiSelectComboBox<User> authorsCombo = new MultiSelectComboBox<>("Authors");
        authorsCombo.setItems(userService.findAll());
        authorsCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        // Initialize and set authors if present
        try {
            if (managedFolder.getAuthors() != null) {
                // Force initialization by calling size()
                managedFolder.getAuthors().size();
                if (!managedFolder.getAuthors().isEmpty()) {
                    authorsCombo.setValue(managedFolder.getAuthors());
                }
            }
        } catch (Exception e) {
            // If authors can't be loaded, just skip setting them
        }
        authorsCombo.setPlaceholder("Search and select authors...");
        authorsCombo.setClearButtonVisible(true);
        authorsCombo.setWidthFull();
        
        FormLayout formLayout = new FormLayout(nameField, pathField, descField, urlField, colorCombo, ownerCombo, authorsCombo);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty()) {
                Notification.show("Folder name is required", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                // Use transactional service method to update folder
                folderService.updateFolder(
                    managedFolder.getId(),
                    nameField.getValue(),
                    pathField.getValue(),
                    descField.getValue(),
                    urlField.getValue(),
                    colorCombo.getValue(),
                    ownerCombo.getValue(),
                    authorsCombo.getValue()
                );
                refreshFolderTree();
                refreshFolderContents();
                dialog.close();
                
                Notification.show("Folder updated successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                Notification.show("Error updating folder: " + ex.getMessage(), 
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
        
        TextField docUrlField = new TextField("URL");
        docUrlField.setPlaceholder("https://example.com");
        docUrlField.setWidthFull();
        
        ComboBox<String> docColorCombo = ColorPickerUtil.createColorPicker(null);
        
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
        
        FormLayout formLayout = new FormLayout(nameField, typeCombo, descField, docUrlField, docColorCombo, ownerCombo, docAuthorsCombo);
        
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
                
                // Set URL, color, owner and authors
                doc.setUrl(docUrlField.getValue());
                doc.setColor(docColorCombo.getValue());
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
     * Open document detail dialog with content viewing and editing
     */
    private void openDocumentDetailDialog(Document document) {
        openDocumentDetailDialog(document, false);
    }
    
    /**
     * Open document detail dialog with content viewing and optional edit mode
     */
    private void openDocumentDetailDialog(Document document, boolean editMode) {
        // For view mode, use the shared DocumentDetailDialog component
        if (!editMode) {
            DocumentDetailDialog detailDialog = new DocumentDetailDialog(
                document,
                documentService,
                userService,
                contentService,
                pluginService,
                similarityService,
                fieldExtractionService,
                fileStoreService
            );
            detailDialog.open();
            return;
        }
        
        // Edit mode uses inline dialog (for now)
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
                openDocumentDetailDialog(e.getValue(), editMode);
            }
        });
        
        // Edit mode toggle
        Checkbox editModeCheckbox = new Checkbox("Edit Mode");
        editModeCheckbox.setValue(editMode);
        editModeCheckbox.addValueChangeListener(e -> {
            dialog.close();
            openDocumentDetailDialog(reloadedDoc, e.getValue());
        });
        
        HorizontalLayout versionRow = new HorizontalLayout(versionPicker, editModeCheckbox);
        versionRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        versionRow.setSpacing(true);
        versionRow.setWidthFull();
        
        // Create dynamic container for document fields
        VerticalLayout documentFieldsContainer = new VerticalLayout();
        documentFieldsContainer.setPadding(false);
        documentFieldsContainer.setSpacing(true);
        
        if (editMode) {
            // Create editable form
            FormLayout formLayout = new FormLayout();
            
            TextField nameField = new TextField("Name");
            nameField.setValue(reloadedDoc.getName() != null ? reloadedDoc.getName() : "");
            nameField.setWidthFull();
            nameField.addValueChangeListener(e -> reloadedDoc.setName(e.getValue()));
            
            TextArea descriptionField = new TextArea("Description");
            descriptionField.setValue(reloadedDoc.getDescription() != null ? reloadedDoc.getDescription() : "");
            descriptionField.setWidthFull();
            descriptionField.setHeight("100px");
            descriptionField.addValueChangeListener(e -> reloadedDoc.setDescription(e.getValue()));
            
            TextField urlField = new TextField("URL");
            urlField.setValue(reloadedDoc.getUrl() != null ? reloadedDoc.getUrl() : "");
            urlField.setPlaceholder("https://example.com");
            urlField.setWidthFull();
            urlField.addValueChangeListener(e -> reloadedDoc.setUrl(e.getValue()));
            
            ComboBox<String> colorCombo = ColorPickerUtil.createColorPicker(reloadedDoc.getColor());
            colorCombo.addValueChangeListener(e -> reloadedDoc.setColor(e.getValue()));
            
            TextField keywordsField = new TextField("Keywords");
            keywordsField.setValue(reloadedDoc.getKeywords() != null ? reloadedDoc.getKeywords() : "");
            keywordsField.setWidthFull();
            keywordsField.addValueChangeListener(e -> reloadedDoc.setKeywords(e.getValue()));
            
            TextArea tagsField = new TextArea("Tags (comma separated)");
            if (reloadedDoc.getTags() != null && !reloadedDoc.getTags().isEmpty()) {
                tagsField.setValue(String.join(", ", reloadedDoc.getTags()));
            }
            tagsField.setWidthFull();
            tagsField.setHeight("80px");
            tagsField.addValueChangeListener(e -> {
                if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                    reloadedDoc.setTags(new java.util.HashSet<>());
                } else {
                    java.util.Set<String> tags = java.util.Arrays.stream(e.getValue().split(","))
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());
                    reloadedDoc.setTags(tags);
                }
            });
            
            ComboBox<User> ownerCombo = new ComboBox<>("Owner");
            ownerCombo.setItems(userService.findAll());
            ownerCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
            ownerCombo.setValue(reloadedDoc.getOwner());
            ownerCombo.setWidthFull();
            ownerCombo.addValueChangeListener(e -> reloadedDoc.setOwner(e.getValue()));
            
            MultiSelectComboBox<User> authorsCombo = new MultiSelectComboBox<>("Authors");
            authorsCombo.setItems(userService.findAll());
            authorsCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
            if (reloadedDoc.getAuthors() != null) {
                authorsCombo.setValue(reloadedDoc.getAuthors());
            }
            authorsCombo.setWidthFull();
            authorsCombo.addValueChangeListener(e -> {
                reloadedDoc.getAuthors().clear();
                if (e.getValue() != null) {
                    reloadedDoc.getAuthors().addAll(e.getValue());
                }
            });
            
            formLayout.add(nameField, descriptionField, urlField, colorCombo, keywordsField, tagsField, ownerCombo, authorsCombo);
            
            // Add type-specific editable fields using DocumentFieldRenderer
            DocumentFieldRenderer.renderEditableFields(reloadedDoc, formLayout, ownerCombo, authorsCombo, userService.findAll());
            
            formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
            
            documentFieldsContainer.add(formLayout);
        } else {
            // Use DocumentFieldRenderer to show all fields (base + type-specific)
            DocumentFieldRenderer.renderReadOnlyFields(reloadedDoc, documentFieldsContainer);
        }
        
        // Add version info
        HorizontalLayout versionInfoRow = new HorizontalLayout();
        versionInfoRow.setWidthFull();
        versionInfoRow.setSpacing(true);
        Span versionLabel = new Span("Version:");
        versionLabel.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px")
            .set("color", "var(--lumo-secondary-text-color)");
        Span versionValue = new Span(reloadedDoc.getMajorVersion() + "." + reloadedDoc.getMinorVersion());
        versionInfoRow.add(versionLabel, versionValue);
        documentFieldsContainer.add(versionInfoRow);
        
        // Content list
        H3 contentTitle = new H3("Content Objects");
        contentTitle.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
        
        Grid<com.docmgmt.model.Content> contentGrid = new Grid<>(com.docmgmt.model.Content.class, false);
        contentGrid.setHeight("200px");
        
        contentGrid.addColumn(content -> content.getName())
            .setHeader("Name").setResizable(true).setAutoWidth(true).setFlexGrow(1);
        contentGrid.addColumn(content -> content.getContentType() != null ? content.getContentType() : "-")
            .setHeader("Type").setResizable(true).setAutoWidth(true);
        contentGrid.addColumn(content -> content.isPrimary() ? "Primary" : "Secondary")
            .setHeader("Rendition").setResizable(true).setAutoWidth(true);
        contentGrid.addColumn(content -> {
            if (content.isStoredInDatabase()) return "Database";
            if (content.isStoredInFileStore()) return "FileStore: " + content.getFileStore().getName();
            return "Unknown";
        }).setHeader("Storage").setResizable(true).setAutoWidth(true);
        
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
        
        Button extractFieldsButton = new Button("Extract Fields (AI)", new Icon(VaadinIcon.LIGHTBULB));
        extractFieldsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        
        // Check if document has text content
        boolean hasTextContent = reloadedDoc.getContents() != null && reloadedDoc.getContents().stream()
            .anyMatch(c -> (c.getContentType() != null && 
                          (c.getContentType().startsWith("text/") || 
                           ("text/plain".equals(c.getContentType()) && c.isIndexable()))));
        
        extractFieldsButton.setEnabled(hasTextContent);
        if (!hasTextContent) {
            extractFieldsButton.setTooltipText("No text content available. Upload a text file or transform PDF to text first.");
        }
        
        extractFieldsButton.addClickListener(e -> {
            dialog.close();
            openFieldExtractionDialog(reloadedDoc);
        });
        
        // AI Plugins menu
        MenuBar pluginsMenu = new MenuBar();
        MenuItem pluginsMenuItem = pluginsMenu.addItem("AI Plugins");
        SubMenu pluginsSubMenu = pluginsMenuItem.getSubMenu();
        
        // Load plugins and group by category
        java.util.List<PluginInfoDTO> allPlugins = pluginService.getDetailedPluginInfo();
        java.util.Map<String, java.util.List<PluginInfoDTO>> pluginsByCategory = new java.util.LinkedHashMap<>();
        
        for (PluginInfoDTO pluginInfo : allPlugins) {
            pluginsByCategory.computeIfAbsent(pluginInfo.getCategory(), k -> new java.util.ArrayList<>()).add(pluginInfo);
        }
        
        // Add categorized plugins to menu
        for (java.util.Map.Entry<String, java.util.List<PluginInfoDTO>> entry : pluginsByCategory.entrySet()) {
            MenuItem categoryItem = pluginsSubMenu.addItem(entry.getKey());
            SubMenu categorySubMenu = categoryItem.getSubMenu();
            
            for (PluginInfoDTO pluginInfo : entry.getValue()) {
                Icon pluginIcon = VaadinIcon.valueOf(pluginInfo.getIcon()).create();
                MenuItem pluginItem = categorySubMenu.addItem(pluginInfo.getDescription());
                pluginItem.addComponentAsFirst(pluginIcon);
                pluginItem.addClickListener(evt -> {
                    dialog.close();
                    openPluginDialog(reloadedDoc, pluginInfo);
                });
            }
        }
        
        pluginsMenu.setEnabled(hasTextContent);
        
        HorizontalLayout contentToolbar = new HorizontalLayout(
            viewContentButton, uploadContentButton, transformContentButton, extractFieldsButton, pluginsMenu);
        
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
        
        // Find Similar button
        Button findSimilarButton = new Button("Find Similar Documents", new Icon(VaadinIcon.SEARCH));
        findSimilarButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        findSimilarButton.addClickListener(e -> {
            dialog.close();
            openSimilarityDialog(reloadedDoc);
        });
        
        HorizontalLayout similarityButtons = new HorizontalLayout(findSimilarButton);
        similarityButtons.setSpacing(true);
        
        // Dialog buttons
        HorizontalLayout buttons;
        if (editMode) {
            Button saveButton = new Button("Save Changes", new Icon(VaadinIcon.CHECK), e -> {
                try {
                    documentService.save(reloadedDoc);
                    Notification.show("Document saved successfully", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshFolderContents();
                    dialog.close();
                    // Optionally reopen in view mode
                    openDocumentDetailDialog(reloadedDoc, false);
                } catch (Exception ex) {
                    Notification.show("Failed to save document: " + ex.getMessage(), 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button cancelButton = new Button("Cancel", e -> {
                dialog.close();
                openDocumentDetailDialog(document, false);
            });
            
            buttons = new HorizontalLayout(cancelButton, saveButton);
        } else {
            Button closeButton = new Button("Close", e -> dialog.close());
            closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            buttons = new HorizontalLayout(closeButton);
        }
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title, versionRow, new Hr(), 
            documentFieldsContainer,
            new Hr(),
            contentTitle,
            contentGrid,
            contentToolbar,
            new Hr(),
            versionButtons,
            similarityButtons,
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
            
            if (contentType != null && contentType.equals("text/markdown")) {
                // Display markdown content with rendering
                String textContent = new String(contentBytes, java.nio.charset.StandardCharsets.UTF_8);
                com.vaadin.flow.component.html.Div markdownDiv = new com.vaadin.flow.component.html.Div();
                markdownDiv.getStyle()
                    .set("padding", "20px")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("overflow", "auto")
                    .set("line-height", "1.6");
                
                // Convert markdown to HTML-like rendering using simple replacements
                String htmlContent = renderMarkdownToHtml(textContent);
                markdownDiv.getElement().setProperty("innerHTML", htmlContent);
                viewerArea.add(markdownDiv);
                
            } else if (contentType != null && contentType.startsWith("text/")) {
                // Display plain text content
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
     * Transform selected document's primary content to text
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
                
                // Find transformable primary content
                java.util.List<com.docmgmt.model.Content> transformableContents = reloadedDoc.getContents().stream()
                    .filter(c -> c.isPrimary() && transformerRegistry.findTransformer(c).isPresent())
                    .toList();
                
                if (transformableContents.isEmpty()) {
                    Notification.show("No transformable content found in this document", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                
                // Transform each content
                int success = 0;
                int failed = 0;
                StringBuilder errors = new StringBuilder();
                
                for (com.docmgmt.model.Content content : transformableContents) {
                    try {
                        contentService.transformAndAddRendition(content.getId(), "text/plain");
                        success++;
                    } catch (Exception e) {
                        logger.warn("Failed to transform content {} ({}) in document {}: {}", 
                            content.getId(), content.getContentType(), document.getName(), e.getMessage());
                        failed++;
                        if (errors.length() < 200) { // Limit error message length
                            errors.append("\n- ").append(e.getMessage());
                        }
                    }
                }
                
                if (success > 0) {
                    Notification.show("Transformed " + success + " content item(s) to text", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                
                if (failed > 0) {
                    String message = "Failed to transform " + failed + " content item(s)";
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
     * Transform all transformable content in the current folder and all subfolders recursively
     */
    private void transformFolderRecursively() {
        if (currentFolder == null) {
            return;
        }
        
        // Confirm with user
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Transform Folder Recursively");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Span("This will transform all transformable content in \"" + currentFolder.getName() + 
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
     * Recursively transform all transformable content in a folder and its subfolders
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
                        Notification.show("Successfully transformed " + counts[0] + " content item(s) to text" +
                            (counts[1] > 0 ? " (" + counts[1] + " failed)" : ""), 
                            5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } else if (counts[1] > 0) {
                        Notification.show("Failed to transform " + counts[1] + " content item(s)", 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    } else {
                        Notification.show("No transformable content found", 
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
     * Helper method to recursively transform content in a folder
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
                        
                        // Find and transform any transformable content
                        for (com.docmgmt.model.Content content : doc.getContents()) {
                            if (content.isPrimary() && transformerRegistry.findTransformer(content).isPresent()) {
                                try {
                                    contentService.transformAndAddRendition(content.getId(), "text/plain");
                                    counts[0]++;
                                    logger.info("Transformed {} content in document: {}", 
                                        content.getContentType(), doc.getName());
                                } catch (Exception e) {
                                    counts[1]++;
                                    logger.warn("Failed to transform {} content in document {}: {}", 
                                        content.getContentType(), doc.getName(), e.getMessage());
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
     * Batch extract and apply AI fields for selected documents
     */
    private void batchExtractFields() {
        List<Long> documentIds = itemsGrid.getSelectedItems().stream()
            .filter(item -> item instanceof Document)
            .map(item -> ((Document) item).getId())
            .collect(Collectors.toList());
        
        if (documentIds.isEmpty()) {
            Notification.show("No documents selected", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        
        // Show progress notification
        Notification.show("Extracting fields for " + documentIds.size() + " document(s)...  This may take a while.", 
            3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        
        // Process in background
        CompletableFuture.supplyAsync(() -> {
            return fieldExtractionService.extractAndApplyFieldsForDocuments(documentIds);
        }).thenAccept(results -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                long successCount = results.values().stream()
                    .filter(result -> result.equals("Success"))
                    .count();
                long skippedCount = results.values().stream()
                    .filter(result -> result.startsWith("Skipped:"))
                    .count();
                long failedCount = results.values().stream()
                    .filter(result -> result.startsWith("Failed:"))
                    .count();
                
                String message;
                if (successCount > 0 && failedCount == 0 && skippedCount == 0) {
                    message = "Successfully extracted and applied fields for " + successCount + " document(s)";
                    Notification.show(message, 5000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else if (successCount > 0) {
                    message = "Extracted fields for " + successCount + " document(s)";
                    if (skippedCount > 0) {
                        message += ", " + skippedCount + " skipped (no text content)";
                    }
                    if (failedCount > 0) {
                        message += ", " + failedCount + " failed";
                    }
                    Notification.show(message, 5000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                } else {
                    if (skippedCount > 0 && failedCount == 0) {
                        message = "All " + skippedCount + " document(s) skipped (no text content available)";
                        Notification.show(message, 5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                    } else {
                        message = "Failed to extract fields";
                        if (skippedCount > 0) {
                            message += ": " + skippedCount + " skipped, ";
                        }
                        if (failedCount > 0) {
                            message += failedCount + " failed";
                        }
                        Notification.show(message, 5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
                
                // Refresh the grid
                refreshFolderContents();
                ui.push();
            }));
        }).exceptionally(ex -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Error during batch extraction: " + ex.getMessage(), 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ui.push();
            }));
            return null;
        });
    }
    
    /**
     * Open import from directory dialog
     */
    private void openImportDirectoryDialog() {
        if (currentFolder == null) {
            return;
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("80vh");
        
        H2 title = new H2("Import from Server Directory");
        
        Span helpText = new Span("Browse and select a directory or files to import. " +
            "The folder hierarchy will be recreated under: " + currentFolder.getName());
        helpText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        // Current path display and navigation
        TextField currentPathField = new TextField("Current Path");
        currentPathField.setWidthFull();
        currentPathField.setReadOnly(true);
        
        // File browser grid
        Grid<File> fileGrid = new Grid<>();
        fileGrid.setHeight("400px");
        fileGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        
        fileGrid.addColumn(file -> file.isDirectory() ? "📁 " + file.getName() : "📄 " + file.getName())
            .setHeader("Name")
            .setAutoWidth(true)
            .setFlexGrow(1);
        
        fileGrid.addColumn(file -> {
            if (file.isDirectory()) {
                return "Directory";
            }
            long bytes = file.length();
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        })
            .setHeader("Size")
            .setAutoWidth(true);
        
        // Track current directory
        File[] currentDirectory = new File[] { new File(System.getProperty("user.home")) };
        
        // Function to refresh file list
        Runnable refreshFileList = () -> {
            File dir = currentDirectory[0];
            currentPathField.setValue(dir.getAbsolutePath());
            
            File[] files = dir.listFiles();
            if (files != null) {
                java.util.Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                fileGrid.setItems(files);
            } else {
                fileGrid.setItems();
            }
        };
        
        refreshFileList.run();
        
        // Navigation buttons
        Button parentButton = new Button("Parent Directory", e -> {
            File parent = currentDirectory[0].getParentFile();
            if (parent != null && parent.exists()) {
                currentDirectory[0] = parent;
                refreshFileList.run();
            }
        });
        parentButton.setIcon(new Icon(VaadinIcon.ARROW_UP));
        
        Button homeButton = new Button("Home", e -> {
            currentDirectory[0] = new File(System.getProperty("user.home"));
            refreshFileList.run();
        });
        homeButton.setIcon(new Icon(VaadinIcon.HOME));
        
        Button rootButton = new Button("Root", e -> {
            currentDirectory[0] = new File("/");
            refreshFileList.run();
        });
        rootButton.setIcon(new Icon(VaadinIcon.FOLDER_O));
        
        HorizontalLayout navButtons = new HorizontalLayout(parentButton, homeButton, rootButton);
        navButtons.setSpacing(true);
        
        // Double-click to navigate into directories
        fileGrid.addItemDoubleClickListener(event -> {
            File file = event.getItem();
            if (file.isDirectory()) {
                currentDirectory[0] = file;
                refreshFileList.run();
                fileGrid.deselectAll();
            }
        });
        
        // Selection info
        Span selectionInfo = new Span("No items selected");
        selectionInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        fileGrid.addSelectionListener(event -> {
            int count = event.getAllSelectedItems().size();
            if (count == 0) {
                selectionInfo.setText("No items selected");
            } else if (count == 1) {
                File selected = event.getAllSelectedItems().iterator().next();
                selectionInfo.setText("Selected: " + selected.getName() + 
                    (selected.isDirectory() ? " (directory)" : " (file)"));
            } else {
                selectionInfo.setText(count + " items selected");
            }
        });
        
        // Storage location selection
        RadioButtonGroup<String> storageType = new RadioButtonGroup<>();
        storageType.setLabel("Storage Location");
        storageType.setItems("Database", "File Store");
        storageType.setValue("Database");
        
        ComboBox<FileStore> fileStoreCombo = new ComboBox<>("Select File Store");
        fileStoreCombo.setItems(fileStoreService.findAll());
        fileStoreCombo.setItemLabelGenerator(FileStore::getName);
        fileStoreCombo.setVisible(false);
        fileStoreCombo.setWidthFull();
        
        storageType.addValueChangeListener(e -> {
            boolean isFileStore = "File Store".equals(e.getValue());
            fileStoreCombo.setVisible(isFileStore);
            if (isFileStore && fileStoreCombo.getValue() == null) {
                fileStoreCombo.getListDataView().getItems().findFirst().ifPresent(fileStoreCombo::setValue);
            }
        });
        
        // Import options
        Checkbox generateTextRenditions = new Checkbox("Generate text renditions for PDFs", true);
        Checkbox indexDocuments = new Checkbox("Index documents in search", true);
        Checkbox extractFields = new Checkbox("Apply AI field extraction", true);
        
        VerticalLayout optionsLayout = new VerticalLayout(
            storageType, fileStoreCombo, new Hr(),
            generateTextRenditions, indexDocuments, extractFields);
        optionsLayout.setPadding(false);
        optionsLayout.setSpacing(false);
        
        // Buttons
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button importButton = new Button("Import Selected", e -> {
            java.util.Set<File> selectedFiles = fileGrid.getSelectedItems();
            if (selectedFiles.isEmpty()) {
                Notification.show("Please select at least one file or directory to import", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Validate file store selection if needed
            boolean useFileStore = "File Store".equals(storageType.getValue());
            FileStore selectedFileStore = fileStoreCombo.getValue();
            if (useFileStore && selectedFileStore == null) {
                Notification.show("Please select a file store", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            dialog.close();
            
            // Import each selected item
            for (File file : selectedFiles) {
                if (file.isDirectory()) {
                    performImport(file.getAbsolutePath(), 
                        generateTextRenditions.getValue(),
                        indexDocuments.getValue(),
                        extractFields.getValue(),
                        selectedFileStore);
                } else {
                    // Import single file
                    performSingleFileImport(file,
                        generateTextRenditions.getValue(),
                        indexDocuments.getValue(),
                        extractFields.getValue(),
                        selectedFileStore);
                }
            }
        });
        importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, importButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title, helpText, new Hr(),
            currentPathField, navButtons,
            fileGrid, selectionInfo,
            new Hr(), optionsLayout, new Hr(), buttonLayout);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setFlexGrow(1, fileGrid);
        
        dialog.add(layout);
        dialog.open();
    }
    
    /**
     * Perform the import of a single file
     */
    private void performSingleFileImport(File file, boolean generateTextRenditions, 
                                        boolean indexDocuments, boolean extractFields, FileStore fileStore) {
        Notification.show("Importing file: " + file.getName() + "...", 
            3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        
        // Process in background
        CompletableFuture.supplyAsync(() -> {
            try {
                importFile(file, currentFolder, generateTextRenditions, indexDocuments, extractFields, fileStore);
                return true;
            } catch (Exception e) {
                logger.error("Failed to import file {}: {}", file.getName(), e.getMessage());
                return false;
            }
        }).thenAccept(success -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                if (success) {
                    Notification.show("Successfully imported: " + file.getName(), 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Failed to import: " + file.getName(), 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                refreshFolderContents();
                ui.push();
            }));
        }).exceptionally(ex -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Import failed: " + ex.getMessage(), 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ui.push();
            }));
            return null;
        });
    }
    
    /**
     * Perform the import from directory
     */
    private void performImport(String directoryPath, boolean generateTextRenditions, 
                              boolean indexDocuments, boolean extractFields, FileStore fileStore) {
        Notification.show("Starting import from: " + directoryPath + "...\nThis may take a while.", 
            3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        
        // Process in background
        CompletableFuture.supplyAsync(() -> {
            return importDirectory(new File(directoryPath), currentFolder, 
                generateTextRenditions, indexDocuments, extractFields, fileStore);
        }).thenAccept(stats -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                String message = String.format(
                    "Import complete: %d documents imported, %d folders created, %d failed",
                    stats[0], stats[1], stats[2]);
                Notification.show(message, 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(stats[2] == 0 ? 
                        NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_CONTRAST);
                refreshFolderTree();
                refreshFolderContents();
                ui.push();
            }));
        }).exceptionally(ex -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Import failed: " + ex.getMessage(), 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ui.push();
            }));
            return null;
        });
    }
    
    /**
     * Recursively import directory structure
     * @return Array of [documentsImported, foldersCreated, failed]
     */
    private int[] importDirectory(File dir, Folder parentFolder, 
                                  boolean generateTextRenditions, 
                                  boolean indexDocuments, 
                                  boolean extractFields,
                                  FileStore fileStore) {
        int[] stats = new int[3]; // [documents, folders, failed]
        
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("Directory does not exist or is not a directory: {}", dir.getAbsolutePath());
            return stats;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return stats;
        }
        
        // First pass: create subdirectories
        Map<File, Folder> dirToFolderMap = new HashMap<>();
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    Folder subfolder = Folder.builder()
                        .name(file.getName())
                        .build();
                    subfolder.setDescription("Imported from: " + file.getAbsolutePath());
                    subfolder.setParentFolder(parentFolder);
                    subfolder = folderService.save(subfolder);
                    parentFolder.addChildFolder(subfolder);
                    dirToFolderMap.put(file, subfolder);
                    stats[1]++; // folders created
                    logger.info("Created folder: {}", file.getName());
                } catch (Exception e) {
                    logger.error("Failed to create folder {}: {}", file.getName(), e.getMessage());
                    stats[2]++;
                }
            }
        }
        
        // Second pass: import files
        for (File file : files) {
            if (file.isFile()) {
                try {
                    importFile(file, parentFolder, generateTextRenditions, indexDocuments, extractFields, fileStore);
                    stats[0]++; // documents imported
                    logger.info("Imported document: {}", file.getName());
                } catch (Exception e) {
                    logger.error("Failed to import file {}: {}", file.getName(), e.getMessage());
                    stats[2]++;
                }
            }
        }
        
        // Third pass: recursively process subdirectories
        for (Map.Entry<File, Folder> entry : dirToFolderMap.entrySet()) {
            int[] subStats = importDirectory(entry.getKey(), entry.getValue(), 
                generateTextRenditions, indexDocuments, extractFields, fileStore);
            stats[0] += subStats[0];
            stats[1] += subStats[1];
            stats[2] += subStats[2];
        }
        
        return stats;
    }
    
    /**
     * Generate a unique hierarchical storage path for a file
     */
    private String generateStoragePath(String originalFilename) {
        String uuid = java.util.UUID.randomUUID().toString();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        
        // Remove hyphens from UUID for easier splitting
        String uuidNoDashes = uuid.replace("-", "");
        
        // Create hierarchical path: split into 4 levels of 2 characters each
        String level1 = uuidNoDashes.substring(0, 2);
        String level2 = uuidNoDashes.substring(2, 4);
        String level3 = uuidNoDashes.substring(4, 6);
        String level4 = uuidNoDashes.substring(6, 8);
        
        // Construct path: dir1/dir2/dir3/dir4/originalUUID.ext
        return String.format("%s/%s/%s/%s/%s%s", 
            level1, level2, level3, level4, uuid, extension);
    }
    
    /**
     * Import a single file as a document
     */
    private void importFile(File file, Folder parentFolder,
                           boolean generateTextRenditions, 
                           boolean indexDocuments, 
                           boolean extractFields,
                           FileStore fileStore) throws Exception {
        // Read file bytes
        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        
        // Determine content type
        String contentType = java.nio.file.Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        // Create document (using Article as default)
        Document document = Article.builder().build();
        document.setName(file.getName());
        document.setDescription("Imported from: " + file.getAbsolutePath());
        document.setDocumentType(DocumentType.OTHER);
        document = documentService.save(document);
        
        // Create primary content
        Content content;
        Content.ContentBuilder contentBuilder = Content.builder()
            .name(file.getName())
            .contentType(contentType)
            .isPrimary(true)
            .isIndexable(true)
            .sysObject(document);
        
        // Set storage location based on fileStore parameter
        if (fileStore != null) {
            // Generate unique storage path for file store
            String storagePath = generateStoragePath(file.getName());
            contentBuilder.fileStore(fileStore).storagePath(storagePath);
            
            // Save content entity first
            content = contentService.save(contentBuilder.build());
            
            // Then store the file bytes
            content.setContentBytes(fileBytes);
        } else {
            // Store in database
            contentBuilder.content(fileBytes);
            content = contentService.save(contentBuilder.build());
        }
        
        // Add document to folder - reload both entities to ensure clean managed state
        Document managedDocument = documentService.findById(document.getId());
        Folder managedFolder = folderService.findById(parentFolder.getId());
        if (!managedFolder.getItems().contains(managedDocument)) {
            managedFolder.addItem(managedDocument);
            folderService.save(managedFolder);
        }
        
        // Generate text rendition if requested and it's a PDF
        if (generateTextRenditions && "application/pdf".equals(contentType)) {
            try {
                contentService.transformAndAddRendition(content.getId(), "text/plain");
                logger.info("Generated text rendition for: {}", file.getName());
            } catch (Exception e) {
                logger.warn("Failed to generate text rendition for {}: {}", file.getName(), e.getMessage());
            }
        }
        
        // Index document if requested
        if (indexDocuments) {
            try {
                luceneIndexService.indexDocument(document);
                logger.info("Indexed document: {}", file.getName());
            } catch (Exception e) {
                logger.warn("Failed to index {}: {}", file.getName(), e.getMessage());
            }
        }
        
        // Extract fields if requested
        if (extractFields) {
            try {
                Map<Long, String> result = fieldExtractionService.extractAndApplyFieldsForDocuments(
                    Collections.singletonList(document.getId()));
                if (result.get(document.getId()).equals("Success")) {
                    logger.info("Extracted fields for: {}", file.getName());
                }
            } catch (Exception e) {
                logger.warn("Failed to extract fields for {}: {}", file.getName(), e.getMessage());
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
        
        // Content name field
        TextField contentNameField = new TextField("Content Name");
        contentNameField.setWidthFull();
        contentNameField.setPlaceholder("Enter content name (leave empty to use filename)");
        
        // Rendition type selection
        RadioButtonGroup<String> renditionType = new RadioButtonGroup<>();
        renditionType.setLabel("Rendition Type");
        renditionType.setItems("Primary", "Secondary");
        renditionType.setValue("Primary");
        
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
                String originalFileName = buffer.getFileName();
                // Use custom content name if provided, otherwise use original filename
                String finalContentName = contentNameField.isEmpty() ? 
                    originalFileName : contentNameField.getValue();
                String contentType = buffer.getFileData().getMimeType();
                
                MultipartFile multipartFile = new MultipartFile() {
                    @Override
                    public String getName() { return finalContentName; }
                    
                    @Override
                    public String getOriginalFilename() { return finalContentName; }
                    
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
                
                // Set rendition type
                boolean isPrimary = "Primary".equals(renditionType.getValue());
                content.setPrimary(isPrimary);
                contentService.save(content);
                
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
        formLayout.add(docInfo, contentNameField, renditionType, storageType, fileStoreCombo, upload, uploadStatus);
        formLayout.setColspan(docInfo, 2);
        formLayout.setColspan(contentNameField, 2);
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
     * Open field extraction dialog for AI-powered field suggestions
     */
    private void openFieldExtractionDialog(Document document) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
        dialog.setHeight("80vh");
        
        H2 title = new H2("AI Field Extraction: " + document.getName());
        
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setSizeFull();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        Span loadingText = new Span("Analyzing document content with AI...");
        loadingText.getStyle().set("font-size", "var(--lumo-font-size-l)");
        loadingLayout.add(loadingText);
        
        dialog.add(loadingLayout);
        dialog.open();
        
        // Perform extraction asynchronously using CompletableFuture
        CompletableFuture.supplyAsync(() -> {
            try {
                return fieldExtractionService.extractFieldsFromDocument(document.getId(), null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(suggestions -> {
            // Update UI on UI thread
            getUI().ifPresent(ui -> ui.access(() -> {
                dialog.removeAll();
                showFieldSuggestions(dialog, document, suggestions);
                ui.push(); // Push the updates to the client
            }));
        }).exceptionally(ex -> {
            logger.error("Failed to extract fields: {}", ex.getMessage(), ex);
            getUI().ifPresent(ui -> ui.access(() -> {
                dialog.close();
                String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                Notification.show("Failed to extract fields: " + errorMsg, 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ui.push(); // Push the updates to the client
            }));
            return null;
        });
    }
    
    /**
     * Show field suggestions in dialog
     */
    private void showFieldSuggestions(Dialog dialog, Document document, FieldSuggestionDTO suggestions) {
        H2 title = new H2("Field Suggestions: " + document.getName());
        
        Span helpText = new Span("Select which AI-suggested fields to apply to the document:");
        helpText.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-m)")
            .set("margin-bottom", "10px");
        
        // Create comparison grid
        VerticalLayout comparisonLayout = new VerticalLayout();
        comparisonLayout.setPadding(false);
        comparisonLayout.setSpacing(true);
        comparisonLayout.getStyle().set("overflow-y", "auto");
        
        Map<String, Checkbox> checkboxMap = new HashMap<>();
        
        // Description
        checkboxMap.put("description", addFieldComparison(
            comparisonLayout, 
            "Description", 
            suggestions.getCurrentFields().getDescription(),
            suggestions.getSuggestedFields().getDescription()
        ));
        
        // Keywords
        checkboxMap.put("keywords", addFieldComparison(
            comparisonLayout, 
            "Keywords", 
            suggestions.getCurrentFields().getKeywords(),
            suggestions.getSuggestedFields().getKeywords()
        ));
        
        // Tags
        String currentTags = suggestions.getCurrentFields().getTags() != null && !suggestions.getCurrentFields().getTags().isEmpty()
            ? String.join(", ", suggestions.getCurrentFields().getTags())
            : "(none)";
        String suggestedTags = suggestions.getSuggestedFields().getTags() != null && !suggestions.getSuggestedFields().getTags().isEmpty()
            ? String.join(", ", suggestions.getSuggestedFields().getTags())
            : "(none)";
        checkboxMap.put("tags", addFieldComparison(
            comparisonLayout, 
            "Tags", 
            currentTags,
            suggestedTags
        ));
        
        // Document Type
        String currentType = suggestions.getCurrentFields().getDocumentType() != null
            ? suggestions.getCurrentFields().getDocumentType().toString()
            : "(none)";
        String suggestedType = suggestions.getSuggestedFields().getDocumentType() != null
            ? suggestions.getSuggestedFields().getDocumentType().toString()
            : "(none)";
        checkboxMap.put("documentType", addFieldComparison(
            comparisonLayout, 
            "Document Type", 
            currentType,
            suggestedType
        ));
        
        // Add type-specific fields
        if (suggestions.getSuggestedFields().getTypeSpecificFields() != null && 
            !suggestions.getSuggestedFields().getTypeSpecificFields().isEmpty()) {
            
            // Add separator
            H3 typeSpecificHeader = new H3("Type-Specific Fields");
            typeSpecificHeader.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
            comparisonLayout.add(typeSpecificHeader);
            
            // Display each type-specific field
            suggestions.getSuggestedFields().getTypeSpecificFields().forEach((fieldName, suggestedValue) -> {
                Object currentValue = null;
                if (suggestions.getCurrentFields().getTypeSpecificFields() != null) {
                    currentValue = suggestions.getCurrentFields().getTypeSpecificFields().get(fieldName);
                }
                
                String currentStr = formatFieldValue(currentValue);
                String suggestedStr = formatFieldValue(suggestedValue);
                
                checkboxMap.put(fieldName, addFieldComparison(
                    comparisonLayout,
                    formatFieldName(fieldName),
                    currentStr,
                    suggestedStr
                ));
            });
        }
        
        // Buttons
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        Button applyButton = new Button("Apply Selected", new Icon(VaadinIcon.CHECK), e -> {
            try {
                // Build map of selected fields
                Map<String, Boolean> fieldsToApply = new HashMap<>();
                checkboxMap.forEach((field, checkbox) -> {
                    fieldsToApply.put(field, checkbox.getValue());
                });
                
                // Apply suggestions
                fieldExtractionService.applyFieldSuggestions(
                    document.getId(),
                    fieldsToApply,
                    suggestions.getSuggestedFields()
                );
                
                Notification.show("Fields applied successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                dialog.close();
                refreshFolderContents();
                
            } catch (Exception ex) {
                Notification.show("Failed to apply fields: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, applyButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title, helpText, new Hr(), comparisonLayout, new Hr(), buttons
        );
        layout.setPadding(true);
        layout.setSpacing(true);
        
        dialog.add(layout);
    }
    
    /**
     * Add a field comparison row with checkbox
     */
    private Checkbox addFieldComparison(VerticalLayout container, String fieldName, 
                                       String currentValue, String suggestedValue) {
        VerticalLayout fieldLayout = new VerticalLayout();
        fieldLayout.setPadding(false);
        fieldLayout.setSpacing(false);
        fieldLayout.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "4px")
            .set("padding", "10px")
            .set("background-color", "var(--lumo-contrast-5pct)");
        
        Checkbox checkbox = new Checkbox(fieldName);
        checkbox.getStyle().set("font-weight", "bold");
        
        // Current value
        HorizontalLayout currentRow = new HorizontalLayout();
        currentRow.setSpacing(true);
        Span currentLabel = new Span("Current:");
        currentLabel.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-weight", "600")
            .set("min-width", "100px");
        Span currentText = new Span(currentValue != null && !currentValue.isEmpty() ? currentValue : "(empty)");
        currentText.getStyle()
            .set("font-style", currentValue == null || currentValue.isEmpty() ? "italic" : "normal")
            .set("color", currentValue == null || currentValue.isEmpty() ? "var(--lumo-disabled-text-color)" : "var(--lumo-body-text-color)");
        currentRow.add(currentLabel, currentText);
        
        // Suggested value
        HorizontalLayout suggestedRow = new HorizontalLayout();
        suggestedRow.setSpacing(true);
        Span suggestedLabel = new Span("Suggested:");
        suggestedLabel.getStyle()
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-weight", "600")
            .set("min-width", "100px");
        Span suggestedText = new Span(suggestedValue != null && !suggestedValue.isEmpty() ? suggestedValue : "(empty)");
        suggestedText.getStyle()
            .set("font-style", suggestedValue == null || suggestedValue.isEmpty() ? "italic" : "normal")
            .set("color", suggestedValue == null || suggestedValue.isEmpty() ? "var(--lumo-disabled-text-color)" : "var(--lumo-success-color)")
            .set("font-weight", "500");
        suggestedRow.add(suggestedLabel, suggestedText);
        
        fieldLayout.add(checkbox, currentRow, suggestedRow);
        container.add(fieldLayout);
        
        return checkbox;
    }
    
    /**
     * Format field name from camelCase to Title Case
     */
    private String formatFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        // Convert camelCase to Title Case with spaces
        return fieldName.replaceAll("([A-Z])", " $1")
                        .replaceAll("([a-z])([A-Z])", "$1 $2")
                        .trim()
                        .substring(0, 1).toUpperCase() + 
               fieldName.replaceAll("([A-Z])", " $1")
                        .replaceAll("([a-z])([A-Z])", "$1 $2")
                        .trim()
                        .substring(1);
    }
    
    /**
     * Format field value to display string
     */
    private String formatFieldValue(Object value) {
        if (value == null) {
            return "(empty)";
        }
        if (value instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) value;
            if (collection.isEmpty()) {
                return "(none)";
            }
            return collection.stream()
                           .map(Object::toString)
                           .collect(java.util.stream.Collectors.joining(", "));
        }
        if (value instanceof String) {
            String str = (String) value;
            return str.isEmpty() ? "(empty)" : str;
        }
        return value.toString();
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
    
    // Note: openPluginDialog is now handled by DocumentDetailDialog in view mode
    // Stub for edit mode - plugins not available in edit mode
    private void openPluginDialog(Document document, PluginInfoDTO pluginInfo) {
        Notification.show("AI Plugins are not available in edit mode. Save changes and reopen in view mode.",
            3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }
    
    /**
     * Open translate dialog for a document (DEPRECATED - use openPluginDialog instead)
     */
    private void openTranslateDialog(Document document) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
        dialog.setHeight("80vh");
        
        H2 title = new H2("Translate: " + document.getName());
        
        // Target language selection
        ComboBox<String> targetLanguageCombo = new ComboBox<>("Target Language");
        targetLanguageCombo.setItems(
            "English", "Spanish", "French", "German", "Italian", 
            "Portuguese", "Russian", "Chinese", "Japanese", "Korean",
            "Arabic", "Hindi"
        );
        targetLanguageCombo.setValue("English");
        targetLanguageCombo.setWidthFull();
        
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setSizeFull();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setVisible(false);
        
        Span loadingText = new Span("Translating document...");
        loadingText.getStyle().set("font-size", "var(--lumo-font-size-l)");
        loadingLayout.add(loadingText);
        
        // Results area
        VerticalLayout resultsLayout = new VerticalLayout();
        resultsLayout.setPadding(false);
        resultsLayout.setSpacing(true);
        resultsLayout.setVisible(false);
        
        Button translateButton = new Button("Translate", new Icon(VaadinIcon.GLOBE), e -> {
            String targetLanguage = targetLanguageCombo.getValue();
            if (targetLanguage == null) {
                Notification.show("Please select a target language", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Show loading
            resultsLayout.setVisible(false);
            loadingLayout.setVisible(true);
            
            // Execute translation in background
            CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> params = Map.of("targetLanguage", targetLanguage);
                    return pluginService.executePlugin(document.getId(), "translate", params);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).thenAccept(response -> {
                // Update UI on UI thread
                getUI().ifPresent(ui -> ui.access(() -> {
                    loadingLayout.setVisible(false);
                    
                    if (response.getStatus() == PluginResponse.PluginStatus.SUCCESS) {
                        showTranslationResults(resultsLayout, response);
                        resultsLayout.setVisible(true);
                    } else {
                        Notification.show("Translation failed: " + response.getErrorMessage(), 
                            5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                    ui.push();
                }));
            }).exceptionally(ex -> {
                logger.error("Translation failed: {}", ex.getMessage(), ex);
                getUI().ifPresent(ui -> ui.access(() -> {
                    loadingLayout.setVisible(false);
                    String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    Notification.show("Translation failed: " + errorMsg, 
                        5000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    ui.push();
                }));
                return null;
            });
        });
        translateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button closeButton = new Button("Close", e -> dialog.close());
        
        HorizontalLayout buttons = new HorizontalLayout(translateButton, closeButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title, new Hr(), targetLanguageCombo, loadingLayout, resultsLayout, new Hr(), buttons
        );
        layout.setPadding(true);
        layout.setSpacing(true);
        
        dialog.add(layout);
        dialog.open();
    }
    
    /**
     * Show translation results in the layout
     */
    private void showTranslationResults(VerticalLayout container, PluginResponse response) {
        container.removeAll();
        
        // Language info
        String originalLang = response.getData("originalLanguage");
        String originalLangName = response.getData("originalLanguageName");
        String targetLang = response.getData("targetLanguage");
        String targetLangName = response.getData("targetLanguageName");
        Boolean truncated = response.getData("truncated", false);
        
        HorizontalLayout languageInfo = new HorizontalLayout();
        languageInfo.setSpacing(true);
        languageInfo.setWidthFull();
        
        Span sourceInfo = new Span("Source: " + originalLangName + " (" + originalLang + ")");
        sourceInfo.getStyle()
            .set("font-weight", "bold")
            .set("color", "var(--lumo-primary-text-color)");
        
        Icon arrowIcon = new Icon(VaadinIcon.ARROW_RIGHT);
        arrowIcon.setColor("var(--lumo-contrast-60pct)");
        
        Span targetInfo = new Span("Target: " + targetLangName + " (" + targetLang + ")");
        targetInfo.getStyle()
            .set("font-weight", "bold")
            .set("color", "var(--lumo-success-text-color)");
        
        languageInfo.add(sourceInfo, arrowIcon, targetInfo);
        
        if (truncated) {
            Span truncatedWarning = new Span("⚠ Content was truncated to 4000 characters for translation");
            truncatedWarning.getStyle()
                .set("color", "var(--lumo-warning-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-style", "italic");
            container.add(truncatedWarning);
        }
        
        container.add(languageInfo);
        
        // Original content
        H3 originalTitle = new H3("Original Content");
        originalTitle.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
        
        String originalContent = response.getData("originalContent");
        TextArea originalArea = new TextArea();
        originalArea.setValue(originalContent != null ? originalContent : "");
        originalArea.setWidthFull();
        originalArea.setHeight("200px");
        originalArea.setReadOnly(true);
        originalArea.getStyle().set("font-family", "monospace");
        
        // Translated content
        H3 translatedTitle = new H3("Translated Content");
        translatedTitle.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
        
        String translatedContent = response.getData("translatedContent");
        TextArea translatedArea = new TextArea();
        translatedArea.setValue(translatedContent != null ? translatedContent : "");
        translatedArea.setWidthFull();
        translatedArea.setHeight("200px");
        translatedArea.setReadOnly(true);
        translatedArea.getStyle()
            .set("font-family", "monospace")
            .set("background-color", "var(--lumo-success-color-10pct)");
        
        container.add(originalTitle, originalArea, translatedTitle, translatedArea);
    }
    
    /**
     * Simple markdown to HTML converter for basic rendering
     * Handles: headers, bold, italic, links, lists, code blocks
     */
    private String renderMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        boolean inList = false;
        
        for (String line : lines) {
            // Code blocks
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</code></pre>");
                    inCodeBlock = false;
                } else {
                    html.append("<pre style='background-color: var(--lumo-contrast-10pct); padding: 10px; border-radius: 4px; overflow-x: auto;'><code>");
                    inCodeBlock = true;
                }
                continue;
            }
            
            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }
            
            // Close list if needed
            if (inList && !line.trim().startsWith("-") && !line.trim().startsWith("*") && !line.trim().isEmpty()) {
                html.append("</ul>");
                inList = false;
            }
            
            // Headers (process inline markdown in headers)
            if (line.startsWith("# ")) {
                html.append("<h1 style='margin-top: 20px; margin-bottom: 10px;'>").append(processInlineMarkdown(line.substring(2))).append("</h1>");
            } else if (line.startsWith("## ")) {
                html.append("<h2 style='margin-top: 16px; margin-bottom: 8px;'>").append(processInlineMarkdown(line.substring(3))).append("</h2>");
            } else if (line.startsWith("### ")) {
                html.append("<h3 style='margin-top: 12px; margin-bottom: 6px;'>").append(processInlineMarkdown(line.substring(4))).append("</h3>");
            }
            // Horizontal rule
            else if (line.trim().equals("---") || line.trim().equals("***")) {
                html.append("<hr style='border: none; border-top: 1px solid var(--lumo-contrast-20pct); margin: 16px 0;'>");
            }
            // Lists
            else if (line.trim().startsWith("-") || line.trim().startsWith("*")) {
                if (!inList) {
                    html.append("<ul style='margin: 10px 0; padding-left: 20px;'>");
                    inList = true;
                }
                String listItem = line.trim().substring(1).trim();
                html.append("<li>").append(processInlineMarkdown(listItem)).append("</li>");
            }
            // Empty line
            else if (line.trim().isEmpty()) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<br>");
            }
            // Regular paragraph
            else {
                html.append("<p style='margin: 8px 0;'>").append(processInlineMarkdown(line)).append("</p>");
            }
        }
        
        // Close any open tags
        if (inCodeBlock) {
            html.append("</code></pre>");
        }
        if (inList) {
            html.append("</ul>");
        }
        
        return html.toString();
    }
    
    /**
     * Process inline markdown (bold, italic, code, links)
     */
    private String processInlineMarkdown(String text) {
        if (text == null) return "";
        
        // First escape HTML to prevent XSS
        text = escapeHtml(text);
        
        // Now apply markdown transformations (working with escaped text)
        // Bold (**text** or __text__)
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("__(.+?)__", "<strong>$1</strong>");
        
        // Italic (*text* or _text_) - avoid matching ** for bold
        text = text.replaceAll("(?<!\\*)\\*([^*]+?)\\*(?!\\*)", "<em>$1</em>");
        text = text.replaceAll("(?<!_)_([^_]+?)_(?!_)", "<em>$1</em>");
        
        // Inline code (`code`)
        text = text.replaceAll("`([^`]+)`", "<code style='background-color: var(--lumo-contrast-10pct); padding: 2px 4px; border-radius: 3px; font-family: monospace;'>$1</code>");
        
        // Links [text](url)
        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href='$2' style='color: var(--lumo-primary-color);' target='_blank'>$1</a>");
        
        return text;
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Open dialog showing similar documents
     */
    private void openSimilarityDialog(Document document) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("600px");
        
        H2 title = new H2("Documents Similar to: " + document.getName());
        
        // Show loading indicator
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setSizeFull();
        Span loadingText = new Span("Preparing similarity search...");
        loadingLayout.add(loadingText);
        
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.add(loadingLayout);
        
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(closeButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(title, contentLayout, buttons);
        layout.setSizeFull();
        dialog.add(layout);
        dialog.open();
        
        // Generate embedding and fetch similar documents asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                // Reload document to ensure contents are loaded
                Document reloadedDoc = documentService.findById(document.getId());
                
                // Try to find similar documents
                List<DocumentSimilarityService.SimilarityResult> results = 
                    similarityService.findSimilar(reloadedDoc.getId(), 10);
                
                // If no results and no embedding exists, try to generate it
                if (results.isEmpty()) {
                    logger.info("No embedding found for document {}, attempting to generate...", reloadedDoc.getId());
                    
                    // Update UI to show we're generating
                    getUI().ifPresent(ui -> ui.access(() -> {
                        loadingText.setText("Generating embedding for this document...");
                    }));
                    
                    // Generate embedding
                    similarityService.generateEmbedding(reloadedDoc);
                    
                    // Try search again
                    getUI().ifPresent(ui -> ui.access(() -> {
                        loadingText.setText("Searching for similar documents...");
                    }));
                    
                    results = similarityService.findSimilar(reloadedDoc.getId(), 10);
                }
                
                return results;
            } catch (Exception e) {
                logger.error("Error finding similar documents", e);
                return Collections.<DocumentSimilarityService.SimilarityResult>emptyList();
            }
        }).thenAccept(results -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                contentLayout.removeAll();
                
                if (results.isEmpty()) {
                    VerticalLayout noResultsLayout = new VerticalLayout();
                    noResultsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                    noResultsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                    noResultsLayout.setPadding(true);
                    
                    Span noResults = new Span("No similar documents found.");
                    noResults.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("font-size", "var(--lumo-font-size-l)")
                        .set("margin-bottom", "10px");
                    
                    Span explanation = new Span("This could mean:");
                    explanation.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("margin-bottom", "5px");
                    
                    Span reason1 = new Span("• No other documents have embeddings yet");
                    reason1.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    
                    Span reason2 = new Span("• This document has no text content to analyze");
                    reason2.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    
                    Span reason3 = new Span("• Ollama service may not be running");
                    reason3.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    
                    noResultsLayout.add(noResults, explanation, reason1, reason2, reason3);
                    contentLayout.add(noResultsLayout);
                } else {
                    // Create grid to show results
                    Grid<DocumentSimilarityService.SimilarityResult> resultsGrid = 
                        new Grid<>(DocumentSimilarityService.SimilarityResult.class, false);
                    resultsGrid.setSizeFull();
                    
                    resultsGrid.addColumn(result -> result.getDocument().getName())
                        .setHeader("Document Name")
                        .setResizable(true)
                        .setAutoWidth(true)
                        .setFlexGrow(2);
                    
                    resultsGrid.addColumn(result -> result.getDocument().getDescription())
                        .setHeader("Description")
                        .setResizable(true)
                        .setAutoWidth(true)
                        .setFlexGrow(3);
                    
                    resultsGrid.addColumn(result -> String.format("%.2f%%", result.getSimilarity() * 100))
                        .setHeader("Similarity Score")
                        .setResizable(true)
                        .setAutoWidth(true)
                        .setFlexGrow(1);
                    
                    resultsGrid.setItems(results);
                    
                    // Add double-click to open document
                    resultsGrid.addItemDoubleClickListener(event -> {
                        dialog.close();
                        openDocumentDetailDialog(event.getItem().getDocument());
                    });
                    
                    Span hint = new Span("Double-click a document to view its details");
                    hint.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)") 
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("font-style", "italic");
                    
                    contentLayout.add(resultsGrid, hint);
                }
            }));
        });
    }
    
    /**
     * Open dialog to rebuild index for documents in the selected folder
     */
    private void openRebuildIndexDialog() {
        if (currentFolder == null) {
            return;
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        
        H2 title = new H2("Rebuild Index: " + currentFolder.getName());
        
        Span description = new Span(
            "This will rebuild the Lucene index and embeddings for all documents " +
            "in this folder (including subfolders).");
        description.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("margin-bottom", "10px");
        
        Checkbox includeSubfoldersCheckbox = new Checkbox("Include subfolders (recursive)", true);
        includeSubfoldersCheckbox.getStyle().set("margin-bottom", "10px");
        
        VerticalLayout progressLayout = new VerticalLayout();
        progressLayout.setVisible(false);
        progressLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        progressLayout.setPadding(true);
        
        Span progressText = new Span("Rebuilding index...");
        progressText.getStyle().set("margin-bottom", "10px");
        progressLayout.add(progressText);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        Button rebuildButton = new Button("Rebuild Index", new Icon(VaadinIcon.REFRESH), e -> {
            // Disable buttons during processing
            cancelButton.setEnabled(false);
            ((Button) e.getSource()).setEnabled(false);
            progressLayout.setVisible(true);
            
            boolean recursive = includeSubfoldersCheckbox.getValue();
            
            // Collect documents asynchronously
            CompletableFuture.supplyAsync(() -> {
                try {
                    List<Document> documents = new ArrayList<>();
                    collectDocumentsFromFolder(currentFolder, documents, recursive);
                    return documents;
                } catch (Exception ex) {
                    logger.error("Error collecting documents", ex);
                    return Collections.<Document>emptyList();
                }
            }).thenAccept(documents -> {
                if (documents.isEmpty()) {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        progressText.setText("No documents found in folder.");
                        cancelButton.setEnabled(true);
                    }));
                    return;
                }
                
                // Update progress
                getUI().ifPresent(ui -> ui.access(() -> {
                    progressText.setText(String.format("Rebuilding index for %d documents...", documents.size()));
                }));
                
                // Rebuild index
                try {
                    luceneIndexService.rebuildIndex(documents);
                    
                    getUI().ifPresent(ui -> ui.access(() -> {
                        progressText.setText(String.format("Index rebuilt successfully for %d documents!", documents.size()));
                        progressText.getStyle().set("color", "var(--lumo-success-color)");
                        
                        Notification.show(
                            String.format("Index rebuilt for %d documents", documents.size()),
                            3000,
                            Notification.Position.BOTTOM_START
                        ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        
                        // Close dialog after short delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                ui.access(dialog::close);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }));
                } catch (Exception ex) {
                    logger.error("Error rebuilding index", ex);
                    getUI().ifPresent(ui -> ui.access(() -> {
                        progressText.setText("Error: " + ex.getMessage());
                        progressText.getStyle().set("color", "var(--lumo-error-color)");
                        cancelButton.setEnabled(true);
                        
                        Notification.show(
                            "Failed to rebuild index: " + ex.getMessage(),
                            5000,
                            Notification.Position.BOTTOM_START
                        ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }));
                }
            });
        });
        rebuildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, rebuildButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title,
            new Hr(),
            description,
            includeSubfoldersCheckbox,
            progressLayout,
            buttons
        );
        layout.setPadding(true);
        layout.setSpacing(true);
        
        dialog.add(layout);
        dialog.open();
    }
    
    /**
     * Recursively collect all documents from a folder and its subfolders
     */
    private void collectDocumentsFromFolder(Folder folder, List<Document> documents, boolean recursive) {
        // Load folder with relationships
        Folder loadedFolder = folderService.findByIdWithRelationships(folder.getId());
        
        // Add documents from current folder
        if (loadedFolder.getItems() != null) {
            for (SysObject item : loadedFolder.getItems()) {
                if (item instanceof Document) {
                    documents.add((Document) item);
                }
            }
        }
        
        // Recursively process subfolders if requested
        if (recursive && loadedFolder.getChildFolders() != null) {
            for (Folder childFolder : loadedFolder.getChildFolders()) {
                collectDocumentsFromFolder(childFolder, documents, recursive);
            }
        }
    }
    
    /**
     * Open the tile configuration dialog for the current folder
     */
    private void openTileConfigurationDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("700px");
        
        H2 title = new H2("Tile Configuration: " + currentFolder.getName());
        
        Span helpText = new Span("Configure how documents are displayed as tiles in this folder.");
        helpText.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("margin-bottom", "1rem");
        
        // Get current configuration
        com.docmgmt.model.TileConfiguration config = tileService.getConfiguration(currentFolder.getId());
        
        // Group by subfolder
        Checkbox groupBySubfolderCheckbox = new Checkbox("Group by Subfolder");
        groupBySubfolderCheckbox.setValue(config.getGroupBySubfolder() != null ? config.getGroupBySubfolder() : false);
        groupBySubfolderCheckbox.getStyle().set("margin-bottom", "0.5rem");
        Span groupHelp = new Span("Organize tiles by their containing subfolder");
        groupHelp.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");
        
        // Visible fields
        TextField visibleFieldsField = new TextField("Visible Fields");
        visibleFieldsField.setValue(config.getVisibleFields() != null ? config.getVisibleFields() : "name,description,url");
        visibleFieldsField.setWidthFull();
        visibleFieldsField.setPlaceholder("name,description,url,documentType,tags");
        Span fieldsHelp = new Span("Comma-separated list of fields to display on each tile");
        fieldsHelp.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");
        
        // Color strategy
        ComboBox<com.docmgmt.model.TileConfiguration.ColorStrategy> colorStrategyCombo = 
            new ComboBox<>("Color Strategy");
        colorStrategyCombo.setItems(com.docmgmt.model.TileConfiguration.ColorStrategy.values());
        colorStrategyCombo.setValue(config.getColorStrategy());
        colorStrategyCombo.setWidthFull();
        Span colorHelp = new Span("How to color-code tiles (NONE, BY_FOLDER, BY_TYPE, BY_TAG, CUSTOM)");
        colorHelp.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");
        
        // Color mappings
        TextArea colorMappingsArea = new TextArea("Custom Color Mappings (JSON)");
        colorMappingsArea.setValue(config.getColorMappings() != null ? config.getColorMappings() : "");
        colorMappingsArea.setWidthFull();
        colorMappingsArea.setHeight("100px");
        colorMappingsArea.setPlaceholder("{\"ARTICLE\": \"#FF5733\", \"REPORT\": \"#33FF57\"}");
        Span mappingsHelp = new Span("JSON object mapping keys to hex colors (used with CUSTOM strategy)");
        mappingsHelp.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");
        
        // Tile size
        ComboBox<com.docmgmt.model.TileConfiguration.TileSize> tileSizeCombo = 
            new ComboBox<>("Tile Size");
        tileSizeCombo.setItems(com.docmgmt.model.TileConfiguration.TileSize.values());
        tileSizeCombo.setValue(config.getTileSize());
        tileSizeCombo.setWidthFull();
        
        // Sort order
        ComboBox<com.docmgmt.model.TileConfiguration.SortOrder> sortOrderCombo = 
            new ComboBox<>("Sort Order");
        sortOrderCombo.setItems(com.docmgmt.model.TileConfiguration.SortOrder.values());
        sortOrderCombo.setValue(config.getSortOrder());
        sortOrderCombo.setWidthFull();
        
        // Show links
        Checkbox showDetailLinkCheckbox = new Checkbox("Show Detail Link");
        showDetailLinkCheckbox.setValue(config.getShowDetailLink() != null ? config.getShowDetailLink() : true);
        
        Checkbox showUrlLinkCheckbox = new Checkbox("Show URL Link");
        showUrlLinkCheckbox.setValue(config.getShowUrlLink() != null ? config.getShowUrlLink() : true);
        
        Checkbox hideNavigationCheckbox = new Checkbox("Hide Navigation Panel");
        hideNavigationCheckbox.setValue(config.getHideNavigation() != null ? config.getHideNavigation() : false);
        
        Checkbox hideEditButtonsCheckbox = new Checkbox("Hide Edit Buttons");
        hideEditButtonsCheckbox.setValue(config.getHideEditButtons() != null ? config.getHideEditButtons() : false);
        
        com.vaadin.flow.component.textfield.NumberField backgroundOpacityField = 
            new com.vaadin.flow.component.textfield.NumberField("Background Color Opacity");
        backgroundOpacityField.setMin(0.0);
        backgroundOpacityField.setMax(1.0);
        backgroundOpacityField.setStep(0.05);
        backgroundOpacityField.setValue(config.getBackgroundColorOpacity() != null ? config.getBackgroundColorOpacity() : 0.05);
        backgroundOpacityField.setWidthFull();
        Span opacityHelp = new Span("0.0 = no background, 1.0 = full color (recommended: 0.03-0.10)");
        opacityHelp.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");
        
        HorizontalLayout checkboxesLayout = new HorizontalLayout(showDetailLinkCheckbox, showUrlLinkCheckbox, 
                                                                  hideNavigationCheckbox, hideEditButtonsCheckbox);
        checkboxesLayout.setSpacing(true);
        
        // Form layout
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        VerticalLayout groupSection = new VerticalLayout(groupBySubfolderCheckbox, groupHelp);
        groupSection.setPadding(false);
        groupSection.setSpacing(false);
        formLayout.add(groupSection, 2);
        
        VerticalLayout fieldsSection = new VerticalLayout(visibleFieldsField, fieldsHelp);
        fieldsSection.setPadding(false);
        fieldsSection.setSpacing(false);
        formLayout.add(fieldsSection, 2);
        
        VerticalLayout colorSection = new VerticalLayout(colorStrategyCombo, colorHelp);
        colorSection.setPadding(false);
        colorSection.setSpacing(false);
        formLayout.add(colorSection);
        
        formLayout.add(tileSizeCombo);
        formLayout.add(sortOrderCombo, 2);
        
        VerticalLayout mappingsSection = new VerticalLayout(colorMappingsArea, mappingsHelp);
        mappingsSection.setPadding(false);
        mappingsSection.setSpacing(false);
        formLayout.add(mappingsSection, 2);
        
        formLayout.add(checkboxesLayout, 2);
        
        VerticalLayout opacitySection = new VerticalLayout(backgroundOpacityField, opacityHelp);
        opacitySection.setPadding(false);
        opacitySection.setSpacing(false);
        formLayout.add(opacitySection, 2);
        
        // Buttons
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        Button saveButton = new Button("Save", e -> {
            try {
                com.docmgmt.dto.TileConfigurationDTO dto = com.docmgmt.dto.TileConfigurationDTO.builder()
                    .id(config.getId())
                    .folderId(currentFolder.getId())
                    .groupBySubfolder(groupBySubfolderCheckbox.getValue())
                    .visibleFields(visibleFieldsField.getValue())
                    .colorStrategy(colorStrategyCombo.getValue() != null ? colorStrategyCombo.getValue().name() : null)
                    .colorMappings(colorMappingsArea.getValue())
                    .tileSize(tileSizeCombo.getValue() != null ? tileSizeCombo.getValue().name() : null)
                    .showDetailLink(showDetailLinkCheckbox.getValue())
                    .showUrlLink(showUrlLinkCheckbox.getValue())
                    .sortOrder(sortOrderCombo.getValue() != null ? sortOrderCombo.getValue().name() : null)
                    .hideNavigation(hideNavigationCheckbox.getValue())
                    .hideEditButtons(hideEditButtonsCheckbox.getValue())
                    .backgroundColorOpacity(backgroundOpacityField.getValue())
                    .build();
                
                tileService.saveConfiguration(dto);
                
                Notification.show("Tile configuration saved successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                dialog.close();
                
            } catch (Exception ex) {
                logger.error("Error saving tile configuration", ex);
                Notification.show("Error saving configuration: " + ex.getMessage(), 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button previewButton = new Button("Save & Preview", new Icon(VaadinIcon.EYE), e -> {
            // First save
            try {
                com.docmgmt.dto.TileConfigurationDTO dto = com.docmgmt.dto.TileConfigurationDTO.builder()
                    .id(config.getId())
                    .folderId(currentFolder.getId())
                    .groupBySubfolder(groupBySubfolderCheckbox.getValue())
                    .visibleFields(visibleFieldsField.getValue())
                    .colorStrategy(colorStrategyCombo.getValue() != null ? colorStrategyCombo.getValue().name() : null)
                    .colorMappings(colorMappingsArea.getValue())
                    .tileSize(tileSizeCombo.getValue() != null ? tileSizeCombo.getValue().name() : null)
                    .showDetailLink(showDetailLinkCheckbox.getValue())
                    .showUrlLink(showUrlLinkCheckbox.getValue())
                    .sortOrder(sortOrderCombo.getValue() != null ? sortOrderCombo.getValue().name() : null)
                    .build();
                
                tileService.saveConfiguration(dto);
                dialog.close();
                
                // Then navigate to tile view
                getUI().ifPresent(ui -> ui.navigate("tiles/" + currentFolder.getName()));
                
            } catch (Exception ex) {
                logger.error("Error saving tile configuration", ex);
                Notification.show("Error saving configuration: " + ex.getMessage(), 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        previewButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, saveButton, previewButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(
            title,
            helpText,
            new Hr(),
            formLayout,
            new Hr(),
            buttons
        );
        layout.setPadding(true);
        layout.setSpacing(true);
        
        dialog.add(layout);
        dialog.open();
    }
}
