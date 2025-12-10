package com.docmgmt.ui.views;

import com.docmgmt.dto.TileConfigurationDTO;
import com.docmgmt.model.Folder;
import com.docmgmt.model.TileConfiguration;
import com.docmgmt.service.FolderService;
import com.docmgmt.service.TileService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * View for configuring tile display settings
 * Accessible at /tile-config/{folderName}
 */
@Route(value = "tile-config", layout = MainLayout.class)
@PageTitle("Tile Configuration | Document Management System")
public class TileConfigurationView extends VerticalLayout implements HasUrlParameter<String> {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TileConfigurationView.class);
    
    private final TileService tileService;
    private final FolderService folderService;
    
    private String folderName;
    private Folder folder;
    private TileConfiguration config;
    
    private Checkbox groupBySubfolderCheckbox;
    private TextField visibleFieldsField;
    private ComboBox<TileConfiguration.ColorStrategy> colorStrategyCombo;
    private TextArea colorMappingsArea;
    private ComboBox<TileConfiguration.TileSize> tileSizeCombo;
    private Checkbox showDetailLinkCheckbox;
    private Checkbox showUrlLinkCheckbox;
    private ComboBox<TileConfiguration.SortOrder> sortOrderCombo;
    private Checkbox hideNavigationCheckbox;
    private Checkbox hideEditButtonsCheckbox;
    private com.vaadin.flow.component.textfield.NumberField backgroundOpacityField;
    
    @Autowired
    public TileConfigurationView(TileService tileService, FolderService folderService) {
        this.tileService = tileService;
        this.folderService = folderService;
        
        addClassName("tile-configuration-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }
    
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null || parameter.trim().isEmpty()) {
            showError("No folder specified");
            return;
        }
        
        this.folderName = parameter;
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        try {
            removeAll();
            
            // Get folder
            List<Folder> folders = folderService.findAllVersionsByName(folderName);
            if (folders.isEmpty()) {
                showError("Folder not found: " + folderName);
                return;
            }
            
            folder = folders.get(0);
            config = tileService.getConfiguration(folder.getId());
            
            // Header
            H2 title = new H2("Tile Configuration: " + folderName);
            add(title);
            
            // Form
            FormLayout formLayout = createForm();
            add(formLayout);
            
            // Buttons
            HorizontalLayout buttonsLayout = createButtons();
            add(buttonsLayout);
            
            // Populate form with current configuration
            populateForm();
            
        } catch (Exception e) {
            logger.error("Error loading configuration for folder: {}", folderName, e);
            showError("Error loading configuration: " + e.getMessage());
        }
    }
    
    private FormLayout createForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Group by subfolder
        groupBySubfolderCheckbox = new Checkbox("Group by Subfolder");
        form.add(groupBySubfolderCheckbox, 2);
        
        // Visible fields
        visibleFieldsField = new TextField("Visible Fields");
        visibleFieldsField.setHelperText("Comma-separated list of fields to display (e.g., name,description,url,documentType,tags)");
        visibleFieldsField.setWidthFull();
        form.add(visibleFieldsField, 2);
        
        // Color strategy
        colorStrategyCombo = new ComboBox<>("Color Strategy");
        colorStrategyCombo.setItems(TileConfiguration.ColorStrategy.values());
        colorStrategyCombo.setHelperText("How to color-code tiles");
        form.add(colorStrategyCombo);
        
        // Tile size
        tileSizeCombo = new ComboBox<>("Tile Size");
        tileSizeCombo.setItems(TileConfiguration.TileSize.values());
        form.add(tileSizeCombo);
        
        // Sort order
        sortOrderCombo = new ComboBox<>("Sort Order");
        sortOrderCombo.setItems(TileConfiguration.SortOrder.values());
        form.add(sortOrderCombo, 2);
        
        // Color mappings
        colorMappingsArea = new TextArea("Custom Color Mappings");
        colorMappingsArea.setHelperText("JSON format: {\"key1\": \"#FF5733\", \"key2\": \"#33FF57\"}");
        colorMappingsArea.setWidthFull();
        colorMappingsArea.setHeight("150px");
        form.add(colorMappingsArea, 2);
        
        // Show links
        showDetailLinkCheckbox = new Checkbox("Show Detail Link");
        form.add(showDetailLinkCheckbox);
        
        showUrlLinkCheckbox = new Checkbox("Show URL Link");
        form.add(showUrlLinkCheckbox);
        
        // Hide options
        hideNavigationCheckbox = new Checkbox("Hide Navigation Panel");
        form.add(hideNavigationCheckbox);
        
        hideEditButtonsCheckbox = new Checkbox("Hide Edit Buttons");
        form.add(hideEditButtonsCheckbox);
        
        // Background color opacity
        backgroundOpacityField = new com.vaadin.flow.component.textfield.NumberField("Background Color Opacity");
        backgroundOpacityField.setMin(0.0);
        backgroundOpacityField.setMax(1.0);
        backgroundOpacityField.setStep(0.05);
        backgroundOpacityField.setValue(0.05);
        backgroundOpacityField.setHelperText("0.0 = no background, 1.0 = full color (recommended: 0.03-0.10)");
        backgroundOpacityField.setWidthFull();
        form.add(backgroundOpacityField, 2);
        
        return form;
    }
    
    private HorizontalLayout createButtons() {
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        
        Button saveButton = new Button("Save Configuration");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveConfiguration());
        
        Button previewButton = new Button("Preview");
        previewButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        previewButton.addClickListener(e -> navigateToPreview());
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> navigateToFolders());
        
        buttons.add(saveButton, previewButton, cancelButton);
        return buttons;
    }
    
    private void populateForm() {
        groupBySubfolderCheckbox.setValue(config.getGroupBySubfolder() != null ? config.getGroupBySubfolder() : false);
        visibleFieldsField.setValue(config.getVisibleFields() != null ? config.getVisibleFields() : "");
        colorStrategyCombo.setValue(config.getColorStrategy());
        colorMappingsArea.setValue(config.getColorMappings() != null ? config.getColorMappings() : "");
        tileSizeCombo.setValue(config.getTileSize());
        showDetailLinkCheckbox.setValue(config.getShowDetailLink() != null ? config.getShowDetailLink() : true);
        showUrlLinkCheckbox.setValue(config.getShowUrlLink() != null ? config.getShowUrlLink() : true);
        sortOrderCombo.setValue(config.getSortOrder());
        hideNavigationCheckbox.setValue(config.getHideNavigation() != null ? config.getHideNavigation() : false);
        hideEditButtonsCheckbox.setValue(config.getHideEditButtons() != null ? config.getHideEditButtons() : false);
        backgroundOpacityField.setValue(config.getBackgroundColorOpacity() != null ? config.getBackgroundColorOpacity() : 0.05);
    }
    
    private void saveConfiguration() {
        try {
            // Create DTO from form
            TileConfigurationDTO dto = TileConfigurationDTO.builder()
                .id(config.getId())
                .folderId(folder.getId())
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
            
            // Save
            tileService.saveConfiguration(dto);
            
            Notification notification = Notification.show("Configuration saved successfully", 3000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Reload
            loadConfiguration();
            
        } catch (Exception e) {
            logger.error("Error saving configuration", e);
            Notification notification = Notification.show("Error saving configuration: " + e.getMessage(), 
                5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void navigateToPreview() {
        getUI().ifPresent(ui -> ui.navigate("tiles/" + folderName));
    }
    
    private void navigateToFolders() {
        getUI().ifPresent(ui -> ui.navigate("folders"));
    }
    
    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        
        removeAll();
        H3 errorTitle = new H3("Error");
        add(errorTitle, new com.vaadin.flow.component.html.Paragraph(message));
    }
}
