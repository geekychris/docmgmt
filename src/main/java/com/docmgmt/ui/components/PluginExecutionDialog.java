package com.docmgmt.ui.components;

import com.docmgmt.dto.PluginInfoDTO;
import com.docmgmt.model.Document;
import com.docmgmt.plugin.PluginParameter;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.plugin.PluginService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Dynamic dialog for executing plugins with parameter inputs
 */
public class PluginExecutionDialog extends Dialog {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginExecutionDialog.class);
    
    private final PluginInfoDTO pluginInfo;
    private final Document document;
    private final PluginService pluginService;
    private final Consumer<PluginResponse> onSuccess;
    
    private final Map<String, Object> parameterValues = new HashMap<>();
    private final VerticalLayout contentLayout;
    private final VerticalLayout loadingLayout;
    private final Button executeButton;
    private final Checkbox saveAsMarkdownCheckbox;
    
    public PluginExecutionDialog(Document document, 
                                PluginInfoDTO pluginInfo,
                                PluginService pluginService,
                                Consumer<PluginResponse> onSuccess) {
        this.document = document;
        this.pluginInfo = pluginInfo;
        this.pluginService = pluginService;
        this.onSuccess = onSuccess;
        
        setWidth("600px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        
        // Title
        H2 title = new H2(pluginInfo.getDescription());
        Icon icon = VaadinIcon.valueOf(pluginInfo.getIcon()).create();
        icon.setSize("24px");
        icon.getStyle().set("margin-right", "10px");
        
        HorizontalLayout titleLayout = new HorizontalLayout(icon, title);
        titleLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        
        // Document info
        Span docInfo = new Span("Document: " + document.getName());
        docInfo.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");
        
        // Parameters form
        FormLayout parameterForm = buildParameterForm();
        
        // Save as markdown checkbox
        saveAsMarkdownCheckbox = new Checkbox("Save result as markdown file");
        saveAsMarkdownCheckbox.getStyle()
            .set("margin-top", "10px")
            .set("font-size", "var(--lumo-font-size-s)");
        
        // Content layout (shows form)
        contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.add(docInfo, new Hr(), parameterForm, saveAsMarkdownCheckbox);
        
        // Loading layout (hidden initially)
        loadingLayout = new VerticalLayout();
        loadingLayout.setPadding(true);
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setVisible(false);
        
        Span loadingText = new Span("Executing " + pluginInfo.getTaskName() + "...");
        loadingText.getStyle().set("font-size", "var(--lumo-font-size-l)");
        loadingLayout.add(loadingText);
        
        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        
        executeButton = new Button("Execute", e -> executePlugin());
        executeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, executeButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        // Main layout
        VerticalLayout mainLayout = new VerticalLayout(
            titleLayout,
            contentLayout,
            loadingLayout,
            new Hr(),
            buttons
        );
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        
        add(mainLayout);
    }
    
    private FormLayout buildParameterForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        if (pluginInfo.getParameters() == null || pluginInfo.getParameters().isEmpty()) {
            Span noParams = new Span("This plugin requires no additional parameters.");
            noParams.getStyle().set("font-style", "italic");
            form.add(noParams);
            return form;
        }
        
        for (PluginParameter param : pluginInfo.getParameters()) {
            com.vaadin.flow.component.Component field = createFieldForParameter(param);
            if (field != null) {
                form.add(field);
            }
        }
        
        return form;
    }
    
    private com.vaadin.flow.component.Component createFieldForParameter(PluginParameter param) {
        switch (param.getType()) {
            case TEXT:
                TextField textField = new TextField(param.getLabel());
                textField.setPlaceholder(param.getDescription());
                textField.setValueChangeMode(ValueChangeMode.EAGER);
                if (param.getDefaultValue() != null) {
                    textField.setValue(param.getDefaultValue());
                }
                textField.addValueChangeListener(e -> parameterValues.put(param.getName(), e.getValue()));
                textField.setWidthFull();
                return textField;
                
            case TEXTAREA:
                TextArea textArea = new TextArea(param.getLabel());
                textArea.setPlaceholder(param.getDescription());
                textArea.setHeight("150px");
                if (param.getDefaultValue() != null) {
                    textArea.setValue(param.getDefaultValue());
                }
                textArea.addValueChangeListener(e -> parameterValues.put(param.getName(), e.getValue()));
                textArea.setWidthFull();
                return textArea;
                
            case NUMBER:
                IntegerField numberField = new IntegerField(param.getLabel());
                numberField.setHelperText(param.getDescription());
                if (param.getMinValue() != null) {
                    numberField.setMin(param.getMinValue());
                }
                if (param.getMaxValue() != null) {
                    numberField.setMax(param.getMaxValue());
                }
                if (param.getDefaultValue() != null) {
                    try {
                        numberField.setValue(Integer.parseInt(param.getDefaultValue()));
                    } catch (NumberFormatException ignored) {}
                }
                numberField.addValueChangeListener(e -> {
                    if (e.getValue() != null) {
                        parameterValues.put(param.getName(), e.getValue());
                    }
                });
                numberField.setWidthFull();
                return numberField;
                
            case SELECT:
                ComboBox<String> comboBox = new ComboBox<>(param.getLabel());
                comboBox.setHelperText(param.getDescription());
                if (param.getOptions() != null) {
                    comboBox.setItems(param.getOptions());
                }
                if (param.getDefaultValue() != null) {
                    comboBox.setValue(param.getDefaultValue());
                }
                comboBox.addValueChangeListener(e -> {
                    if (e.getValue() != null) {
                        parameterValues.put(param.getName(), e.getValue());
                    }
                });
                comboBox.setWidthFull();
                return comboBox;
                
            case BOOLEAN:
                Checkbox checkbox = new Checkbox(param.getLabel());
                if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                    Span helpText = new Span(param.getDescription());
                    helpText.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
                    VerticalLayout checkboxLayout = new VerticalLayout(checkbox, helpText);
                    checkboxLayout.setPadding(false);
                    checkboxLayout.setSpacing(false);
                    if (param.getDefaultValue() != null) {
                        checkbox.setValue(Boolean.parseBoolean(param.getDefaultValue()));
                    }
                    checkbox.addValueChangeListener(e -> parameterValues.put(param.getName(), e.getValue()));
                    return checkboxLayout;
                }
                if (param.getDefaultValue() != null) {
                    checkbox.setValue(Boolean.parseBoolean(param.getDefaultValue()));
                }
                checkbox.addValueChangeListener(e -> parameterValues.put(param.getName(), e.getValue()));
                return checkbox;
                
            default:
                return null;
        }
    }
    
    private void executePlugin() {
        contentLayout.setVisible(false);
        loadingLayout.setVisible(true);
        executeButton.setEnabled(false);
        
        boolean saveAsMarkdown = saveAsMarkdownCheckbox.getValue();
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return pluginService.executePlugin(document.getId(), pluginInfo.getTaskName(), parameterValues, saveAsMarkdown);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(response -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                close();
                
                if (response.getStatus() == PluginResponse.PluginStatus.SUCCESS) {
                    if (onSuccess != null) {
                        onSuccess.accept(response);
                    }
                    Notification.show("Plugin executed successfully", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Plugin execution failed: " + response.getErrorMessage(), 
                        5000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                ui.push();
            }));
        }).exceptionally(ex -> {
            logger.error("Plugin execution failed", ex);
            getUI().ifPresent(ui -> ui.access(() -> {
                close();
                String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                Notification.show("Plugin execution failed: " + errorMsg, 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ui.push();
            }));
            return null;
        });
    }
}
