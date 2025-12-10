package com.docmgmt.ui.components;

import com.docmgmt.model.Document;
import com.docmgmt.model.User;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.UserService;
import com.docmgmt.ui.util.ColorPickerUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable dialog for editing document properties
 */
public class DocumentEditDialog extends Dialog {
    
    private final Document document;
    private final DocumentService documentService;
    private final UserService userService;
    private final Consumer<Document> onSaveCallback;
    
    public DocumentEditDialog(Document document,
                             DocumentService documentService,
                             UserService userService,
                             Consumer<Document> onSaveCallback) {
        this.document = document;
        this.documentService = documentService;
        this.userService = userService;
        this.onSaveCallback = onSaveCallback;
        
        initializeDialog();
    }
    
    private void initializeDialog() {
        setWidth("900px");
        setHeight("80vh");
        
        // Reload document with contents
        Document reloadedDoc = documentService.findById(document.getId());
        if (reloadedDoc.getContents() != null) {
            reloadedDoc.getContents().size(); // Force initialization
        }
        
        H2 title = new H2("Edit Document: " + reloadedDoc.getName());
        
        // Version picker
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
                close();
                new DocumentEditDialog(e.getValue(), documentService, userService, onSaveCallback).open();
            }
        });
        
        // Edit mode toggle (always on for this dialog, but keeping for consistency)
        Checkbox editModeCheckbox = new Checkbox("Edit Mode");
        editModeCheckbox.setValue(true);
        editModeCheckbox.setEnabled(false); // Always in edit mode
        
        HorizontalLayout versionRow = new HorizontalLayout(versionPicker, editModeCheckbox);
        versionRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        versionRow.setSpacing(true);
        versionRow.setWidthFull();
        
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
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        // Save button
        Button saveButton = new Button("Save Changes", e -> {
            try {
                documentService.save(reloadedDoc);
                Notification.show("Document saved successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                close();
                if (onSaveCallback != null) {
                    onSaveCallback.accept(reloadedDoc);
                }
            } catch (Exception ex) {
                Notification.show("Error saving document: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, versionRow, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        
        add(dialogLayout);
    }
}
