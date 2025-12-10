package com.docmgmt.ui.views;

import com.docmgmt.model.Document;
import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.search.SearchResult;
import com.docmgmt.search.SearchResultsWrapper;
import com.docmgmt.service.DocumentService;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.docmgmt.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Route(value = "search", layout = MainLayout.class)
@PageTitle("Search | Document Management System")
public class SearchView extends VerticalLayout {
    
    private final LuceneIndexService searchService;
    private final DocumentService documentService;
    private final com.docmgmt.service.UserService userService;
    private final com.docmgmt.service.ContentService contentService;
    private final com.docmgmt.plugin.PluginService pluginService;
    private final com.docmgmt.service.DocumentSimilarityService similarityService;
    private final com.docmgmt.service.DocumentFieldExtractionService fieldExtractionService;
    private final com.docmgmt.service.FileStoreService fileStoreService;
    
    private TextField searchField;
    private TextField nameField;
    private TextField descriptionField;
    private TextField keywordsField;
    private TextField tagsField;
    private TextField contentField;
    private RadioButtonGroup<String> searchMode;
    private RadioButtonGroup<String> operatorGroup;
    private CheckboxGroup<String> fieldSelection;
    private Grid<SearchResult> resultsGrid;
    private Span resultsCount;
    private Select<Integer> maxResultsSelect;
    private Select<Integer> pageSizeSelect;
    private ListDataProvider<SearchResult> dataProvider;
    private static final int DEFAULT_MAX_RESULTS = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    @Autowired
    public SearchView(LuceneIndexService searchService, 
                     DocumentService documentService,
                     com.docmgmt.service.UserService userService,
                     com.docmgmt.service.ContentService contentService,
                     com.docmgmt.plugin.PluginService pluginService,
                     com.docmgmt.service.DocumentSimilarityService similarityService,
                     com.docmgmt.service.DocumentFieldExtractionService fieldExtractionService,
                     com.docmgmt.service.FileStoreService fileStoreService) {
        this.searchService = searchService;
        this.documentService = documentService;
        this.userService = userService;
        this.contentService = contentService;
        this.pluginService = pluginService;
        this.similarityService = similarityService;
        this.fieldExtractionService = fieldExtractionService;
        this.fileStoreService = fileStoreService;
        
        addClassName("search-view");
        setSizeFull();
        setPadding(true);
        
        H2 title = new H2("Search Documents");
        
        VerticalLayout searchForm = createSearchForm();
        VerticalLayout resultsPanel = createResultsPanel();
        
        add(title, new Hr(), searchForm, resultsPanel);
        expand(resultsPanel);
    }
    
    private VerticalLayout createSearchForm() {
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        
        // Search mode selector
        searchMode = new RadioButtonGroup<>();
        searchMode.setLabel("Search Mode");
        searchMode.setItems("Simple Search", "Field-Specific Search");
        searchMode.setValue("Simple Search");
        
        // Simple search field
        searchField = new TextField("Search Query");
        searchField.setPlaceholder("Enter search terms...");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        
        // Field-specific search fields
        VerticalLayout fieldSearchLayout = new VerticalLayout();
        fieldSearchLayout.setPadding(false);
        fieldSearchLayout.setSpacing(false);
        fieldSearchLayout.setVisible(false);
        
        nameField = new TextField("Name");
        nameField.setPlaceholder("Search in name...");
        
        descriptionField = new TextField("Description");
        descriptionField.setPlaceholder("Search in description...");
        
        keywordsField = new TextField("Keywords");
        keywordsField.setPlaceholder("Search in keywords...");
        
        tagsField = new TextField("Tags");
        tagsField.setPlaceholder("Search in tags...");
        
        contentField = new TextField("Content");
        contentField.setPlaceholder("Search in content...");
        
        // Arrange fields in two rows to save space
        HorizontalLayout row1 = new HorizontalLayout(nameField, descriptionField, keywordsField);
        row1.setWidthFull();
        row1.setSpacing(true);
        
        HorizontalLayout row2 = new HorizontalLayout(tagsField, contentField);
        row2.setWidthFull();
        row2.setSpacing(true);
        
        operatorGroup = new RadioButtonGroup<>();
        operatorGroup.setLabel("Operator");
        operatorGroup.setItems("AND", "OR");
        operatorGroup.setValue("AND");
        
        fieldSearchLayout.add(row1, row2, operatorGroup);
        
        // Search mode change listener
        searchMode.addValueChangeListener(event -> {
            boolean isFieldSearch = "Field-Specific Search".equals(event.getValue());
            searchField.setVisible(!isFieldSearch);
            fieldSearchLayout.setVisible(isFieldSearch);
        });
        
        // Search and index buttons
        Button searchButton = new Button("Search", new Icon(VaadinIcon.SEARCH));
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(e -> performSearch());
        
        Button rebuildIndexButton = new Button("Rebuild Index", new Icon(VaadinIcon.REFRESH));
        rebuildIndexButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        rebuildIndexButton.addClickListener(e -> rebuildIndex());
        
        Button clearButton = new Button("Clear", new Icon(VaadinIcon.ERASER));
        clearButton.addClickListener(e -> clearForm());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(searchButton, rebuildIndexButton, clearButton);
        buttonLayout.setSpacing(true);
        
        formLayout.add(searchMode, searchField, fieldSearchLayout, buttonLayout);
        
        return formLayout;
    }
    
