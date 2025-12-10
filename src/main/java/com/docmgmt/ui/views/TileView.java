package com.docmgmt.ui.views;

import com.docmgmt.dto.TileDTO;
import com.docmgmt.model.Document;
import com.docmgmt.model.Folder;
import com.docmgmt.model.TileConfiguration;
import com.docmgmt.model.User;
import com.docmgmt.model.SysObject;
import com.docmgmt.plugin.PluginService;
import com.docmgmt.service.*;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.docmgmt.ui.MainLayout;
import com.docmgmt.ui.components.DocumentDetailDialog;
import com.docmgmt.ui.components.DocumentEditDialog;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
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
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tile view for displaying documents as tiles
 * Accessible at /tiles/{folderName}
 */
@Route(value = "tiles", layout = MainLayout.class)
@PageTitle("Tile View | Document Management System")
public class TileView extends VerticalLayout implements HasUrlParameter<String> {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TileView.class);
    
    private final TileService tileService;
    private final FolderService folderService;
    private final DocumentService documentService;
    private final UserService userService;
    private final ContentService contentService;
    private final PluginService pluginService;
    private final DocumentSimilarityService similarityService;
    private final DocumentFieldExtractionService fieldExtractionService;
    private final FileStoreService fileStoreService;
    
    private VerticalLayout contentLayout;
    private String folderName;
    private TileConfiguration config;
    
    @Autowired
    public TileView(TileService tileService, FolderService folderService,
                   DocumentService documentService, UserService userService,
                   ContentService contentService, PluginService pluginService,
                   DocumentSimilarityService similarityService,
                   DocumentFieldExtractionService fieldExtractionService,
                   FileStoreService fileStoreService) {
        this.tileService = tileService;
        this.folderService = folderService;
        this.documentService = documentService;
        this.userService = userService;
        this.contentService = contentService;
        this.pluginService = pluginService;
        this.similarityService = similarityService;
        this.fieldExtractionService = fieldExtractionService;
        this.fileStoreService = fileStoreService;
        
        addClassName("tile-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        
        add(contentLayout);
    }
    
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null || parameter.trim().isEmpty()) {
            showError("No folder specified");
            return;
        }
        
        this.folderName = parameter;
        loadTiles();
    }
    
    private void loadTiles() {
        try {
            contentLayout.removeAll();
            
            // Get folder
            List<Folder> folders = folderService.findByNameForTileDisplay(folderName);
            if (folders.isEmpty()) {
                showError("Folder not found: " + folderName);
                return;
            }
            
            Folder folder = folders.get(0);
            config = tileService.getConfiguration(folder.getId());
            List<TileDTO> tiles = tileService.getTilesByFolderName(folderName);
            
            // Hide navigation if configured
            if (config.getHideNavigation() != null && config.getHideNavigation()) {
                getUI().ifPresent(ui -> ui.getElement().executeJs(
                    "const appLayout = this.closest('vaadin-app-layout');" +
                    "if (appLayout) { appLayout.drawerOpened = false; appLayout.primarySection = 'navbar'; }"
                ));
            }
            
            // Header
            HorizontalLayout header = createHeader(folder);
            contentLayout.add(header);
            
            // Tiles
            if (tiles.isEmpty()) {
                Span emptyMessage = new Span("No documents found in this folder.");
                emptyMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
                contentLayout.add(emptyMessage);
            } else {
                VerticalLayout tilesContainer = createTilesContainer(tiles);
                contentLayout.add(tilesContainer);
            }
            
        } catch (Exception e) {
            logger.error("Error loading tiles for folder: {}", folderName, e);
            showError("Error loading tiles: " + e.getMessage());
        }
    }
    
    private HorizontalLayout createHeader(Folder folder) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(false);
        
        H2 title = new H2(folder.getName());
        title.getStyle().set("margin", "0");
        
        Button configButton = new Button("Configure", new Icon(VaadinIcon.COG));
        configButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        configButton.addClickListener(e -> navigateToConfig());
        
        Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadTiles());
        
        header.add(title);
        header.add(configButton, refreshButton);
        header.setFlexGrow(1, title);
        
        if (folder.getDescription() != null && !folder.getDescription().isEmpty()) {
            Paragraph description = new Paragraph(folder.getDescription());
            description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "0.5rem")
                .set("margin-bottom", "1rem");
            
            VerticalLayout headerContainer = new VerticalLayout(header, description);
            headerContainer.setPadding(false);
            headerContainer.setSpacing(false);
            
            return new HorizontalLayout(headerContainer);
        }
        
        return header;
    }
    
    private VerticalLayout createTilesContainer(List<TileDTO> tiles) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);
        container.setWidthFull();
        
        if (config.getGroupBySubfolder()) {
            // Group tiles by subfolder
            Map<String, List<TileDTO>> groupedTiles = tiles.stream()
                .collect(Collectors.groupingBy(
                    tile -> tile.getGroupName() != null ? tile.getGroupName() : "Other"
                ));
            
            for (Map.Entry<String, List<TileDTO>> entry : groupedTiles.entrySet()) {
                String groupName = entry.getKey();
                List<TileDTO> groupTiles = entry.getValue();
                
                // Group header
                H3 groupHeader = new H3(groupName);
                groupHeader.getStyle().set("margin-top", "1rem");
                container.add(groupHeader);
                
                // Group tiles
                HorizontalLayout tileGrid = createTileGrid(groupTiles);
                container.add(tileGrid);
            }
        } else {
            // No grouping - just display all tiles
            HorizontalLayout tileGrid = createTileGrid(tiles);
            container.add(tileGrid);
        }
        
        return container;
    }
    
    private HorizontalLayout createTileGrid(List<TileDTO> tiles) {
        HorizontalLayout grid = new HorizontalLayout();
        grid.setWidthFull();
        grid.getStyle()
            .set("flex-wrap", "wrap")
            .set("gap", "1rem");
        
        for (TileDTO tile : tiles) {
            VerticalLayout tileCard = createTileCard(tile);
            grid.add(tileCard);
        }
        
        return grid;
    }
    
    private VerticalLayout createTileCard(TileDTO tile) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("tile-card");
        card.setPadding(true);
        card.setSpacing(true);
        
        // Size based on configuration
        int width = getTileWidth();
        card.setWidth(width, Unit.PIXELS);
        card.setMinHeight(150, Unit.PIXELS);
        
        card.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("cursor", "pointer")
            .set("transition", "all 0.2s");
        
        // Apply color if specified
        if (tile.getColor() != null) {
            // Wider left border
            card.getStyle()
                .set("border-left", "8px solid " + tile.getColor());
            
            // Add subtle background tint with configured opacity
            double opacity = (config.getBackgroundColorOpacity() != null) 
                ? config.getBackgroundColorOpacity() 
                : 0.05; // default
            String rgbaBackground = convertHexToRgba(tile.getColor(), opacity);
            card.getStyle()
                .set("background", "linear-gradient(to right, " + rgbaBackground + " 0%, var(--lumo-base-color) 100%)");
        } else {
            card.getStyle()
                .set("background", "var(--lumo-base-color)");
        }
        
        // Hover effect
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().remove("box-shadow");
        });
        
        // Title
        H4 title = new H4(tile.getName());
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "1.1rem");
        card.add(title);
        
        // Description
        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            Paragraph description = new Paragraph(tile.getDescription());
            description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9rem")
                .set("margin", "0.5rem 0")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "3")
                .set("-webkit-box-orient", "vertical");
            card.add(description);
        }
        
        // Tags
        if (tile.getTags() != null && !tile.getTags().isEmpty()) {
            HorizontalLayout tagsLayout = new HorizontalLayout();
            tagsLayout.setSpacing(true);
            tagsLayout.getStyle().set("flex-wrap", "wrap");
            
            for (String tag : tile.getTags()) {
                Span tagBadge = new Span(tag);
                tagBadge.getStyle()
                    .set("padding", "0.25rem 0.5rem")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("font-size", "0.8rem");
                tagsLayout.add(tagBadge);
            }
            
            card.add(tagsLayout);
        }
        
        // Links
        HorizontalLayout linksLayout = new HorizontalLayout();
        linksLayout.setSpacing(true);
        linksLayout.setWidthFull();
        linksLayout.getStyle()
            .set("margin-top", "auto")
            .set("flex-wrap", "wrap")
            .set("gap", "0.5rem");
        
        if (config.getShowDetailLink() && (config.getHideEditButtons() == null || !config.getHideEditButtons())) {
            Button viewButton = new Button("View", new Icon(VaadinIcon.EYE));
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewButton.addClickListener(e -> {
                // Load document and open dialog
                Document doc = documentService.findById(tile.getId());
                if (doc != null) {
                    openDocumentDetailDialog(doc, false);
                }
            });
            
            Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> {
                // Load document and open dialog in edit mode
                Document doc = documentService.findById(tile.getId());
                if (doc != null) {
                    openDocumentDetailDialog(doc, true);
                }
            });
            
            Button viewContentButton = new Button("Content", new Icon(VaadinIcon.FILE_TEXT_O));
            viewContentButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewContentButton.addClickListener(e -> {
                // Load document and open content viewer
                Document doc = documentService.findById(tile.getId());
                if (doc != null) {
                    openContentViewerDialog(doc);
                }
            });
            
            linksLayout.add(viewButton, editButton, viewContentButton);
        }
        
        if (config.getShowUrlLink() && tile.getUrl() != null && !tile.getUrl().isEmpty()) {
            Button urlButton = new Button("Open URL", new Icon(VaadinIcon.EXTERNAL_LINK));
            urlButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            urlButton.addClickListener(e -> 
                getUI().ifPresent(ui -> ui.getPage().open(tile.getUrl(), "_blank")));
            linksLayout.add(urlButton);
        }
        
        card.add(linksLayout);
        
        return card;
    }
    
    private int getTileWidth() {
        if (config == null || config.getTileSize() == null) {
            return 300; // Default medium
        }
        
        switch (config.getTileSize()) {
            case SMALL:
                return 250;
            case LARGE:
                return 400;
            case MEDIUM:
            default:
                return 300;
        }
    }
    
    private void navigateToConfig() {
        getUI().ifPresent(ui -> ui.navigate("tile-config/" + folderName));
    }
    
    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        
        contentLayout.removeAll();
        H3 errorTitle = new H3("Error");
        Paragraph errorMessage = new Paragraph(message);
        contentLayout.add(errorTitle, errorMessage);
    }
    
    /**
     * Convert hex color to rgba with specified opacity
     * @param hex Hex color (e.g., "#FF6B6B")
     * @param opacity Opacity value between 0 and 1
     * @return rgba color string (e.g., "rgba(255, 107, 107, 0.1)")
     */
    private String convertHexToRgba(String hex, double opacity) {
        // Remove # if present
        hex = hex.replace("#", "");
        
        // Parse RGB values
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        
        return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, opacity);
    }
    
    /**
     * Open document detail dialog with content viewing and optional edit mode
     * Uses the same dialog approach as FolderView for consistency
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
        
        // Edit mode uses reusable DocumentEditDialog
        DocumentEditDialog editDialog = new DocumentEditDialog(
            document,
            documentService,
            userService,
            doc -> loadTiles() // Refresh tiles after save
        );
        editDialog.open();
    }
    
    /**
     * Open a dialog to view document content
     */
    private void openContentViewerDialog(Document document) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1000px");
        dialog.setHeight("80vh");
        
        H2 title = new H2("Content: " + document.getName());
        
        // Reload document with contents
        Document reloadedDoc = documentService.findById(document.getId());
        if (reloadedDoc.getContents() != null) {
            reloadedDoc.getContents().size(); // Force initialization
        }
        
        // Create split layout: content list on left, viewer on right
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        splitLayout.setSplitterPosition(35); // 35% for list, 65% for viewer
        
        // Left side: Content list
        VerticalLayout contentListLayout = new VerticalLayout();
        contentListLayout.setPadding(false);
        contentListLayout.setSpacing(false);
        
        H3 listTitle = new H3("Content Renditions");
        listTitle.getStyle().set("margin", "0 0 1rem 0");
        contentListLayout.add(listTitle);
        
        // Right side: Content viewer
        VerticalLayout viewerLayout = new VerticalLayout();
        viewerLayout.setPadding(false);
        viewerLayout.setSpacing(true);
        viewerLayout.setSizeFull();
        
        H3 viewerTitle = new H3("Content Viewer");
        viewerTitle.getStyle().set("margin", "0 0 1rem 0");
        
        // Create a div to hold the viewer component (will be replaced dynamically)
        Div viewerContainer = new Div();
        viewerContainer.setWidthFull();
        viewerContainer.setHeight("100%");
        viewerContainer.getStyle()
            .set("overflow", "auto")
            .set("flex-grow", "1");
        
        Span placeholder = new Span("Select a content rendition to view");
        placeholder.getStyle().set("color", "var(--lumo-secondary-text-color)");
        viewerContainer.add(placeholder);
        
        viewerLayout.add(viewerTitle, viewerContainer);
        
        // Check if document has contents
        if (reloadedDoc.getContents() == null || reloadedDoc.getContents().isEmpty()) {
            Span noContent = new Span("This document has no content renditions.");
            noContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
            contentListLayout.add(noContent);
            viewerContainer.removeAll();
            Span emptyMessage = new Span("No content available for this document.");
            emptyMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
            viewerContainer.add(emptyMessage);
        } else {
            // Create clickable content items
            for (com.docmgmt.model.Content content : reloadedDoc.getContents()) {
                VerticalLayout contentItem = new VerticalLayout();
                contentItem.setPadding(true);
                contentItem.setSpacing(false);
                contentItem.getStyle()
                    .set("cursor", "pointer")
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("transition", "background 0.2s");
                
                Span contentName = new Span(content.getName());
                contentName.getStyle().set("font-weight", "bold");
                
                Span contentInfo = new Span(
                    (content.isPrimary() ? "Primary" : "Secondary") + " • " + 
                    (content.getContentType() != null ? content.getContentType() : "unknown")
                );
                contentInfo.getStyle()
                    .set("font-size", "0.875rem")
                    .set("color", "var(--lumo-secondary-text-color)");
                
                contentItem.add(contentName, contentInfo);
                
                // Hover effect
                contentItem.getElement().addEventListener("mouseenter", e -> {
                    contentItem.getStyle().set("background", "var(--lumo-contrast-5pct)");
                });
                contentItem.getElement().addEventListener("mouseleave", e -> {
                    contentItem.getStyle().remove("background");
                });
                
                // Click to view
                contentItem.addClickListener(e -> {
                    try {
                        byte[] bytes = contentService.getContentBytes(content.getId());
                        viewerContainer.removeAll();
                        viewerTitle.setText("Viewing: " + content.getName());
                        
                        if (bytes != null && content.getContentType() != null) {
                            String contentType = content.getContentType();
                            
                            // PDF viewer
                            if (contentType.equals("application/pdf")) {
                                // Use Blob URL approach for cross-browser compatibility (Chrome/Safari)
                                String base64Pdf = java.util.Base64.getEncoder().encodeToString(bytes);
                                Div pdfViewer = new Div();
                                pdfViewer.setId("pdf-viewer-" + content.getId());
                                pdfViewer.setWidthFull();
                                pdfViewer.setHeight("600px");
                                viewerContainer.add(pdfViewer);
                                
                                // Create Blob URL and load PDF in iframe via JavaScript
                                pdfViewer.getElement().executeJs(
                                    "const base64Data = $0;" +
                                    "const binaryString = atob(base64Data);" +
                                    "const bytes = new Uint8Array(binaryString.length);" +
                                    "for (let i = 0; i < binaryString.length; i++) {" +
                                    "  bytes[i] = binaryString.charCodeAt(i);" +
                                    "}" +
                                    "const blob = new Blob([bytes], { type: 'application/pdf' });" +
                                    "const url = URL.createObjectURL(blob);" +
                                    "this.innerHTML = '<iframe src=\"' + url + '\" style=\"width: 100%; height: 100%; border: 1px solid var(--lumo-contrast-10pct);\" />';" +
                                    "this.querySelector('iframe').onload = function() {" +
                                    "  setTimeout(() => URL.revokeObjectURL(url), 1000);" +
                                    "};",
                                    base64Pdf
                                );
                            }
                            // Markdown viewer
                            else if (contentType.equals("text/markdown") || 
                                     content.getName().toLowerCase().endsWith(".md")) {
                                String markdownText = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                                Div markdownViewer = new Div();
                                markdownViewer.getElement().setProperty("innerHTML", convertMarkdownToHtml(markdownText));
                                markdownViewer.getStyle()
                                    .set("padding", "1rem")
                                    .set("background", "var(--lumo-contrast-5pct)")
                                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                                    .set("border-radius", "4px")
                                    .set("overflow", "auto")
                                    .set("max-height", "600px");
                                viewerContainer.add(markdownViewer);
                            }
                            // Text viewer for all text/* types and JSON
                            else if (contentType.startsWith("text/") || 
                                     contentType.equals("application/json")) {
                                String contentText = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                                TextArea textViewer = new TextArea();
                                textViewer.setValue(contentText);
                                textViewer.setWidthFull();
                                textViewer.setHeight("600px");
                                textViewer.setReadOnly(true);
                                textViewer.getStyle().set("font-family", "monospace");
                                viewerContainer.add(textViewer);
                            }
                            // Binary content info
                            else {
                                Div infoDiv = new Div();
                                infoDiv.getElement().setProperty("innerHTML",
                                    "<div style='padding: 1rem; background: var(--lumo-contrast-5pct); border: 1px solid var(--lumo-contrast-10pct); border-radius: 4px;'>" +
                                    "<h4 style='margin-top: 0;'>Binary Content Information</h4>" +
                                    "<p><strong>Name:</strong> " + content.getName() + "</p>" +
                                    "<p><strong>Type:</strong> " + contentType + "</p>" +
                                    "<p><strong>Size:</strong> " + bytes.length + " bytes</p>" +
                                    "<p><strong>Rendition:</strong> " + (content.isPrimary() ? "Primary" : "Secondary") + "</p>" +
                                    "<p style='color: var(--lumo-secondary-text-color);'>[This is binary content and cannot be displayed]</p>" +
                                    "</div>"
                                );
                                viewerContainer.add(infoDiv);
                            }
                        } else {
                            Span errorMessage = new Span("No content data available");
                            errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
                            viewerContainer.add(errorMessage);
                        }
                    } catch (Exception ex) {
                        viewerContainer.removeAll();
                        Span errorMessage = new Span("Error loading content: " + ex.getMessage());
                        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
                        viewerContainer.add(errorMessage);
                        Notification.show("Failed to load content", 3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                
                contentListLayout.add(contentItem);
            }
        }
        
        splitLayout.addToPrimary(contentListLayout);
        splitLayout.addToSecondary(viewerLayout);
        
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(closeButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), splitLayout, buttonLayout
        );
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    /**
     * Minimal Markdown to HTML converter for headings, bold/italic, code blocks, and links.
     * This avoids adding heavy dependencies; adjust as needed for richer features.
     */
    private String convertMarkdownToHtml(String md) {
        if (md == null || md.isEmpty()) return "";
        String html = md;
        // Escape basic HTML first
        html = html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        // Code blocks ```
        html = html.replaceAll("(?s)```(.*?)```", "<pre><code>$1</code></pre>");
        // Inline code `code`
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");
        // Headings # to ######
        html = html.replaceAll("(?m)^######\\s+(.*)$", "<h6>$1</h6>");
        html = html.replaceAll("(?m)^#####\\s+(.*)$", "<h5>$1</h5>");
        html = html.replaceAll("(?m)^####\\s+(.*)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^###\\s+(.*)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^##\\s+(.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^#\\s+(.*)$", "<h1>$1</h1>");
        // Bold **text** or __text__
        html = html.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("__([^_]+)__", "<strong>$1</strong>");
        // Italic *text* or _text_
        html = html.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "<em>$1</em>");
        html = html.replaceAll("_([^_]+)_", "<em>$1</em>");
        // Links [text](url)
        html = html.replaceAll("\\[([^]]+)\\]\\(([^)]+)\\)", "<a href='$2' target='_blank' rel='noopener'>$1</a>");
        // Paragraphs: wrap lines separated by blank line
        html = html.replaceAll("(?m)^(?!<h\\d>|<pre>|</pre>|<ul>|<ol>|<li>|<p>|</p>|<blockquote>)([^\n]+)$", "<p>$1</p>");
        return html;
    }
}
