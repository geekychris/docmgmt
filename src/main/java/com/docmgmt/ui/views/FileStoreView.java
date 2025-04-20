package com.docmgmt.ui.views;

import com.docmgmt.model.FileStore;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Route(value = "filestores", layout = MainLayout.class)
@PageTitle("FileStores | Document Management System")
public class FileStoreView extends VerticalLayout {

    private final FileStoreService fileStoreService;
    
    private Grid<FileStore> grid;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button toggleStatusButton;
    
    private Dialog fileStoreDialog;
    private Binder<FileStore> binder;
    private FileStore currentFileStore;
    
    private ListDataProvider<FileStore> dataProvider;
    
    @Autowired
    public FileStoreView(FileStoreService fileStoreService) {
        this.fileStoreService = fileStoreService;
        
        addClassName("filestore-view");
        setSizeFull();
        
        configureGrid();
        
        HorizontalLayout toolbar = createToolbar();
        
        add(toolbar, grid);
        
        updateList();
    }
    
    private void configureGrid() {
        grid = new Grid<>(FileStore.class, false);
        grid.addClassNames("filestore-grid");
        grid.setSizeFull();
        
        // Add columns to the grid
        grid.addColumn(FileStore::getName).setHeader("Name").setSortable(true);
        grid.addColumn(FileStore::getRootPath).setHeader("Root Path").setSortable(true);
        
        // Status column with color indicator
        grid.addColumn(new ComponentRenderer<>(fileStore -> {
            Span status = new Span(fileStore.getStatus().toString());
            status.getElement().getThemeList().add(
                fileStore.getStatus() == FileStore.Status.ACTIVE ? "badge success" : "badge error"
            );
            return status;
        })).setHeader("Status").setSortable(true);
        
        // Content count column
        grid.addColumn(fileStore -> fileStore.getContents().size())
            .setHeader("Content Count").setSortable(true);
        
        // Configure grid styling and behavior
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            boolean hasSelection = event.getValue() != null;
            editButton.setEnabled(hasSelection);
            deleteButton.setEnabled(hasSelection);
            toggleStatusButton.setEnabled(hasSelection);
        });
    }
    
    private HorizontalLayout createToolbar() {
        // Configure buttons
        addButton = new Button("Add FileStore", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openFileStoreDialog(new FileStore()));
        
        editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
        editButton.setEnabled(false);
        editButton.addClickListener(e -> {
            FileStore fileStore = grid.asSingleSelect().getValue();
            if (fileStore != null) {
                openFileStoreDialog(fileStore);
            }
        });
        
        deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setEnabled(false);
        deleteButton.addClickListener(e -> {
            FileStore fileStore = grid.asSingleSelect().getValue();
            if (fileStore != null) {
                confirmDelete(fileStore);
            }
        });
        
        toggleStatusButton = new Button("Toggle Status", new Icon(VaadinIcon.EXCHANGE));
        toggleStatusButton.setEnabled(false);
        toggleStatusButton.addClickListener(e -> {
            FileStore fileStore = grid.asSingleSelect().getValue();
            if (fileStore != null) {
                toggleFileStoreStatus(fileStore);
            }
        });
        
        // Create the toolbar layout
        HorizontalLayout toolbar = new HorizontalLayout(
            addButton, editButton, deleteButton, toggleStatusButton
        );
        
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(true);
        
        return toolbar;
    }
    
    private void openFileStoreDialog(FileStore fileStore) {
        currentFileStore = fileStore;
        
        fileStoreDialog = new Dialog();
        fileStoreDialog.setWidth("500px");
        
        // Create dialog title based on whether we are creating or editing
        H2 title = new H2(fileStore.getId() == null ? "Create New FileStore" : "Edit FileStore");
        
        // Create form fields
        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        
        TextField rootPathField = new TextField("Root Path");
        rootPathField.setRequired(true);
        rootPathField.setWidthFull();
        
        Select<FileStore.Status> statusSelect = new Select<>();
        statusSelect.setLabel("Status");
        statusSelect.setItems(FileStore.Status.values());
        statusSelect.setRequiredIndicatorVisible(true);
        
        // Create the form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, rootPathField, statusSelect);
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1)
        );
        
        // Create buttons for the dialog
        Button cancelButton = new Button("Cancel", e -> fileStoreDialog.close());
        Button saveButton = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                saveFileStore();
                fileStoreDialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setPadding(true);
        buttonLayout.setWidthFull();
        
        // Set up the binder
        binder = new BeanValidationBinder<>(FileStore.class);
        
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(FileStore::getName, FileStore::setName);
        
        // Root path validation
        binder.forField(rootPathField)
            .asRequired("Root path is required")
            .withValidator(validateRootPath())
            .bind(FileStore::getRootPath, FileStore::setRootPath);
        
        binder.forField(statusSelect)
            .asRequired("Status is required")
            .bind(FileStore::getStatus, FileStore::setStatus);
        
        // Read the fileStore into the form
        binder.readBean(fileStore);
        
        // Add components to the dialog
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        fileStoreDialog.add(dialogLayout);
        fileStoreDialog.open();
    }
    
    private Validator<String> validateRootPath() {
        return (value, context) -> {
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.error("Root path is required");
            }
            
            try {
                Path path = Paths.get(value);
                
                // Check if the path exists
                if (!Files.exists(path)) {
                    // Create a confirmation dialog for creating the directory
                    ConfirmDialog dialog = new ConfirmDialog();
                    dialog.setHeader("Path Does Not Exist");
                    dialog.setText("The path '" + value + "' does not exist. Do you want to create it?");
                    
                    dialog.setCancelable(true);
                    dialog.setCancelText("Cancel");
                    
                    dialog.setConfirmText("Create");
                    dialog.setConfirmButtonTheme("primary");
                    
                    dialog.addConfirmListener(event -> {
                        try {
                            Files.createDirectories(path);
                            Notification.show("Directory created successfully.", 
                                3000, Notification.Position.BOTTOM_START)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            //XXX CJC ?
                            //context.getComponent().ifPresent(c -> c.se(false));
                        } catch (Exception e) {
                            Notification.show("Failed to create directory: " + e.getMessage(), 
                                3000, Notification.Position.BOTTOM_START)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                            //context.getComponent().ifPresent(c -> c.setInvalid(true));
                        }
                    });
                    
                    dialog.open();
                    return ValidationResult.error("Path does not exist. Please create it first or select an existing path.");
                }
                
                // Check if it's a directory
                if (!Files.isDirectory(path)) {
                    return ValidationResult.error("Root path must be a directory");
                }
                
                // Check if it's readable and writable
                if (!Files.isReadable(path) || !Files.isWritable(path)) {
                    return ValidationResult.error("Directory is not readable or writable");
                }
                
                return ValidationResult.ok();
            } catch (Exception e) {
                return ValidationResult.error("Invalid path: " + e.getMessage());
            }
        };
    }
    
    private void saveFileStore() {
        if (currentFileStore != null && binder.writeBeanIfValid(currentFileStore)) {
            try {
                fileStoreService.save(currentFileStore);
                updateList();
                Notification.show("FileStore saved successfully.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to save FileStore: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
    
    private void confirmDelete(FileStore fileStore) {
        // Only allow delete if there are no contents in the file store
        if (!fileStore.getContents().isEmpty()) {
            Notification.show("Cannot delete FileStore with associated content. Remove content first.", 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete FileStore");
        dialog.setText("Are you sure you want to delete '" + fileStore.getName() + "'? This action cannot be undone.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(event -> {
            try {
                fileStoreService.delete(fileStore.getId());
                updateList();
                Notification.show("FileStore deleted.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to delete FileStore: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        dialog.open();
    }
    
    private void toggleFileStoreStatus(FileStore fileStore) {
        try {
            // Toggle the status
            FileStore.Status newStatus = fileStore.getStatus() == FileStore.Status.ACTIVE ? 
                FileStore.Status.INACTIVE : FileStore.Status.ACTIVE;
                
            fileStore.setStatus(newStatus);
            fileStoreService.save(fileStore);
            
            updateList();
            
            String statusText = newStatus == FileStore.Status.ACTIVE ? "activated" : "deactivated";
            Notification.show("FileStore " + statusText + " successfully.", 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to update FileStore status: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }
    
    /**
     * Updates the file store list in the grid by fetching fresh data from the service.
     * This method is called after CRUD operations to refresh the UI.
     */
    private void updateList() {
        try {
            // Get all file stores
            List<FileStore> fileStores = fileStoreService.findAll();
            
            // Update the data provider
            dataProvider = DataProvider.ofCollection(fileStores);
            grid.setDataProvider(dataProvider);
            
            // Reset button states since selection is cleared
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            toggleStatusButton.setEnabled(false);
        } catch (Exception e) {
            Notification.show("Failed to load file stores: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Log the error for server-side tracking
            e.printStackTrace();
        }
    }
}
