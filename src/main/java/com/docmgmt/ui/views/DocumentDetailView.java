package com.docmgmt.ui.views;

import com.docmgmt.model.Content;
import com.docmgmt.model.Document;
import com.docmgmt.model.User;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "document", layout = MainLayout.class)
@PageTitle("Document Details | Document Management System")
public class DocumentDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final DocumentService documentService;
    private final ContentService contentService;
    
    private Document currentDocument;
    private VerticalLayout documentInfoPanel;
    private VerticalLayout contentPanel;
    private Grid<Content> contentGrid;
    
    @Autowired
    public DocumentDetailView(DocumentService documentService, ContentService contentService) {
        this.documentService = documentService;
        this.contentService = contentService;
        
        addClassName("document-detail-view");
        setSizeFull();
        setPadding(true);
    }
    
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long documentId) {
        if (documentId != null) {
            loadDocument(documentId);
        } else {
            showError("No document ID provided");
            createBackButton();
        }
    }
    
    private void loadDocument(Long documentId) {
        try {
            currentDocument = documentService.findById(documentId);
            if (currentDocument != null) {
                buildView();
            } else {
                showError("Document not found");
                createBackButton();
            }
        } catch (Exception e) {
            showError("Failed to load document: " + e.getMessage());
            createBackButton();
        }
    }
    
    private void buildView() {
        removeAll();
        
        // Create back button and header
        Button backButton = new Button("Back to Search", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> UI.getCurrent().navigate("search"));
        
        Button backToDocumentsButton = new Button("Back to Documents", new Icon(VaadinIcon.ARROW_LEFT));
        backToDocumentsButton.addClickListener(e -> UI.getCurrent().navigate(""));
        
        HorizontalLayout headerButtons = new HorizontalLayout(backButton, backToDocumentsButton);
        headerButtons.setSpacing(true);
        
        H2 title = new H2("Document Details");
        
        HorizontalLayout header = new HorizontalLayout(headerButtons);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        
        // Create split layout
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        
        // Left side: Document information
        documentInfoPanel = createDocumentInfoPanel();
        splitLayout.addToPrimary(documentInfoPanel);
        
        // Right side: Content grid
        contentPanel = createContentPanel();
        splitLayout.addToSecondary(contentPanel);
        
        splitLayout.setSplitterPosition(50); // 50/50 split
        
        add(header, title, new Hr(), splitLayout);
        expand(splitLayout);
    }
    
    private VerticalLayout createDocumentInfoPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(true);
        panel.setSpacing(true);
        
        H3 infoTitle = new H3("Document Information");
        
        // Create styled information display
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setSpacing(true);
        infoLayout.setPadding(false);
        
        addInfoRow(infoLayout, "Name:", currentDocument.getName());
        addInfoRow(infoLayout, "Type:", currentDocument.getDocumentType().toString());
        addInfoRow(infoLayout, "Description:", currentDocument.getDescription());
        addInfoRow(infoLayout, "Keywords:", currentDocument.getKeywords());
        
        if (currentDocument.getTags() != null && !currentDocument.getTags().isEmpty()) {
            addInfoRow(infoLayout, "Tags:", String.join(", ", currentDocument.getTags()));
        }
        
        if (currentDocument.getOwner() != null) {
            addInfoRow(infoLayout, "Owner:", currentDocument.getOwner().getUsername());
        }
        
        if (currentDocument.getAuthors() != null && !currentDocument.getAuthors().isEmpty()) {
            String authors = currentDocument.getAuthors().stream()
                .map(User::getUsername)
                .collect(Collectors.joining(", "));
            addInfoRow(infoLayout, "Authors:", authors);
        }
        
        addInfoRow(infoLayout, "Version:", 
            currentDocument.getMajorVersion() + "." + currentDocument.getMinorVersion());
        
        if (currentDocument.getParentVersion() != null) {
            addInfoRow(infoLayout, "Parent Version:", 
                currentDocument.getParentVersion().getMajorVersion() + "." + 
                currentDocument.getParentVersion().getMinorVersion());
        }
        
        addInfoRow(infoLayout, "Created:", 
            currentDocument.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addInfoRow(infoLayout, "Modified:", 
            currentDocument.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        panel.add(infoTitle, infoLayout);
        return panel;
    }
    
    private void addInfoRow(VerticalLayout layout, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return; // Skip empty values
        }
        
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.START);
        
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("font-weight", "bold")
            .set("min-width", "150px")
            .set("color", "var(--lumo-secondary-text-color)");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("flex", "1");
        
        row.add(labelSpan, valueSpan);
        layout.add(row);
    }
    
    private VerticalLayout createContentPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(true);
        panel.setSpacing(true);
        
        H3 contentTitle = new H3("Associated Content");
        
        // Configure content grid
        contentGrid = new Grid<>(Content.class, false);
        contentGrid.setSizeFull();
        
        contentGrid.addColumn(Content::getName).setHeader("File Name").setAutoWidth(true).setFlexGrow(1);
        contentGrid.addColumn(Content::getContentType).setHeader("Type").setAutoWidth(true);
        
        // Add rendition type column with badge
        contentGrid.addComponentColumn(content -> {
            Span badge = new Span(content.isPrimary() ? "Primary" : "Secondary");
            if (content.isPrimary()) {
                badge.getElement().getThemeList().add("badge success");
            } else {
                badge.getElement().getThemeList().add("badge");
            }
            return badge;
        }).setHeader("Rendition").setAutoWidth(true);
        
        // Add indexable column with icon
        contentGrid.addComponentColumn(content -> {
            if (content.isIndexable()) {
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
                icon.setColor("var(--lumo-success-color)");
                icon.getElement().setAttribute("title", "Indexable");
                return icon;
            } else {
                Icon icon = new Icon(VaadinIcon.MINUS_CIRCLE);
                icon.setColor("var(--lumo-disabled-text-color)");
                icon.getElement().setAttribute("title", "Not indexable");
                return icon;
            }
        }).setHeader("Indexable").setAutoWidth(true);
        
        contentGrid.addColumn(content -> {
            if (content.isStoredInDatabase()) {
                return "Database";
            } else if (content.isStoredInFileStore()) {
                return content.getFileStore().getName();
            }
            return "Unknown";
        }).setHeader("Storage").setAutoWidth(true);
        
        contentGrid.addColumn(content -> {
            if (content.getContent() != null) {
                return formatBytes(content.getContent().length);
            } else if (content.getStoragePath() != null) {
                try {
                    return formatBytes(contentService.getContentBytes(content.getId()).length);
                } catch (IOException e) {
                    return "Error";
                }
            }
            return "0 bytes";
        }).setHeader("Size").setAutoWidth(true);
        
        // Add actions column
        contentGrid.addComponentColumn(content -> {
            Button viewButton = new Button(new Icon(VaadinIcon.EYE));
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewButton.addClickListener(e -> viewContent(content));
            viewButton.getElement().setAttribute("title", "View content");
            
            Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
            downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downloadButton.addClickListener(e -> downloadContent(content));
            downloadButton.getElement().setAttribute("title", "Download content");
            
            HorizontalLayout actions = new HorizontalLayout(viewButton, downloadButton);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        contentGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        
        // Load content for this document
        List<Content> contents = contentService.findBySysObject(currentDocument);
        contentGrid.setItems(contents);
        
        if (contents.isEmpty()) {
            Span emptyMsg = new Span("No content attached to this document");
            emptyMsg.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic")
                .set("text-align", "center")
                .set("padding", "2em");
            panel.add(contentTitle, emptyMsg);
        } else {
            panel.add(contentTitle, contentGrid);
            panel.expand(contentGrid);
        }
        
        return panel;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private void viewContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            String contentType = content.getContentType();
            
            com.vaadin.flow.component.dialog.Dialog viewDialog = new com.vaadin.flow.component.dialog.Dialog();
            viewDialog.setWidth("80%");
            viewDialog.setHeight("80%");
            
            H2 title = new H2("View: " + content.getName());
            
            VerticalLayout contentLayout = new VerticalLayout();
            contentLayout.setSizeFull();
            contentLayout.setPadding(false);
            
            // Display content based on type
            if (contentType != null && contentType.startsWith("text/")) {
                com.vaadin.flow.component.textfield.TextArea textArea = 
                    new com.vaadin.flow.component.textfield.TextArea();
                textArea.setValue(new String(bytes));
                textArea.setReadOnly(true);
                textArea.setSizeFull();
                contentLayout.add(textArea);
            } else if (contentType != null && contentType.startsWith("image/")) {
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + contentType + ";base64," + base64;
                
                Image image = new Image(dataUrl, content.getName());
                image.setMaxWidth("100%");
                image.getStyle().set("display", "block").set("margin", "auto");
                contentLayout.add(image);
                contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            } else {
                Span message = new Span("Preview not available for this file type. Size: " + 
                    formatBytes(bytes.length));
                message.getStyle().set("padding", "2em");
                
                Button downloadBtn = new Button("Download File", new Icon(VaadinIcon.DOWNLOAD));
                downloadBtn.addClickListener(e -> downloadContent(content));
                
                contentLayout.add(message, downloadBtn);
                contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            }
            
            Button closeButton = new Button("Close", e -> viewDialog.close());
            closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            HorizontalLayout buttonLayout = new HorizontalLayout(closeButton);
            buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            buttonLayout.setWidthFull();
            buttonLayout.setPadding(true);
            
            VerticalLayout dialogLayout = new VerticalLayout(title, new Hr(), contentLayout, buttonLayout);
            dialogLayout.setSizeFull();
            dialogLayout.setPadding(false);
            dialogLayout.setSpacing(false);
            dialogLayout.expand(contentLayout);
            
            viewDialog.add(dialogLayout);
            viewDialog.open();
            
        } catch (Exception e) {
            Notification.show("Failed to view content: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            
            com.vaadin.flow.server.StreamResource resource = 
                new com.vaadin.flow.server.StreamResource(content.getName(), 
                    () -> new ByteArrayInputStream(bytes));
            
            if (content.getContentType() != null) {
                resource.setContentType(content.getContentType());
            }
            
            com.vaadin.flow.component.html.Anchor download = 
                new com.vaadin.flow.component.html.Anchor(resource, "");
            download.getElement().setAttribute("download", true);
            download.getElement().getStyle().set("display", "none");
            
            getUI().ifPresent(ui -> {
                ui.getElement().appendChild(download.getElement());
                ui.getPage().executeJs("$0.click()", download.getElement());
                ui.getElement().removeChild(download.getElement());
            });
            
            Notification.show("Downloading " + content.getName(), 
                2000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception e) {
            Notification.show("Failed to download content: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void createBackButton() {
        Button backButton = new Button("Back to Search", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> UI.getCurrent().navigate("search"));
        add(backButton);
    }
    
    private void showError(String message) {
        H2 errorTitle = new H2("Error");
        Span errorMessage = new Span(message);
        errorMessage.getStyle().set("color", "var(--lumo-error-color)");
        add(errorTitle, errorMessage);
    }
}
