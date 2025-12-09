package com.docmgmt.ui.components;

import com.docmgmt.dto.PluginInfoDTO;
import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.User;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.plugin.PluginService;
import com.docmgmt.service.*;
import com.docmgmt.dto.FieldSuggestionDTO;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Reusable dialog component for displaying document details with full interaction capabilities
 * including AI plugins, content viewing, versioning, and similarity search.
 */
public class DocumentDetailDialog extends Dialog {
    
    private final Document document;
    private final DocumentService documentService;
    private final UserService userService;
    private final ContentService contentService;
    private final PluginService pluginService;
    private final DocumentSimilarityService similarityService;
    private final DocumentFieldExtractionService fieldExtractionService;
    
    public DocumentDetailDialog(Document document,
                               DocumentService documentService,
                               UserService userService,
                               ContentService contentService,
                               PluginService pluginService,
                               DocumentSimilarityService similarityService,
                               DocumentFieldExtractionService fieldExtractionService) {
        this.document = document;
        this.documentService = documentService;
        this.userService = userService;
        this.contentService = contentService;
        this.pluginService = pluginService;
        this.similarityService = similarityService;
        this.fieldExtractionService = fieldExtractionService;
        
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
        
        H2 title = new H2("Document: " + reloadedDoc.getName());
        
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
                new DocumentDetailDialog(e.getValue(), documentService, userService, 
                    contentService, pluginService, similarityService, fieldExtractionService).open();
            }
        });
        
        // Document fields container
        VerticalLayout documentFieldsContainer = new VerticalLayout();
        documentFieldsContainer.setPadding(false);
        documentFieldsContainer.setSpacing(true);
        
        // Use DocumentFieldRenderer to show all fields
        DocumentFieldRenderer.renderReadOnlyFields(reloadedDoc, documentFieldsContainer);
        
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
        
        Grid<Content> contentGrid = new Grid<>(Content.class, false);
        contentGrid.setHeight("200px");
        
        contentGrid.addColumn(Content::getName)
            .setHeader("Name").setResizable(true).setAutoWidth(true).setFlexGrow(1);
        contentGrid.addColumn(content -> content.getContentType() != null ? content.getContentType() : "-")
            .setHeader("Type").setResizable(true).setAutoWidth(true);
        contentGrid.addColumn(content -> content.isPrimary() ? "Primary" : "Secondary")
            .setHeader("Rendition").setResizable(true).setAutoWidth(true);
        
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
        
        contentGrid.addSelectionListener(event -> {
            viewContentButton.setEnabled(event.getFirstSelectedItem().isPresent());
        });
        
        // Check if document has text content
        boolean hasTextContent = reloadedDoc.getContents() != null && reloadedDoc.getContents().stream()
            .anyMatch(c -> (c.getContentType() != null && 
                          (c.getContentType().startsWith("text/") || 
                           ("text/plain".equals(c.getContentType()) && c.isIndexable()))));
        
        // Extract Fields button
        Button extractFieldsButton = new Button("Extract Fields (AI)", new Icon(VaadinIcon.LIGHTBULB));
        extractFieldsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        extractFieldsButton.setEnabled(hasTextContent);
        if (!hasTextContent) {
            extractFieldsButton.setTooltipText("No text content available. Upload a text file or transform PDF to text first.");
        }
        extractFieldsButton.addClickListener(e -> {
            close();
            openFieldExtractionDialog(reloadedDoc);
        });
        
        // AI Plugins menu
        MenuBar pluginsMenu = new MenuBar();
        MenuItem pluginsMenuItem = pluginsMenu.addItem("AI Plugins");
        SubMenu pluginsSubMenu = pluginsMenuItem.getSubMenu();
        
        // Load plugins and group by category
        List<PluginInfoDTO> allPlugins = pluginService.getDetailedPluginInfo();
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
                    close();
                    openPluginDialog(reloadedDoc, pluginInfo);
                });
            }
        }
        
        pluginsMenu.setEnabled(hasTextContent);
        
        HorizontalLayout contentToolbar = new HorizontalLayout(viewContentButton, extractFieldsButton, pluginsMenu);
        
        // Version control buttons
        Button createMajorVersionButton = new Button("Create Major Version", new Icon(VaadinIcon.PLUS_CIRCLE));
        createMajorVersionButton.addClickListener(e -> {
            try {
                documentService.createMajorVersion(reloadedDoc.getId());
                Notification.show("Major version created", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                close();
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
                close();
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
            close();
            openSimilarityDialog(reloadedDoc);
        });
        
        HorizontalLayout similarityButtons = new HorizontalLayout(findSimilarButton);
        similarityButtons.setSpacing(true);
        
        // Dialog buttons
        Button closeButton = new Button("Close", e -> close());
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
            similarityButtons,
            buttons
        );
        layout.setPadding(true);
        layout.setSpacing(true);
        
        add(layout);
    }
    
    private void viewContent(Content content) {
        Dialog viewDialog = new Dialog();
        viewDialog.setWidth("90vw");
        viewDialog.setHeight("90vh");
        
        H2 title = new H2("Content: " + content.getName());
        
        VerticalLayout contentView = new VerticalLayout();
        contentView.setSizeFull();
        
        try {
            byte[] contentBytes = contentService.getContentBytes(content.getId());
            String contentType = content.getContentType();
            
            if (contentType != null && contentType.startsWith("text/")) {
                String textContent = new String(contentBytes, java.nio.charset.StandardCharsets.UTF_8);
                com.vaadin.flow.component.html.Pre pre = new com.vaadin.flow.component.html.Pre(textContent);
                pre.getStyle()
                    .set("white-space", "pre-wrap")
                    .set("font-family", "monospace")
                    .set("padding", "10px");
                contentView.add(pre);
            } else {
                Span unsupportedMsg = new Span("Content type not supported for inline viewing: " + contentType);
                contentView.add(unsupportedMsg);
            }
        } catch (Exception e) {
            Span errorMsg = new Span("Error loading content: " + e.getMessage());
            contentView.add(errorMsg);
        }
        
        Button closeButton = new Button("Close", e -> viewDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        VerticalLayout layout = new VerticalLayout(title, contentView, closeButton);
        layout.setSizeFull();
        
        viewDialog.add(layout);
        viewDialog.open();
    }
    
    private void openPluginDialog(Document document, PluginInfoDTO pluginInfo) {
        PluginExecutionDialog pluginDialog = new PluginExecutionDialog(
            document,
            pluginInfo,
            pluginService,
            response -> {
                PluginResultDialog resultDialog = new PluginResultDialog(pluginInfo.getDescription(), response);
                resultDialog.open();
            }
        );
        pluginDialog.open();
    }
    
    private void openSimilarityDialog(Document document) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("70vh");
        
        H2 title = new H2("Similar Documents");
        Span description = new Span("Finding documents similar to: " + document.getName());
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.add(new Span("Calculating similarity..."));
        
        dialog.add(new VerticalLayout(title, description, new Hr(), loadingLayout));
        dialog.open();
        
        // Find similar documents asynchronously
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            return similarityService.findSimilar(document, 10);
        }).thenAccept(results -> {
            dialog.getUI().ifPresent(ui -> ui.access(() -> {
                loadingLayout.removeAll();
                
                if (results.isEmpty()) {
                    loadingLayout.add(new Span("No similar documents found. Ensure embeddings are generated."));
                } else {
                    Grid<DocumentSimilarityService.SimilarityResult> resultsGrid = new Grid<>();
                    resultsGrid.setHeight("400px");
                    
                    resultsGrid.addColumn(result -> result.getDocument().getName())
                        .setHeader("Document Name")
                        .setAutoWidth(true)
                        .setFlexGrow(1);
                    
                    resultsGrid.addColumn(result -> String.format("%.2f%%", result.getSimilarity() * 100))
                        .setHeader("Similarity Score")
                        .setAutoWidth(true);
                    
                    resultsGrid.setItems(results);
                    loadingLayout.add(resultsGrid);
                }
                
                Button closeButton = new Button("Close", e -> dialog.close());
                closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                loadingLayout.add(closeButton);
                
                ui.push();
            }));
        });
    }
    
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
        
        // Perform extraction asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return fieldExtractionService.extractFieldsFromDocument(document.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(suggestions -> {
            dialog.getUI().ifPresent(ui -> ui.access(() -> {
                dialog.removeAll();
                showFieldSuggestions(dialog, document, suggestions);
                ui.push();
            }));
        }).exceptionally(ex -> {
            dialog.getUI().ifPresent(ui -> ui.access(() -> {
                dialog.close();
                String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                Notification.show("Failed to extract fields: " + errorMsg, 
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ui.push();
            }));
            return null;
        });
    }
    
    private void showFieldSuggestions(Dialog dialog, Document document, FieldSuggestionDTO suggestions) {
        H2 title = new H2("Field Suggestions: " + document.getName());
        
        Span helpText = new Span("Select which AI-suggested fields to apply to the document:");
        helpText.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-m)")
            .set("margin-bottom", "10px");
        
        VerticalLayout comparisonLayout = new VerticalLayout();
        comparisonLayout.setPadding(false);
        comparisonLayout.setSpacing(true);
        comparisonLayout.getStyle().set("overflow-y", "auto");
        
        Map<String, com.vaadin.flow.component.checkbox.Checkbox> checkboxMap = new HashMap<>();
        
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
            
            H3 typeSpecificHeader = new H3("Type-Specific Fields");
            typeSpecificHeader.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
            comparisonLayout.add(typeSpecificHeader);
            
            suggestions.getSuggestedFields().getTypeSpecificFields().forEach((fieldName, suggestedValue) -> {
                Object currentValue = null;
                if (suggestions.getCurrentFields().getTypeSpecificFields() != null) {
                    currentValue = suggestions.getCurrentFields().getTypeSpecificFields().get(fieldName);
                }
                
                String currentStr = currentValue != null ? currentValue.toString() : "(none)";
                String suggestedStr = suggestedValue != null ? suggestedValue.toString() : "(none)";
                
                checkboxMap.put(fieldName, addFieldComparison(
                    comparisonLayout,
                    fieldName,
                    currentStr,
                    suggestedStr
                ));
            });
        }
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        Button applyButton = new Button("Apply Selected", new Icon(VaadinIcon.CHECK), e -> {
            try {
                Map<String, Boolean> fieldsToApply = new HashMap<>();
                checkboxMap.forEach((field, checkbox) -> {
                    fieldsToApply.put(field, checkbox.getValue());
                });
                
                fieldExtractionService.applyFieldSuggestions(
                    document.getId(),
                    fieldsToApply,
                    suggestions.getSuggestedFields()
                );
                
                Notification.show("Fields applied successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                dialog.close();
                
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
    
    private com.vaadin.flow.component.checkbox.Checkbox addFieldComparison(VerticalLayout container, String fieldName, 
                                       String currentValue, String suggestedValue) {
        VerticalLayout fieldLayout = new VerticalLayout();
        fieldLayout.setPadding(false);
        fieldLayout.setSpacing(false);
        fieldLayout.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "4px")
            .set("padding", "10px")
            .set("background-color", "var(--lumo-contrast-5pct)");
        
        com.vaadin.flow.component.checkbox.Checkbox checkbox = new com.vaadin.flow.component.checkbox.Checkbox(fieldName);
        checkbox.getStyle().set("font-weight", "bold");
        
        HorizontalLayout currentRow = new HorizontalLayout();
        currentRow.setSpacing(true);
        Span currentLabel = new Span("Current:");
        currentLabel.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-weight", "600")
            .set("min-width", "100px");
        Span currentText = new Span(currentValue != null && !currentValue.isEmpty() ? currentValue : "(empty)");
        currentText.getStyle()
            .set("font-style", currentValue == null || currentValue.isEmpty() ? "italic" : "normal");
        currentRow.add(currentLabel, currentText);
        
        HorizontalLayout suggestedRow = new HorizontalLayout();
        suggestedRow.setSpacing(true);
        Span suggestedLabel = new Span("Suggested:");
        suggestedLabel.getStyle()
            .set("color", "var(--lumo-primary-color)")
            .set("font-weight", "600")
            .set("min-width", "100px");
        Span suggestedText = new Span(suggestedValue != null && !suggestedValue.isEmpty() ? suggestedValue : "(empty)");
        suggestedText.getStyle()
            .set("font-style", suggestedValue == null || suggestedValue.isEmpty() ? "italic" : "normal")
            .set("color", "var(--lumo-primary-text-color)");
        suggestedRow.add(suggestedLabel, suggestedText);
        
        fieldLayout.add(checkbox, currentRow, suggestedRow);
        container.add(fieldLayout);
        
        // Auto-select if there's a suggested value and current is empty
        if ((currentValue == null || currentValue.isEmpty()) && 
            (suggestedValue != null && !suggestedValue.isEmpty())) {
            checkbox.setValue(true);
        }
        
        return checkbox;
    }
}
