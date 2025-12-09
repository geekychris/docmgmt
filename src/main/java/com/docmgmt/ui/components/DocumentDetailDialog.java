package com.docmgmt.ui.components;

import com.docmgmt.dto.FieldSuggestionDTO;
import com.docmgmt.dto.PluginInfoDTO;
import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.User;
import com.docmgmt.plugin.PluginResponse;
import com.docmgmt.plugin.PluginService;
import com.docmgmt.service.*;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentDetailDialog.class);
    
    private final Document document;
    private final DocumentService documentService;
    private final UserService userService;
    private final ContentService contentService;
    private final PluginService pluginService;
    private final DocumentSimilarityService similarityService;
    private final DocumentFieldExtractionService fieldExtractionService;
    
    // UI components that need to be refreshed
    private Grid<Content> contentGrid;
    private ComboBox<Content> contentSelector;
    
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
        logger.info("========== DocumentDetailDialog.initializeDialog() START ==========");
        logger.info("Document ID: {}, Name: {}", document.getId(), document.getName());
        
        setWidth("900px");
        setHeight("80vh");
        
        // Reload document with contents
        Document reloadedDoc = documentService.findById(document.getId());
        logger.info("Reloaded document, ID: {}", reloadedDoc.getId());
        if (reloadedDoc.getContents() != null) {
            reloadedDoc.getContents().size(); // Force initialization
        }
        
        // Reload content objects to ensure they're managed
        List<Content> managedContents = new java.util.ArrayList<>();
        if (reloadedDoc.getContents() != null) {
            for (Content c : reloadedDoc.getContents()) {
                Content managedContent = contentService.findById(c.getId());
                if (managedContent != null) {
                    managedContents.add(managedContent);
                }
            }
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
        
        // Content selector for AI operations
        contentSelector = new ComboBox<>("Select content for AI operations");
        logger.info("Creating content selector. managedContents size: {}", managedContents.size());
        
        if (!managedContents.isEmpty()) {
            // Filter to only text content
            List<Content> textContents = managedContents.stream()
                .filter(c -> c.getContentType() != null && c.getContentType().startsWith("text/"))
                .collect(java.util.stream.Collectors.toList());
            
            logger.info("Text contents found: {}", textContents.size());
            
            if (!textContents.isEmpty()) {
                contentSelector.setItems(textContents);
                contentSelector.setItemLabelGenerator(c -> {
                    String rendition;
                    if (c.isPrimary()) {
                        rendition = "Primary";
                    } else if (c.getParentRendition() != null) {
                        rendition = "Secondary (from: " + c.getParentRendition().getName() + ")";
                    } else {
                        rendition = "Secondary";
                    }
                    return String.format("%s (%s, %s)", c.getName(), c.getContentType(), rendition);
                });
                // Default to first text content
                Content defaultContent = textContents.get(0);
                contentSelector.setValue(defaultContent);
                logger.info("Set default content - ID: {}, Name: {}", defaultContent.getId(), defaultContent.getName());
                logger.info("ContentSelector value after set: {}", 
                    contentSelector.getValue() != null ? contentSelector.getValue().getName() : "NULL");
                contentSelector.setWidthFull();
                contentSelector.getStyle().set("margin-bottom", "10px");
            } else {
                contentSelector.setEnabled(false);
                contentSelector.setPlaceholder("No text content available");
                contentSelector.setWidthFull();
                contentSelector.getStyle().set("margin-bottom", "10px");
            }
        } else {
            contentSelector.setEnabled(false);
            contentSelector.setPlaceholder("No content available");
            contentSelector.setWidthFull();
            contentSelector.getStyle().set("margin-bottom", "10px");
        }
        
        contentGrid = new Grid<>(Content.class, false);
        contentGrid.setHeight("200px");
        
        contentGrid.addColumn(Content::getName)
            .setHeader("Name").setResizable(true).setAutoWidth(true).setFlexGrow(1);
        contentGrid.addColumn(content -> content.getContentType() != null ? content.getContentType() : "-")
            .setHeader("Type").setResizable(true).setAutoWidth(true);
        contentGrid.addColumn(content -> {
            if (content.isPrimary()) {
                return "Primary";
            } else {
                // Show parent rendition if available
                if (content.getParentRendition() != null) {
                    return "Secondary (from: " + content.getParentRendition().getName() + ")";
                }
                return "Secondary";
            }
        }).setHeader("Rendition").setResizable(true).setAutoWidth(true);
        
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
            Content selectedContent = contentSelector.getValue();
            logger.info("Extract fields clicked. Selected content: {}", 
                selectedContent != null ? selectedContent.getName() : "NULL");
            // Don't close the dialog - let user see the document details
            openFieldExtractionDialog(reloadedDoc, selectedContent);
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
                    Content selectedContent = contentSelector.getValue();
                    // Debug: log selected content
                    logger.info("Plugin menu clicked. ContentSelector enabled: {}", contentSelector.isEnabled());
                    logger.info("ContentSelector has items: {}", contentSelector.getListDataView().getItemCount());
                    if (selectedContent != null) {
                        logger.info("Selected content - ID: {}, Name: {}", selectedContent.getId(), selectedContent.getName());
                    } else {
                        logger.info("Selected content is NULL");
                        logger.info("Checking if selector has value (isEmpty): {}", contentSelector.isEmpty());
                    }
                    // Don't close the dialog - let user see the document details while plugin dialog is open
                    openPluginDialog(reloadedDoc, pluginInfo, selectedContent);
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
            contentSelector,
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
            
            // Add download/open link at the top for all content types
            com.vaadin.flow.server.StreamResource streamResource = 
                new com.vaadin.flow.server.StreamResource(
                    content.getName(),
                    () -> new java.io.ByteArrayInputStream(contentBytes)
                );
            if (contentType != null) {
                streamResource.setContentType(contentType);
            }
            
            com.vaadin.flow.component.html.Anchor downloadLink = 
                new com.vaadin.flow.component.html.Anchor(streamResource, "Download/Open in New Tab");
            downloadLink.setTarget("_blank");
            downloadLink.getElement().setAttribute("download", content.getName());
            downloadLink.getStyle()
                .set("display", "inline-block")
                .set("margin-bottom", "15px")
                .set("padding", "8px 16px")
                .set("background-color", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)")
                .set("text-decoration", "none")
                .set("border-radius", "4px");
            
            contentView.add(downloadLink);
            
            if (contentType != null && contentType.startsWith("text/")) {
                String textContent = new String(contentBytes, java.nio.charset.StandardCharsets.UTF_8);
                
                // Check if it's markdown
                if (contentType.equals("text/markdown") || content.getName().endsWith(".md")) {
                    // Render markdown as HTML
                    com.vaadin.flow.component.html.Div markdownDiv = new com.vaadin.flow.component.html.Div();
                    markdownDiv.getElement().setProperty("innerHTML", convertMarkdownToHtml(textContent));
                    markdownDiv.getStyle()
                        .set("padding", "20px")
                        .set("overflow", "auto");
                    contentView.add(markdownDiv);
                } else {
                    // Plain text rendering
                    com.vaadin.flow.component.html.Pre pre = new com.vaadin.flow.component.html.Pre(textContent);
                    pre.getStyle()
                        .set("white-space", "pre-wrap")
                        .set("font-family", "monospace")
                        .set("padding", "10px");
                    contentView.add(pre);
                }
            } else if (contentType != null && contentType.equals("application/pdf")) {
                // Display PDF using iframe with base64 data URI
                String base64 = java.util.Base64.getEncoder().encodeToString(contentBytes);
                String dataUri = "data:application/pdf;base64," + base64;
                
                com.vaadin.flow.component.html.IFrame iframe = new com.vaadin.flow.component.html.IFrame(dataUri);
                iframe.setSizeFull();
                iframe.getStyle()
                    .set("border", "none")
                    .set("min-height", "600px");
                contentView.add(iframe);
            } else if (contentType != null && isExcelFile(contentType)) {
                // Display Excel file as HTML table
                String htmlTable = convertExcelToHtmlTable(contentBytes, contentType);
                com.vaadin.flow.component.html.Div excelDiv = new com.vaadin.flow.component.html.Div();
                excelDiv.getElement().setProperty("innerHTML", htmlTable);
                excelDiv.getStyle()
                    .set("overflow", "auto")
                    .set("padding", "10px");
                contentView.add(excelDiv);
            } else if (contentType != null && isVideoFile(contentType)) {
                // Display video using HTML5 video player
                String base64 = java.util.Base64.getEncoder().encodeToString(contentBytes);
                String dataUri = "data:" + contentType + ";base64," + base64;
                
                // Create video element using HTML
                com.vaadin.flow.component.html.Div videoContainer = new com.vaadin.flow.component.html.Div();
                videoContainer.getElement().setProperty("innerHTML", 
                    "<video controls style='width: 100%; max-height: 70vh;'>" +
                    "<source src='" + dataUri + "' type='" + contentType + "'>" +
                    "Your browser does not support the video tag." +
                    "</video>");
                videoContainer.getStyle().set("text-align", "center");
                contentView.add(videoContainer);
            } else if (contentType != null && isImageFile(contentType)) {
                // Display image
                String base64 = java.util.Base64.getEncoder().encodeToString(contentBytes);
                String dataUri = "data:" + contentType + ";base64," + base64;
                
                com.vaadin.flow.component.html.Image image = new com.vaadin.flow.component.html.Image(dataUri, content.getName());
                image.setMaxWidth("100%");
                image.getStyle().set("display", "block").set("margin", "auto");
                contentView.add(image);
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
    
    /**
     * Simple markdown to HTML converter
     * Handles basic markdown syntax: headers, bold, italic, links, lists, code blocks
     */
    private String convertMarkdownToHtml(String markdown) {
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
                    html.append("</pre></code>");
                    inCodeBlock = false;
                } else {
                    html.append("<code><pre style='background-color: #f5f5f5; padding: 10px; border-radius: 4px;'>");
                    inCodeBlock = true;
                }
                continue;
            }
            
            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }
            
            // Headers
            if (line.startsWith("# ")) {
                html.append("<h1>").append(escapeHtml(line.substring(2))).append("</h1>");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(escapeHtml(line.substring(3))).append("</h2>");
            } else if (line.startsWith("### ")) {
                html.append("<h3>").append(escapeHtml(line.substring(4))).append("</h3>");
            } else if (line.startsWith("#### ")) {
                html.append("<h4>").append(escapeHtml(line.substring(5))).append("</h4>");
            } else if (line.startsWith("##### ")) {
                html.append("<h5>").append(escapeHtml(line.substring(6))).append("</h5>");
            } else if (line.startsWith("###### ")) {
                html.append("<h6>").append(escapeHtml(line.substring(7))).append("</h6>");
            } 
            // Lists
            else if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(processInlineMarkdown(line.trim().substring(2))).append("</li>");
            } else {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                
                if (line.trim().isEmpty()) {
                    html.append("<br>");
                } else {
                    html.append("<p>").append(processInlineMarkdown(line)).append("</p>");
                }
            }
        }
        
        if (inCodeBlock) {
            html.append("</pre></code>");
        }
        if (inList) {
            html.append("</ul>");
        }
        
        return html.toString();
    }
    
    /**
     * Process inline markdown: bold, italic, code, links
     */
    private String processInlineMarkdown(String text) {
        text = escapeHtml(text);
        
        // Bold: **text** or __text__
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("__(.+?)__", "<strong>$1</strong>");
        
        // Italic: *text* or _text_
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        text = text.replaceAll("_(.+?)_", "<em>$1</em>");
        
        // Inline code: `code`
        text = text.replaceAll("`(.+?)`", "<code style='background-color: #f5f5f5; padding: 2px 4px; border-radius: 3px;'>$1</code>");
        
        // Links: [text](url)
        text = text.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href='$2' target='_blank'>$1</a>");
        
        return text;
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
    
    /**
     * Check if content type is an Excel file
     */
    private boolean isExcelFile(String contentType) {
        String type = contentType.toLowerCase();
        return type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
               type.equals("application/vnd.ms-excel") ||
               type.equals("application/excel") ||
               type.equals("application/x-excel") ||
               type.equals("application/x-msexcel");
    }
    
    /**
     * Check if content type is a video file
     */
    private boolean isVideoFile(String contentType) {
        String type = contentType.toLowerCase();
        return type.startsWith("video/") ||
               type.equals("application/x-mpegurl") ||
               type.equals("video/mp4") ||
               type.equals("video/webm") ||
               type.equals("video/ogg") ||
               type.equals("video/quicktime") ||
               type.equals("video/x-msvideo");
    }
    
    /**
     * Check if content type is an image file
     */
    private boolean isImageFile(String contentType) {
        String type = contentType.toLowerCase();
        return type.startsWith("image/");
    }
    
    /**
     * Convert Excel file to HTML table
     */
    private String convertExcelToHtmlTable(byte[] excelBytes, String contentType) {
        try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(excelBytes)) {
            org.apache.poi.ss.usermodel.Workbook workbook;
            
            if (contentType.equals("application/vnd.ms-excel")) {
                workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(inputStream);
            } else {
                workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<style>")
                .append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                .append("th { background-color: #f2f2f2; font-weight: bold; }")
                .append("tr:nth-child(even) { background-color: #f9f9f9; }")
                .append(".sheet-header { font-size: 18px; font-weight: bold; margin: 20px 0 10px 0; }")
                .append("</style>");
            
            org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();
            
            // Iterate through all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                
                // Add sheet name
                html.append("<div class='sheet-header'>Sheet: ")
                    .append(escapeHtml(sheet.getSheetName()))
                    .append("</div>");
                
                // Start table
                html.append("<table>");
                
                boolean firstRow = true;
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    html.append("<tr>");
                    
                    // Use first row as header
                    String cellTag = firstRow ? "th" : "td";
                    
                    int lastCellNum = row.getLastCellNum();
                    for (int cellNum = 0; cellNum < lastCellNum; cellNum++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(cellNum);
                        String cellValue = "";
                        
                        if (cell != null) {
                            cellValue = dataFormatter.formatCellValue(cell);
                        }
                        
                        html.append("<").append(cellTag).append(">")
                            .append(escapeHtml(cellValue))
                            .append("</").append(cellTag).append(">");
                    }
                    
                    html.append("</tr>");
                    firstRow = false;
                }
                
                html.append("</table>");
            }
            
            workbook.close();
            return html.toString();
            
        } catch (Exception e) {
            return "<p style='color: red;'>Error rendering Excel file: " + escapeHtml(e.getMessage()) + "</p>";
        }
    }
    
    private void openPluginDialog(Document document, PluginInfoDTO pluginInfo, Content selectedContent) {
        PluginExecutionDialog pluginDialog = new PluginExecutionDialog(
            document,
            pluginInfo,
            pluginService,
            contentService,
            selectedContent,
            response -> {
                PluginResultDialog resultDialog = new PluginResultDialog(pluginInfo.getDescription(), response);
                resultDialog.open();
                // Refresh the document detail dialog to show new content
                refreshDialog();
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
    
    private void openFieldExtractionDialog(Document document, Content selectedContent) {
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
        Long contentId = selectedContent != null ? selectedContent.getId() : null;
        CompletableFuture.supplyAsync(() -> {
            try {
                return fieldExtractionService.extractFieldsFromDocument(document.getId(), contentId);
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
    
    /**
     * Refresh the dialog to show newly added content (e.g., after plugin execution)
     */
    private void refreshDialog() {
        // Reload document with fresh contents
        Document reloadedDoc = documentService.findById(document.getId());
        if (reloadedDoc.getContents() != null) {
            reloadedDoc.getContents().size(); // Force initialization
        }
        
        // Reload content objects to ensure they're managed
        List<Content> managedContents = new java.util.ArrayList<>();
        if (reloadedDoc.getContents() != null) {
            for (Content c : reloadedDoc.getContents()) {
                Content managedContent = contentService.findById(c.getId());
                if (managedContent != null) {
                    managedContents.add(managedContent);
                }
            }
        }
        
        // Update the content grid
        if (contentGrid != null) {
            contentGrid.setItems(reloadedDoc.getContents());
        }
        
        // Update the content selector
        if (contentSelector != null) {
            List<Content> textContents = managedContents.stream()
                .filter(c -> c.getContentType() != null && c.getContentType().startsWith("text/"))
                .collect(java.util.stream.Collectors.toList());
            
            if (!textContents.isEmpty()) {
                Content currentSelection = contentSelector.getValue();
                contentSelector.setItems(textContents);
                
                // Try to keep the same selection if it still exists, otherwise select first
                if (currentSelection != null) {
                    boolean stillExists = textContents.stream()
                        .anyMatch(c -> c.getId().equals(currentSelection.getId()));
                    if (stillExists) {
                        // Find the refreshed version of the selected content
                        textContents.stream()
                            .filter(c -> c.getId().equals(currentSelection.getId()))
                            .findFirst()
                            .ifPresent(contentSelector::setValue);
                    } else {
                        contentSelector.setValue(textContents.get(0));
                    }
                } else {
                    contentSelector.setValue(textContents.get(0));
                }
                
                contentSelector.setEnabled(true);
            } else {
                contentSelector.clear();
                contentSelector.setEnabled(false);
                contentSelector.setPlaceholder("No text content available");
            }
        }
        
        logger.info("Dialog refreshed. Content count: {}", 
            reloadedDoc.getContents() != null ? reloadedDoc.getContents().size() : 0);
    }
}
