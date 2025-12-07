package com.docmgmt.ui.views;

import com.docmgmt.model.User;
import com.docmgmt.service.UserService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | Document Management System")
public class UserView extends VerticalLayout {

    private final UserService userService;
    
    private Grid<User> grid;
    private TextField filterText;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button toggleActiveButton;
    
    private Dialog userDialog;
    private Binder<User> binder;
    private User currentUser;
    
    private ListDataProvider<User> dataProvider;
    
    @Autowired
    public UserView(UserService userService) {
        this.userService = userService;
        
        addClassName("user-view");
        setSizeFull();
        
        configureGrid();
        configureFilter();
        configureDialog();
        
        HorizontalLayout toolbar = createToolbar();
        
        VerticalLayout mainLayout = new VerticalLayout(toolbar, grid);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.expand(grid);
        
        add(mainLayout);
        
        updateList();
    }
    
    private void configureGrid() {
        grid = new Grid<>(User.class, false);
        grid.addClassNames("user-grid");
        grid.setSizeFull();
        
        // Add columns
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true).setWidth("150px");
        grid.addColumn(User::getEmail).setHeader("Email").setSortable(true).setWidth("200px");
        grid.addColumn(User::getFullName).setHeader("Full Name").setSortable(true).setWidth("200px");
        grid.addColumn(new ComponentRenderer<>(user -> {
            Span badge = new Span(user.isAccountActive() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add(user.isAccountActive() ? "badge success" : "badge error");
            return badge;
        })).setHeader("Status").setSortable(true).setWidth("100px");
        
        grid.addColumn(user -> user.getOwner() != null ? user.getOwner().getUsername() : "-")
            .setHeader("Owner").setSortable(false).setWidth("150px");
        
        grid.addColumn(user -> user.getMajorVersion() + "." + user.getMinorVersion())
            .setHeader("Version").setSortable(true).setWidth("100px");
        
        grid.addColumn(user -> user.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .setHeader("Last Modified").setSortable(true).setWidth("150px");
        
        // Configure grid styling and behavior
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            boolean hasSelection = event.getValue() != null;
            editButton.setEnabled(hasSelection);
            deleteButton.setEnabled(hasSelection);
            toggleActiveButton.setEnabled(hasSelection);
            
            if (hasSelection) {
                toggleActiveButton.setText(event.getValue().isAccountActive() ? "Deactivate" : "Activate");
            }
        });
    }
    
    private void configureFilter() {
        filterText = new TextField();
        filterText.setPlaceholder("Filter by username, email, or name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> filterGrid());
    }
    
    private void filterGrid() {
        if (dataProvider != null) {
            dataProvider.setFilter(user -> {
                String filter = filterText.getValue().toLowerCase();
                if (filter.isEmpty()) {
                    return true;
                }
                
                boolean usernameMatches = user.getUsername().toLowerCase().contains(filter);
                boolean emailMatches = user.getEmail().toLowerCase().contains(filter);
                boolean nameMatches = user.getFullName().toLowerCase().contains(filter);
                
                return usernameMatches || emailMatches || nameMatches;
            });
        }
    }
    
    private HorizontalLayout createToolbar() {
        filterText.setWidth("300px");
        
        addButton = new Button("Add User", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openDialog(null));
        
        editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
        editButton.setEnabled(false);
        editButton.addClickListener(e -> {
            User selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openDialog(selected);
            }
        });
        
        deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
        deleteButton.setEnabled(false);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> {
            User selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                confirmDelete(selected);
            }
        });
        
        toggleActiveButton = new Button("Deactivate", new Icon(VaadinIcon.BAN));
        toggleActiveButton.setEnabled(false);
        toggleActiveButton.addClickListener(e -> {
            User selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                toggleUserActive(selected);
            }
        });
        
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addButton, editButton, deleteButton, toggleActiveButton);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        
        return toolbar;
    }
    
    private void configureDialog() {
        userDialog = new Dialog();
        userDialog.setWidth("500px");
        
        binder = new BeanValidationBinder<>(User.class);
    }
    
    private void openDialog(User user) {
        currentUser = user;
        boolean isNew = (user == null);
        
        userDialog.removeAll();
        userDialog.setHeaderTitle(isNew ? "New User" : "Edit User");
        
        FormLayout formLayout = new FormLayout();
        
        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        
        TextField usernameField = new TextField("Username");
        usernameField.setRequired(true);
        usernameField.setWidthFull();
        
        EmailField emailField = new EmailField("Email");
        emailField.setRequired(true);
        emailField.setWidthFull();
        
        TextField firstNameField = new TextField("First Name");
        firstNameField.setWidthFull();
        
        TextField lastNameField = new TextField("Last Name");
        lastNameField.setWidthFull();
        
        Checkbox activeCheckbox = new Checkbox("Active");
        activeCheckbox.setValue(true);
        
        formLayout.add(nameField, usernameField, emailField, firstNameField, lastNameField, activeCheckbox);
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(usernameField, 2);
        formLayout.setColspan(emailField, 2);
        
        // Bind fields
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(User::getName, User::setName);
        
        binder.forField(usernameField)
            .asRequired("Username is required")
            .bind(User::getUsername, User::setUsername);
        
        binder.forField(emailField)
            .asRequired("Email is required")
            .bind(User::getEmail, User::setEmail);
        
        binder.bind(firstNameField, User::getFirstName, User::setFirstName);
        binder.bind(lastNameField, User::getLastName, User::setLastName);
        binder.bind(activeCheckbox, User::getIsActive, User::setIsActive);
        
        if (isNew) {
            binder.readBean(User.builder()
                .majorVersion(1)
                .minorVersion(0)
                .isActive(true)
                .build());
        } else {
            binder.readBean(user);
        }
        
        // Dialog buttons
        Button saveButton = new Button("Save", e -> saveUser(isNew));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> userDialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        
        userDialog.add(dialogLayout);
        userDialog.open();
    }
    
    private void saveUser(boolean isNew) {
        try {
            User user;
            if (isNew) {
                user = User.builder().build();
                binder.writeBean(user);
                userService.createUser(user);
                Notification.show("User created successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                user = currentUser;
                binder.writeBean(user);
                userService.updateUser(user.getId(), user);
                Notification.show("User updated successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            
            userDialog.close();
            updateList();
            grid.select(user);
        } catch (ValidationException e) {
            Notification.show("Please fix validation errors", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (IllegalArgumentException e) {
            Notification.show(e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show("Error saving user: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void confirmDelete(User user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete User?");
        dialog.setText("Are you sure you want to delete user '" + user.getUsername() + "'? This action cannot be undone.");
        
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(e -> {
            try {
                userService.delete(user.getId());
                Notification.show("User deleted successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateList();
            } catch (Exception ex) {
                Notification.show("Error deleting user: " + ex.getMessage(), 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        dialog.open();
    }
    
    private void toggleUserActive(User user) {
        try {
            if (user.isAccountActive()) {
                userService.deactivateUser(user.getId());
                Notification.show("User deactivated", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                userService.activateUser(user.getId());
                Notification.show("User activated", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            updateList();
            grid.select(user);
        } catch (Exception e) {
            Notification.show("Error toggling user status: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void updateList() {
        try {
            List<User> users = userService.findAll();
            dataProvider = new ListDataProvider<>(users);
            grid.setDataProvider(dataProvider);
            filterGrid();
        } catch (Exception e) {
            Notification.show("Error loading users: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