    private VerticalLayout createResultsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);
        
        // Max results selector
        maxResultsSelect = new Select<>();
        maxResultsSelect.setLabel("Max Results");
        maxResultsSelect.setItems(100, 500, 1000, 5000, 10000);
        maxResultsSelect.setValue(DEFAULT_MAX_RESULTS);
        maxResultsSelect.setWidth("150px");
        
        // Page size selector
        pageSizeSelect = new Select<>();
        pageSizeSelect.setLabel("Page Size");
        pageSizeSelect.setItems(10, 20, 50, 100, 200);
        pageSizeSelect.setValue(DEFAULT_PAGE_SIZE);
        pageSizeSelect.setWidth("120px");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null && dataProvider != null) {
                resultsGrid.getDataProvider().refreshAll();
            }
        });
        
        resultsCount = new Span("No search performed");
        resultsCount.getStyle().set("font-weight", "bold");
        
        Button batchExtractFieldsButton = new Button("AI Extract Fields (Batch)", new Icon(VaadinIcon.LIGHTBULB));
        batchExtractFieldsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        batchExtractFieldsButton.setEnabled(false);
        batchExtractFieldsButton.addClickListener(e -> batchExtractFields());
        
        HorizontalLayout controlsLayout = new HorizontalLayout(resultsCount, maxResultsSelect, pageSizeSelect, batchExtractFieldsButton);
        controlsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlsLayout.setSpacing(true);
        
        resultsGrid = new Grid<>(SearchResult.class, false);
        resultsGrid.setSizeFull();
        resultsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        
        resultsGrid.addColumn(SearchResult::getName).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        resultsGrid.addColumn(SearchResult::getDescription).setHeader("Description").setAutoWidth(true).setFlexGrow(2);
        
        // Add type-specific details column
        resultsGrid.addColumn(result -> {
            if (result.getDocumentId() != null) {
                try {
                    Document doc = documentService.findById(result.getDocumentId());
                    return DocumentFieldRenderer.getTypeSpecificSummary(doc);
                } catch (Exception e) {
                    return "";
                }
            }
            return "";
        }).setHeader("Type-Specific Details").setAutoWidth(true).setFlexGrow(1);
        
        resultsGrid.addColumn(SearchResult::getKeywords).setHeader("Keywords").setAutoWidth(true);
        resultsGrid.addColumn(SearchResult::getTags).setHeader("Tags").setAutoWidth(true);
        resultsGrid.addColumn(result -> String.format("%.2f", result.getScore()))
            .setHeader("Score").setAutoWidth(true);
        
        resultsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        
        // Enable batch extract button when documents are selected
        resultsGrid.addSelectionListener(event -> {
            batchExtractFieldsButton.setEnabled(!event.getAllSelectedItems().isEmpty());
        });
        
        // Make rows clickable to navigate to document details
        resultsGrid.addItemClickListener(event -> {
            SearchResult result = event.getItem();
            if (result != null && result.getDocumentId() != null) {
                openDocument(result.getDocumentId());
            }
        });
        
        panel.add(controlsLayout, resultsGrid);
        panel.expand(resultsGrid);
        
        return panel;
    }
    
    private void performSearch() {
        try {
            SearchResultsWrapper wrapper;
            
            if ("Simple Search".equals(searchMode.getValue())) {
                String query = searchField.getValue();
                if (query == null || query.trim().isEmpty()) {
                    Notification.show("Please enter a search query", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                int maxResults = maxResultsSelect.getValue() != null ? maxResultsSelect.getValue() : DEFAULT_MAX_RESULTS;
                wrapper = searchService.search(query, maxResults);
                
            } else {
                // Field-specific search
                Map<String, String> fieldQueries = new HashMap<>();
                
                if (nameField.getValue() != null && !nameField.getValue().trim().isEmpty()) {
                    fieldQueries.put(LuceneIndexService.FIELD_NAME, nameField.getValue());
                }
                if (descriptionField.getValue() != null && !descriptionField.getValue().trim().isEmpty()) {
                    fieldQueries.put(LuceneIndexService.FIELD_DESCRIPTION, descriptionField.getValue());
                }
                if (keywordsField.getValue() != null && !keywordsField.getValue().trim().isEmpty()) {
                    fieldQueries.put(LuceneIndexService.FIELD_KEYWORDS, keywordsField.getValue());
                }
                if (tagsField.getValue() != null && !tagsField.getValue().trim().isEmpty()) {
                    fieldQueries.put(LuceneIndexService.FIELD_TAGS, tagsField.getValue());
                }
                if (contentField.getValue() != null && !contentField.getValue().trim().isEmpty()) {
                    fieldQueries.put(LuceneIndexService.FIELD_CONTENT, contentField.getValue());
                }
                
                if (fieldQueries.isEmpty()) {
                    Notification.show("Please enter at least one field query", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                org.apache.lucene.search.BooleanClause.Occur operator = 
                    "OR".equals(operatorGroup.getValue()) ? 
                        org.apache.lucene.search.BooleanClause.Occur.SHOULD : 
                        org.apache.lucene.search.BooleanClause.Occur.MUST;
                
                int maxResults = maxResultsSelect.getValue() != null ? maxResultsSelect.getValue() : DEFAULT_MAX_RESULTS;
                wrapper = searchService.searchFieldsWithOperator(fieldQueries, operator, maxResults);
            }
            
            // Setup data provider with pagination
            List<SearchResult> results = wrapper.getResults();
            dataProvider = DataProvider.ofCollection(results);
            resultsGrid.setDataProvider(dataProvider);
            
            // Set page size
            int pageSize = pageSizeSelect.getValue() != null ? pageSizeSelect.getValue() : DEFAULT_PAGE_SIZE;
            resultsGrid.setPageSize(pageSize);
            
            // Update count with total hits
            String countText = String.format("%,d result(s) found", wrapper.getTotalHits());
            if (wrapper.hasMoreResults()) {
                countText += String.format(" (showing first %,d)", results.size());
            }
            resultsCount.setText(countText);
            
            if (results.isEmpty()) {
                Notification.show("No results found", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            }
            
        } catch (Exception e) {
            Notification.show("Search failed: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void rebuildIndex() {
        try {
            List<Document> allDocuments = documentService.findAll();
            searchService.rebuildIndex(allDocuments);
            
            Notification.show("Index rebuilt successfully with " + allDocuments.size() + " documents", 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception e) {
            Notification.show("Failed to rebuild index: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void clearForm() {
        searchField.clear();
        nameField.clear();
        descriptionField.clear();
        keywordsField.clear();
        tagsField.clear();
        contentField.clear();
        resultsGrid.setItems();
        resultsCount.setText("No search performed");
    }
    
    private void openDocument(Long documentId) {
        Document document = documentService.findById(documentId);
        if (document == null) {
            Notification.show("Document not found", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Use a full-featured document dialog
        new com.docmgmt.ui.components.DocumentDetailDialog(
            document, 
            documentService, 
            userService, 
            contentService, 
            pluginService, 
            similarityService,
            fieldExtractionService,
            fileStoreService
        ).open();
    }
    
    /**
     * Batch extract and apply AI fields for selected documents
     */
    private void batchExtractFields() {
        List<Long> documentIds = resultsGrid.getSelectedItems().stream()
            .map(SearchResult::getDocumentId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
        
        if (documentIds.isEmpty()) {
            Notification.show("No documents selected", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        
        // Show progress notification
        Notification.show("Extracting fields for " + documentIds.size() + " document(s)... This may take a while.", 
            3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        
        // Process in background
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
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
                
                // Clear selection
                resultsGrid.deselectAll();
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
}
