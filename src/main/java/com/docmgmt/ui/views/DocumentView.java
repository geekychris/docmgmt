package com.docmgmt.ui.views;

import com.docmgmt.model.*;
import com.docmgmt.model.Document.DocumentType;
import com.docmgmt.service.ContentService;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.FileStoreService;
import com.docmgmt.service.UserService;
import com.docmgmt.transformer.TransformerRegistry;
import com.docmgmt.ui.MainLayout;
import com.docmgmt.ui.util.DocumentFieldRenderer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.select.Select;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Documents | Document Management System")
public class DocumentView extends VerticalLayout {

    private final DocumentService documentService;
    private final ContentService contentService;
    private final FileStoreService fileStoreService;
    private final UserService userService;
    private final TransformerRegistry transformerRegistry;
    
    private Grid<Document> grid;
    private TextField filterText;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button createVersionButton;
    private Button uploadContentButton;
    
    private Dialog documentDialog;
    private Binder<Document> binder;
    private Document currentDocument;
    
    private VerticalLayout contentDetailsPanel;
    private Grid<Content> contentGrid;
    private H2 contentPanelTitle;
    private Checkbox showAllVersionsCheckbox;
    
    private CallbackDataProvider<Document, Void> dataProvider;
    private static final int DEFAULT_PAGE_SIZE = 50; // Transparent lazy loading batch size
    private boolean currentFilterValue = false; // Tracks "Show All Versions" checkbox
    
    @Autowired
    public DocumentView(DocumentService documentService, ContentService contentService, 
                       FileStoreService fileStoreService, UserService userService,
                       TransformerRegistry transformerRegistry) {
        this.documentService = documentService;
        this.contentService = contentService;
        this.fileStoreService = fileStoreService;
        this.userService = userService;
        this.transformerRegistry = transformerRegistry;
        
        addClassName("document-view");
        setSizeFull();
        
        configureGrid();
        configureFilter();
        configureDialog();
        configureContentDetailsPanel();
        
        HorizontalLayout toolbar = createToolbar();
        
        // Create split layout with document grid on left and content details on right
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.addToPrimary(grid);
        splitLayout.addToSecondary(contentDetailsPanel);
        splitLayout.setSplitterPosition(60); // 60% for grid, 40% for details
        
        VerticalLayout mainLayout = new VerticalLayout(toolbar, splitLayout);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.expand(splitLayout);
        
        add(mainLayout);
        
        updateList();
    }
    
    private void configureGrid() {
        grid = new Grid<>(Document.class, false);
        grid.addClassNames("document-grid");
        grid.setSizeFull();
        
        // Add columns to the grid
        grid.addColumn(Document::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Document::getDocumentType).setHeader("Type").setSortable(true);
        grid.addColumn(Document::getDescription).setHeader("Description");
        grid.addColumn(doc -> doc.getOwner() != null ? doc.getOwner().getUsername() : "-")
            .setHeader("Owner").setSortable(false);
        grid.addColumn(doc -> formatAuthors(doc.getAuthors())).setHeader("Authors");
        grid.addColumn(doc -> formatTags(doc.getTags())).setHeader("Tags");
        grid.addColumn(doc -> doc.getMajorVersion() + "." + doc.getMinorVersion())
            .setHeader("Version").setSortable(true);
        grid.addColumn(doc -> {
            if (doc.getParentVersion() != null) {
                SysObject parent = doc.getParentVersion();
                return parent.getMajorVersion() + "." + parent.getMinorVersion();
            }
            return "-";
        }).setHeader("Parent Ver.").setSortable(false);
        grid.addColumn(doc -> doc.getContents() != null ? doc.getContents().size() : 0)
            .setHeader("Content Items").setSortable(true);
        grid.addColumn(doc -> doc.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .setHeader("Last Modified").setSortable(true);
        
        // Configure grid styling and behavior
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            try {
                boolean hasSelection = event.getValue() != null;
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                createVersionButton.setEnabled(hasSelection);
                uploadContentButton.setEnabled(hasSelection);
                
                // Update content details panel
                if (hasSelection) {
                    showContentForDocument(event.getValue());
                } else {
                    clearContentDetails();
                }
            } catch (Exception e) {
                // Log error but don't disrupt the UI
                e.printStackTrace();
                Notification.show("Error in selection handling: " + e.getMessage(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }
    
    private String formatTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(", ", tags);
    }
    
    private String formatAuthors(Set<User> authors) {
        if (authors == null || authors.isEmpty()) {
            return "";
        }
        return authors.stream()
            .map(User::getUsername)
            .collect(Collectors.joining(", "));
    }
    
    private void configureFilter() {
        filterText = new TextField();
        filterText.setPlaceholder("Filter by name, description, or author...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> filterGrid());
    }
    
    private void filterGrid() {
        // For lazy loading, filter needs to be implemented server-side
        // For now, refresh the data when filter changes
        // A full implementation would pass the filter to the backend query
        if (dataProvider != null && filterText != null) {
            String filter = filterText.getValue();
            if (filter != null && !filter.trim().isEmpty()) {
                Notification.show("Client-side filtering not supported with lazy loading. Use search page for filtering.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            }
        }
    }
    
    private HorizontalLayout createToolbar() {
        // Configure checkbox for showing all versions
        showAllVersionsCheckbox = new Checkbox("Show All Versions");
        showAllVersionsCheckbox.setValue(false);
        showAllVersionsCheckbox.addValueChangeListener(e -> updateList());
        
        // Configure buttons
        addButton = new Button("Add Document", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openDocumentTypeSelectionDialog());
        
        editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
        editButton.setEnabled(false);
        editButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                openDocumentDialog(document);
            }
        });
        
        deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setEnabled(false);
        deleteButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                confirmDelete(document);
            }
        });
        
        createVersionButton = new Button("Create Version", new Icon(VaadinIcon.FILE_CODE));
        createVersionButton.setEnabled(false);
        createVersionButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                openVersionDialog(document);
            }
        });
        
        uploadContentButton = new Button("Upload Content", new Icon(VaadinIcon.UPLOAD));
        uploadContentButton.setEnabled(false);
        uploadContentButton.addClickListener(e -> {
            Document document = grid.asSingleSelect().getValue();
            if (document != null) {
                openUploadContentDialog(document);
            }
        });
        
        // Create the toolbar layout
        HorizontalLayout toolbar = new HorizontalLayout(
            filterText, showAllVersionsCheckbox, addButton, editButton, deleteButton, createVersionButton, uploadContentButton
        );
        
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.expand(filterText); // Make the filter take up available space
        
        return toolbar;
    }
    
    private void configureDialog() {
        documentDialog = new Dialog();
        documentDialog.setWidth("600px");
        
        // Create the binder for the document form
        binder = new BeanValidationBinder<>(Document.class);
    }
    
    private void openDocumentDialog(Document document) {
        currentDocument = document;
        
        documentDialog.removeAll();
        
        // Create a NEW binder for each dialog to avoid stale bindings from previous documents
        binder = new BeanValidationBinder<>(Document.class);
        
        // Create dialog title based on whether we are creating or editing
        H2 title = new H2(document.getId() == null ? "Create New Document" : "Edit Document");
        
        // Create form fields
        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        
        ComboBox<Document.DocumentType> typeCombo = new ComboBox<>("Document Type");
        typeCombo.setItems(Document.DocumentType.values());
        typeCombo.setRequired(true);
        
        TextArea descriptionField = new TextArea("Description");
        descriptionField.setHeight("100px");
        
        TextArea tagsField = new TextArea("Tags (comma separated)");
        tagsField.setHeight("80px");
        
        TextField keywordsField = new TextField("Keywords");
        
        // Owner selection
        ComboBox<User> ownerCombo = new ComboBox<>("Owner");
        ownerCombo.setItems(userService.findAll());
        ownerCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        ownerCombo.setPlaceholder("Select owner...");
        ownerCombo.setClearButtonVisible(true);
        
        // Authors selection
        MultiSelectComboBox<User> authorsCombo = new MultiSelectComboBox<>("Authors");
        authorsCombo.setItems(userService.findAll());
        authorsCombo.setItemLabelGenerator(user -> user.getUsername() + " (" + user.getFullName() + ")");
        authorsCombo.setPlaceholder("Search and select authors...");
        authorsCombo.setClearButtonVisible(true);
        
        // Create the form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, typeCombo, descriptionField, ownerCombo, tagsField, keywordsField, authorsCombo);
        
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Set column spans for full-width fields
        formLayout.setColspan(descriptionField, 2);
        formLayout.setColspan(tagsField, 2);
        formLayout.setColspan(keywordsField, 2);
        formLayout.setColspan(authorsCombo, 2);
        
        // Create buttons for the dialog
        Button cancelButton = new Button("Cancel", e -> documentDialog.close());
        Button saveButton = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                saveDocument();
                documentDialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setPadding(true);
        
        // Set up the binder
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(Document::getName, Document::setName);
        
        binder.forField(typeCombo)
            .asRequired("Document type is required")
            .bind(Document::getDocumentType, Document::setDocumentType);
        
        binder.forField(descriptionField)
            .bind(Document::getDescription, Document::setDescription);
        
        // Handling tags as a string in the UI but as a Set<String> in the model
        binder.forField(tagsField)
            .bind(
                doc -> doc.getTags() != null ? String.join(", ", doc.getTags()) : "",
                (doc, value) -> {
                    if (value == null || value.trim().isEmpty()) {
                        doc.setTags(new HashSet<>());
                    } else {
                        Set<String> tags = Arrays.stream(value.split(","))
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .collect(Collectors.toSet());
                        doc.setTags(tags);
                    }
                }
            );
        
        binder.forField(keywordsField)
            .bind(Document::getKeywords, Document::setKeywords);
        
        // Bind owner
        binder.forField(ownerCombo)
            .bind(Document::getOwner, Document::setOwner);
        
        // Bind authors
        binder.forField(authorsCombo)
            .bind(
                doc -> doc.getAuthors(),
                (doc, authors) -> {
                    doc.getAuthors().clear();
                    if (authors != null) {
                        doc.getAuthors().addAll(authors);
                    }
                }
            );
        
        // Read the document into the form for base fields only
        binder.readBean(document);
        
        // Add type-specific fields AFTER reading base fields
        // Type-specific fields are already initialized with values in their add methods
        addTypeSpecificFields(formLayout, document);
        
        // Add components to the dialog
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        documentDialog.add(dialogLayout);
        documentDialog.open();
    }
    
    private void saveDocument() {
        if (currentDocument != null && binder.writeBeanIfValid(currentDocument)) {
            try {
                documentService.save(currentDocument);
                updateList();
                Notification.show("Document saved successfully.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to save document: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
    
    /**
     * Add type-specific fields to the form based on document type.
     * These fields are NOT bound to the main binder to avoid cross-contamination between document types.
     * Instead, we handle their values manually in saveDocument().
     */
    private Map<String, Object> typeSpecificFieldMap = new HashMap<>();
    
    private void addTypeSpecificFields(FormLayout formLayout, Document document) {
        // Clear previous type-specific fields
        typeSpecificFieldMap.clear();
        
        if (document instanceof Article) {
            addArticleFields(formLayout, (Article) document);
        } else if (document instanceof Report) {
            addReportFields(formLayout, (Report) document);
        } else if (document instanceof Contract) {
            addContractFields(formLayout, (Contract) document);
        } else if (document instanceof Manual) {
            addManualFields(formLayout, (Manual) document);
        } else if (document instanceof Presentation) {
            addPresentationFields(formLayout, (Presentation) document);
        } else if (document instanceof TripReport) {
            addTripReportFields(formLayout, (TripReport) document);
        }
    }
    
    private void addArticleFields(FormLayout formLayout, Article article) {
        DatePicker publicationDatePicker = new DatePicker("Publication Date");
        publicationDatePicker.setValue(article.getPublicationDate());
        publicationDatePicker.setWidthFull();
        publicationDatePicker.addValueChangeListener(e -> article.setPublicationDate(e.getValue()));
        typeSpecificFieldMap.put("publicationDate", publicationDatePicker);
        
        TextField journalField = new TextField("Journal");
        journalField.setValue(article.getJournal() != null ? article.getJournal() : "");
        journalField.setWidthFull();
        journalField.addValueChangeListener(e -> article.setJournal(e.getValue()));
        typeSpecificFieldMap.put("journal", journalField);
        
        TextField volumeField = new TextField("Volume");
        volumeField.setValue(article.getVolume() != null ? article.getVolume() : "");
        volumeField.addValueChangeListener(e -> article.setVolume(e.getValue()));
        typeSpecificFieldMap.put("volume", volumeField);
        
        TextField issueField = new TextField("Issue");
        issueField.setValue(article.getIssue() != null ? article.getIssue() : "");
        issueField.addValueChangeListener(e -> article.setIssue(e.getValue()));
        typeSpecificFieldMap.put("issue", issueField);
        
        TextField pagesField = new TextField("Pages");
        pagesField.setValue(article.getPages() != null ? article.getPages() : "");
        pagesField.addValueChangeListener(e -> article.setPages(e.getValue()));
        typeSpecificFieldMap.put("pages", pagesField);
        
        TextField doiField = new TextField("DOI");
        doiField.setValue(article.getDoi() != null ? article.getDoi() : "");
        doiField.setWidthFull();
        doiField.addValueChangeListener(e -> article.setDoi(e.getValue()));
        typeSpecificFieldMap.put("doi", doiField);
        
        formLayout.add(publicationDatePicker, journalField, volumeField, issueField, pagesField, doiField);
        formLayout.setColspan(journalField, 2);
        formLayout.setColspan(doiField, 2);
    }
    
    private void addReportFields(FormLayout formLayout, Report report) {
        DatePicker reportDatePicker = new DatePicker("Report Date");
        reportDatePicker.setValue(report.getReportDate());
        reportDatePicker.setWidthFull();
        reportDatePicker.addValueChangeListener(e -> report.setReportDate(e.getValue()));
        typeSpecificFieldMap.put("reportDate", reportDatePicker);
        
        TextField reportNumberField = new TextField("Report Number");
        reportNumberField.setValue(report.getReportNumber() != null ? report.getReportNumber() : "");
        reportNumberField.setWidthFull();
        reportNumberField.addValueChangeListener(e -> report.setReportNumber(e.getValue()));
        typeSpecificFieldMap.put("reportNumber", reportNumberField);
        
        TextField departmentField = new TextField("Department");
        departmentField.setValue(report.getDepartment() != null ? report.getDepartment() : "");
        departmentField.setWidthFull();
        departmentField.addValueChangeListener(e -> report.setDepartment(e.getValue()));
        typeSpecificFieldMap.put("department", departmentField);
        
        TextField confidentialityField = new TextField("Confidentiality Level");
        confidentialityField.setValue(report.getConfidentialityLevel() != null ? report.getConfidentialityLevel() : "");
        confidentialityField.setWidthFull();
        confidentialityField.addValueChangeListener(e -> report.setConfidentialityLevel(e.getValue()));
        typeSpecificFieldMap.put("confidentialityLevel", confidentialityField);
        
        formLayout.add(reportDatePicker, reportNumberField, departmentField, confidentialityField);
    }
    
    private void addContractFields(FormLayout formLayout, Contract contract) {
        TextField contractNumberField = new TextField("Contract Number");
        contractNumberField.setValue(contract.getContractNumber() != null ? contract.getContractNumber() : "");
        contractNumberField.setWidthFull();
        contractNumberField.addValueChangeListener(e -> contract.setContractNumber(e.getValue()));
        typeSpecificFieldMap.put("contractNumber", contractNumberField);
        
        DatePicker effectiveDatePicker = new DatePicker("Effective Date");
        effectiveDatePicker.setValue(contract.getEffectiveDate());
        effectiveDatePicker.setWidthFull();
        effectiveDatePicker.addValueChangeListener(e -> contract.setEffectiveDate(e.getValue()));
        typeSpecificFieldMap.put("effectiveDate", effectiveDatePicker);
        
        DatePicker expirationDatePicker = new DatePicker("Expiration Date");
        expirationDatePicker.setValue(contract.getExpirationDate());
        expirationDatePicker.setWidthFull();
        expirationDatePicker.addValueChangeListener(e -> contract.setExpirationDate(e.getValue()));
        typeSpecificFieldMap.put("expirationDate", expirationDatePicker);
        
        TextArea partiesField = new TextArea("Parties (comma separated)");
        if (contract.getParties() != null && !contract.getParties().isEmpty()) {
            partiesField.setValue(String.join(", ", contract.getParties()));
        }
        partiesField.setWidthFull();
        partiesField.setHeight("80px");
        partiesField.addValueChangeListener(e -> {
            String value = e.getValue();
            if (value == null || value.trim().isEmpty()) {
                contract.setParties(new HashSet<>());
            } else {
                contract.setParties(Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .collect(Collectors.toSet()));
            }
        });
        typeSpecificFieldMap.put("parties", partiesField);
        
        NumberField contractValueField = new NumberField("Contract Value");
        contractValueField.setValue(contract.getContractValue() != null ? contract.getContractValue() : 0.0);
        contractValueField.setWidthFull();
        contractValueField.addValueChangeListener(e -> contract.setContractValue(e.getValue()));
        typeSpecificFieldMap.put("contractValue", contractValueField);
        
        formLayout.add(contractNumberField, effectiveDatePicker, expirationDatePicker, partiesField, contractValueField);
        formLayout.setColspan(partiesField, 2);
    }
    
    private void addManualFields(FormLayout formLayout, Manual manual) {
        TextField manualVersionField = new TextField("Manual Version");
        manualVersionField.setValue(manual.getManualVersion() != null ? manual.getManualVersion() : "");
        manualVersionField.setWidthFull();
        manualVersionField.addValueChangeListener(e -> manual.setManualVersion(e.getValue()));
        typeSpecificFieldMap.put("manualVersion", manualVersionField);
        
        TextField productNameField = new TextField("Product Name");
        productNameField.setValue(manual.getProductName() != null ? manual.getProductName() : "");
        productNameField.setWidthFull();
        productNameField.addValueChangeListener(e -> manual.setProductName(e.getValue()));
        typeSpecificFieldMap.put("productName", productNameField);
        
        DatePicker lastReviewDatePicker = new DatePicker("Last Review Date");
        lastReviewDatePicker.setValue(manual.getLastReviewDate());
        lastReviewDatePicker.setWidthFull();
        lastReviewDatePicker.addValueChangeListener(e -> manual.setLastReviewDate(e.getValue()));
        typeSpecificFieldMap.put("lastReviewDate", lastReviewDatePicker);
        
        TextField targetAudienceField = new TextField("Target Audience");
        targetAudienceField.setValue(manual.getTargetAudience() != null ? manual.getTargetAudience() : "");
        targetAudienceField.setWidthFull();
        targetAudienceField.addValueChangeListener(e -> manual.setTargetAudience(e.getValue()));
        typeSpecificFieldMap.put("targetAudience", targetAudienceField);
        
        formLayout.add(manualVersionField, productNameField, lastReviewDatePicker, targetAudienceField);
    }
    
    private void addPresentationFields(FormLayout formLayout, Presentation presentation) {
        DatePicker presentationDatePicker = new DatePicker("Presentation Date");
        presentationDatePicker.setValue(presentation.getPresentationDate());
        presentationDatePicker.setWidthFull();
        presentationDatePicker.addValueChangeListener(e -> presentation.setPresentationDate(e.getValue()));
        typeSpecificFieldMap.put("presentationDate", presentationDatePicker);
        
        TextField venueField = new TextField("Venue");
        venueField.setValue(presentation.getVenue() != null ? presentation.getVenue() : "");
        venueField.setWidthFull();
        venueField.addValueChangeListener(e -> presentation.setVenue(e.getValue()));
        typeSpecificFieldMap.put("venue", venueField);
        
        TextField audienceField = new TextField("Audience");
        audienceField.setValue(presentation.getAudience() != null ? presentation.getAudience() : "");
        audienceField.setWidthFull();
        audienceField.addValueChangeListener(e -> presentation.setAudience(e.getValue()));
        typeSpecificFieldMap.put("audience", audienceField);
        
        NumberField durationField = new NumberField("Duration (minutes)");
        durationField.setValue(presentation.getDurationMinutes() != null ? presentation.getDurationMinutes().doubleValue() : 0.0);
        durationField.setWidthFull();
        durationField.addValueChangeListener(e -> presentation.setDurationMinutes(e.getValue() != null ? e.getValue().intValue() : null));
        typeSpecificFieldMap.put("durationMinutes", durationField);
        
        formLayout.add(presentationDatePicker, venueField, audienceField, durationField);
    }
    
    private void addTripReportFields(FormLayout formLayout, TripReport tripReport) {
        TextField destinationField = new TextField("Destination");
        destinationField.setValue(tripReport.getDestination() != null ? tripReport.getDestination() : "");
        destinationField.setWidthFull();
        destinationField.addValueChangeListener(e -> tripReport.setDestination(e.getValue()));
        typeSpecificFieldMap.put("destination", destinationField);
        
        DatePicker startDatePicker = new DatePicker("Trip Start Date");
        startDatePicker.setValue(tripReport.getTripStartDate());
        startDatePicker.setWidthFull();
        startDatePicker.addValueChangeListener(e -> tripReport.setTripStartDate(e.getValue()));
        typeSpecificFieldMap.put("tripStartDate", startDatePicker);
        
        DatePicker endDatePicker = new DatePicker("Trip End Date");
        endDatePicker.setValue(tripReport.getTripEndDate());
        endDatePicker.setWidthFull();
        endDatePicker.addValueChangeListener(e -> tripReport.setTripEndDate(e.getValue()));
        typeSpecificFieldMap.put("tripEndDate", endDatePicker);
        
        TextArea purposeField = new TextArea("Purpose");
        purposeField.setValue(tripReport.getPurpose() != null ? tripReport.getPurpose() : "");
        purposeField.setWidthFull();
        purposeField.setHeight("100px");
        purposeField.addValueChangeListener(e -> tripReport.setPurpose(e.getValue()));
        typeSpecificFieldMap.put("purpose", purposeField);
        
        NumberField budgetField = new NumberField("Budget Amount");
        budgetField.setValue(tripReport.getBudgetAmount() != null ? tripReport.getBudgetAmount() : 0.0);
        budgetField.setWidthFull();
        budgetField.addValueChangeListener(e -> tripReport.setBudgetAmount(e.getValue()));
        typeSpecificFieldMap.put("budgetAmount", budgetField);
        
        NumberField actualField = new NumberField("Actual Amount");
        actualField.setValue(tripReport.getActualAmount() != null ? tripReport.getActualAmount() : 0.0);
        actualField.setWidthFull();
        actualField.addValueChangeListener(e -> tripReport.setActualAmount(e.getValue()));
        typeSpecificFieldMap.put("actualAmount", actualField);
        
        TextArea attendeesField = new TextArea("Attendees (comma separated)");
        if (tripReport.getAttendees() != null && !tripReport.getAttendees().isEmpty()) {
            attendeesField.setValue(String.join(", ", tripReport.getAttendees()));
        }
        attendeesField.setWidthFull();
        attendeesField.setHeight("80px");
        attendeesField.addValueChangeListener(e -> {
            String value = e.getValue();
            if (value == null || value.trim().isEmpty()) {
                tripReport.setAttendees(new HashSet<>());
            } else {
                tripReport.setAttendees(Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(a -> !a.isEmpty())
                    .collect(Collectors.toSet()));
            }
        });
        typeSpecificFieldMap.put("attendees", attendeesField);
        
        TextArea summaryField = new TextArea("Summary");
        summaryField.setValue(tripReport.getSummary() != null ? tripReport.getSummary() : "");
        summaryField.setWidthFull();
        summaryField.setHeight("100px");
        summaryField.addValueChangeListener(e -> tripReport.setSummary(e.getValue()));
        typeSpecificFieldMap.put("summary", summaryField);
        
        TextArea followUpField = new TextArea("Follow-up Actions");
        followUpField.setValue(tripReport.getFollowUpActions() != null ? tripReport.getFollowUpActions() : "");
        followUpField.setWidthFull();
        followUpField.setHeight("100px");
        followUpField.addValueChangeListener(e -> tripReport.setFollowUpActions(e.getValue()));
        typeSpecificFieldMap.put("followUpActions", followUpField);
        
        formLayout.add(destinationField, startDatePicker, endDatePicker, purposeField, 
                      budgetField, actualField, attendeesField, summaryField, followUpField);
        formLayout.setColspan(purposeField, 2);
        formLayout.setColspan(attendeesField, 2);
        formLayout.setColspan(summaryField, 2);
        formLayout.setColspan(followUpField, 2);
    }
    
    private void confirmDelete(Document document) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Document");
        dialog.setText("Are you sure you want to delete '" + document.getName() + "'? This action cannot be undone.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(event -> {
            try {
                documentService.delete(document.getId());
                updateList();
                Notification.show("Document deleted.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Failed to delete document: " + e.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        dialog.open();
    }
    
    private void openVersionDialog(Document document) {
        Dialog versionDialog = new Dialog();
        versionDialog.setWidth("400px");
        
        H2 title = new H2("Create New Version");
        
        // Add a label showing the current version
        Span currentVersionLabel = new Span("Current version: " + document.getMajorVersion() + "." + document.getMinorVersion());
        currentVersionLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        ComboBox<String> versionTypeCombo = new ComboBox<>("Version Type");
        versionTypeCombo.setItems("Major Version", "Minor Version");
        versionTypeCombo.setValue("Minor Version");
        versionTypeCombo.setRequired(true);
        
        Button cancelButton = new Button("Cancel", e -> versionDialog.close());
        Button createButton = new Button("Create", e -> {
            if (versionTypeCombo.getValue() == null) {
                Notification.show("Please select a version type", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                if ("Major Version".equals(versionTypeCombo.getValue())) {
                    documentService.createMajorVersion(document.getId());
                } else {
                    documentService.createMinorVersion(document.getId());
                }
                updateList();
                Notification.show("New version created.", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                versionDialog.close();
            } catch (Exception ex) {
                Notification.show("Failed to create version: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        // Create dialog layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(currentVersionLabel, versionTypeCombo);
        formLayout.setColspan(currentVersionLabel, 2);
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, createButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        versionDialog.add(dialogLayout);
        versionDialog.open();
    }
    
    private void configureContentDetailsPanel() {
        contentDetailsPanel = new VerticalLayout();
        contentDetailsPanel.setSizeFull();
        contentDetailsPanel.setPadding(true);
        contentDetailsPanel.setSpacing(true);
        
        contentPanelTitle = new H2("Content");
        contentPanelTitle.getStyle().set("margin-top", "0");
        
        // Configure content grid
        contentGrid = new Grid<>(Content.class, false);
        contentGrid.setSizeFull();
        contentGrid.addColumn(Content::getName).setHeader("File Name").setAutoWidth(true);
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
                return content.getContent().length + " bytes";
            } else if (content.getStoragePath() != null) {
                try {
                    return contentService.getContentBytes(content.getId()).length + " bytes";
                } catch (IOException e) {
                    return "Error";
                }
            }
            return "0 bytes";
        }).setHeader("Size").setAutoWidth(true);
        
        // Add actions column with view/download/transform buttons
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
            
            // Add transform button for primary content that can be transformed
            if (content.isPrimary() && transformerRegistry.findTransformer(content).isPresent()) {
                Button transformButton = new Button(new Icon(VaadinIcon.MAGIC));
                transformButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                transformButton.addClickListener(e -> transformContent(content));
                transformButton.getElement().setAttribute("title", "Transform to text");
                actions.add(transformButton);
            }
            
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        contentGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        
        Span emptyState = new Span("Select a document to view its content");
        emptyState.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-style", "italic")
            .set("text-align", "center")
            .set("padding", "2em");
        
        contentDetailsPanel.add(contentPanelTitle, contentGrid);
        contentDetailsPanel.expand(contentGrid);
    }
    
    private void showContentForDocument(Document document) {
        if (document == null) {
            clearContentDetails();
            return;
        }
        
        contentPanelTitle.setText("Content for: " + document.getName());
        
        // Fetch content for the document
        List<Content> contents = contentService.findBySysObject(document);
        contentGrid.setItems(contents);
        contentGrid.setVisible(!contents.isEmpty());
        
        if (contents.isEmpty()) {
            // Show empty state
            Span emptyMsg = new Span("No content attached to this document");
            emptyMsg.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic");
            // Note: Could add empty state component here if needed
        }
    }
    
    private void clearContentDetails() {
        contentPanelTitle.setText("Content");
        contentGrid.setItems();
    }
    
    private void viewContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            String contentType = content.getContentType();
            
            Dialog viewDialog = new Dialog();
            viewDialog.setWidth("80%");
            viewDialog.setHeight("80%");
            
            H2 title = new H2("View: " + content.getName());
            
            VerticalLayout contentLayout = new VerticalLayout();
            contentLayout.setSizeFull();
            contentLayout.setPadding(false);
            
            // Display content based on type
            if (contentType != null && contentType.startsWith("text/")) {
                // Display text content with monospace font
                com.vaadin.flow.component.html.Pre pre = new com.vaadin.flow.component.html.Pre();
                pre.setText(new String(bytes));
                pre.getStyle()
                    .set("white-space", "pre-wrap")
                    .set("font-family", "monospace")
                    .set("font-size", "12px")
                    .set("padding", "1em")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("overflow", "auto")
                    .set("max-height", "100%");
                contentLayout.add(pre);
                
            } else if (contentType != null && contentType.equals("application/pdf")) {
                // Display PDF using iframe with base64 data URI
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUri = "data:application/pdf;base64," + base64;
                
                com.vaadin.flow.component.html.IFrame iframe = new com.vaadin.flow.component.html.IFrame(dataUri);
                iframe.setSizeFull();
                iframe.getStyle().set("border", "none");
                contentLayout.add(iframe);
                
            } else if (contentType != null && contentType.startsWith("image/")) {
                // Display image
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + contentType + ";base64," + base64;
                
                com.vaadin.flow.component.html.Image image = 
                    new com.vaadin.flow.component.html.Image(dataUrl, content.getName());
                image.setMaxWidth("100%");
                image.getStyle().set("display", "block").set("margin", "auto");
                contentLayout.add(image);
                contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                
            } else {
                // Unsupported type - show download option
                Span message = new Span("Preview not available for this file type: " + contentType);
                message.getStyle().set("padding", "2em");
                
                Span sizeInfo = new Span("Size: " + formatBytes((long) bytes.length));
                sizeInfo.getStyle().set("margin-top", "10px");
                
                Button downloadBtn = new Button("Download File", new Icon(VaadinIcon.DOWNLOAD));
                downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                downloadBtn.addClickListener(e -> downloadContent(content));
                
                contentLayout.add(message, sizeInfo, downloadBtn);
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
    
    private void transformContent(Content content) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setWidth("400px");
        
        H2 title = new H2("Transform Content");
        Span message = new Span("Transform " + content.getName() + " to searchable text?");
        message.getStyle().set("display", "block").set("margin-bottom", "1em");
        
        Button cancelButton = new Button("Cancel", e -> confirmDialog.close());
        Button transformButton = new Button("Transform", e -> {
            try {
                contentService.transformAndAddRendition(content.getId(), null);
                
                // Refresh content display
                Document doc = (Document) content.getSysObject();
                showContentForDocument(doc);
                
                Notification.show("Content transformed successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                confirmDialog.close();
            } catch (Exception ex) {
                Notification.show("Failed to transform content: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        transformButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(cancelButton, transformButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout layout = new VerticalLayout(title, new Hr(), message, buttons);
        layout.setPadding(false);
        layout.setSpacing(false);
        
        confirmDialog.add(layout);
        confirmDialog.open();
    }
    
    private void downloadContent(Content content) {
        try {
            byte[] bytes = contentService.getContentBytes(content.getId());
            
            // Create a stream resource for download
            com.vaadin.flow.server.StreamResource resource = 
                new com.vaadin.flow.server.StreamResource(content.getName(), 
                    () -> new ByteArrayInputStream(bytes));
            
            // Set content type if available
            if (content.getContentType() != null) {
                resource.setContentType(content.getContentType());
            }
            
            // Trigger download using anchor
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
    
    private void openUploadContentDialog(Document document) {
        Dialog uploadDialog = new Dialog();
        uploadDialog.setWidth("500px");
        
        H2 title = new H2("Upload Content");
        
        Span docInfo = new Span("Document: " + document.getName() + " (v" + 
                               document.getMajorVersion() + "." + document.getMinorVersion() + ")");
        docInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        // Storage type selection
        RadioButtonGroup<String> storageType = new RadioButtonGroup<>();
        storageType.setLabel("Storage Type");
        storageType.setItems("Database", "File Store");
        storageType.setValue("Database");
        
        // File store selection (initially hidden)
        ComboBox<FileStore> fileStoreCombo = new ComboBox<>("File Store");
        fileStoreCombo.setItems(fileStoreService.findAll());
        fileStoreCombo.setItemLabelGenerator(FileStore::getName);
        fileStoreCombo.setVisible(false);
        
        storageType.addValueChangeListener(e -> {
            boolean isFileStore = "File Store".equals(e.getValue());
            fileStoreCombo.setVisible(isFileStore);
            if (isFileStore && fileStoreCombo.getValue() == null && !fileStoreCombo.getListDataView().getItems().findFirst().isEmpty()) {
                fileStoreCombo.setValue(fileStoreCombo.getListDataView().getItems().findFirst().get());
            }
        });
        
        // File upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes(
            "application/pdf", ".pdf",
            "text/plain", ".txt",
            "application/msword", ".doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "image/*"
        );
        
        Span uploadStatus = new Span();
        uploadStatus.setVisible(false);
        
        upload.addSucceededListener(event -> {
            uploadStatus.setText("File uploaded: " + event.getFileName());
            uploadStatus.getStyle().set("color", "var(--lumo-success-color)");
            uploadStatus.setVisible(true);
        });
        
        upload.addFileRejectedListener(event -> {
            uploadStatus.setText("File rejected: " + event.getErrorMessage());
            uploadStatus.getStyle().set("color", "var(--lumo-error-color)");
            uploadStatus.setVisible(true);
        });
        
        // Buttons
        Button cancelButton = new Button("Cancel", e -> uploadDialog.close());
        Button saveButton = new Button("Upload", e -> {
            try {
                if (buffer.getInputStream().available() == 0) {
                    Notification.show("Please select a file to upload", 
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Create a custom MultipartFile from the buffer
                byte[] fileBytes = buffer.getInputStream().readAllBytes();
                String fileName = buffer.getFileName();
                String contentType = buffer.getFileData().getMimeType();
                
                MultipartFile multipartFile = new MultipartFile() {
                    @Override
                    public String getName() { return fileName; }
                    
                    @Override
                    public String getOriginalFilename() { return fileName; }
                    
                    @Override
                    public String getContentType() { return contentType; }
                    
                    @Override
                    public boolean isEmpty() { return fileBytes.length == 0; }
                    
                    @Override
                    public long getSize() { return fileBytes.length; }
                    
                    @Override
                    public byte[] getBytes() { return fileBytes; }
                    
                    @Override
                    public InputStream getInputStream() { 
                        return new ByteArrayInputStream(fileBytes); 
                    }
                    
                    @Override
                    public void transferTo(File dest) throws IllegalStateException {
                        throw new UnsupportedOperationException();
                    }
                };
                
                Content content;
                if ("Database".equals(storageType.getValue())) {
                    content = contentService.createContentInDatabase(multipartFile, document);
                } else {
                    if (fileStoreCombo.getValue() == null) {
                        Notification.show("Please select a file store", 
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    content = contentService.createContentInFileStore(
                        multipartFile, document, fileStoreCombo.getValue().getId()
                    );
                }
                
                Notification.show("Content uploaded successfully", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                uploadDialog.close();
                
                // Refresh content panel to show new content
                showContentForDocument(document);
                
            } catch (Exception ex) {
                Notification.show("Failed to upload content: " + ex.getMessage(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        // Layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(docInfo, storageType, fileStoreCombo, upload, uploadStatus);
        formLayout.setColspan(docInfo, 2);
        formLayout.setColspan(upload, 2);
        formLayout.setColspan(uploadStatus, 2);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), formLayout, buttonLayout
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        
        uploadDialog.add(dialogLayout);
        uploadDialog.open();
    }
    
    /**
     * Updates the document list in the grid by configuring lazy loading data provider.
     * This method is called after CRUD operations to refresh the UI.
     */
    private void updateList() {
        try {
            currentFilterValue = showAllVersionsCheckbox.getValue();
            
            // Create a lazy loading data provider
            dataProvider = DataProvider.fromCallbacks(
                // Fetch callback - called when grid needs data
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;
                    
                    Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "modifiedAt"));
                    
                    Page<Document> documentPage;
                    if (currentFilterValue) {
                        documentPage = documentService.findAllPaginated(pageable);
                    } else {
                        documentPage = documentService.findAllLatestVersionsPaginated(pageable);
                    }
                    
                    // Collections are already initialized in the service method
                    return documentPage.getContent().stream();
                },
                // Count callback - called to determine total number of items
                query -> {
                    if (currentFilterValue) {
                        return (int) documentService.count();
                    } else {
                        return (int) documentService.countLatestVersions();
                    }
                }
            );
            
            grid.setDataProvider(dataProvider);
            
            // Set page size for transparent lazy loading
            grid.setPageSize(DEFAULT_PAGE_SIZE);
            
            // Reset button states since selection is cleared
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            createVersionButton.setEnabled(false);
            uploadContentButton.setEnabled(false);
        } catch (Exception e) {
            Notification.show("Failed to load documents: " + e.getMessage(), 
                3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Log the error for server-side tracking
            e.printStackTrace();
        }
    }
    
    /**
     * Opens a dialog to select document type before creating a new document
     */
    private void openDocumentTypeSelectionDialog() {
        Dialog typeDialog = new Dialog();
        typeDialog.setWidth("400px");
        
        H2 title = new H2("Select Document Type");
        
        ComboBox<Document.DocumentType> typeCombo = new ComboBox<>("Document Type");
        typeCombo.setItems(Document.DocumentType.values());
        typeCombo.setValue(Document.DocumentType.ARTICLE); // Default
        typeCombo.setWidthFull();
        typeCombo.setRequired(true);
        
        Span helpText = new Span("Choose the type of document you want to create. Each type has specific fields tailored to its purpose.");
        helpText.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)")
            .set("display", "block")
            .set("margin-bottom", "1em");
        
        Button cancelButton = new Button("Cancel", e -> typeDialog.close());
        Button continueButton = new Button("Continue", e -> {
            if (typeCombo.getValue() != null) {
                Document newDoc = createDocumentByType(typeCombo.getValue());
                typeDialog.close();
                openDocumentDialog(newDoc);
            } else {
                Notification.show("Please select a document type", 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, continueButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(
            title, new Hr(), helpText, typeCombo, buttonLayout
        );
        dialogLayout.setPadding(true);
        
        typeDialog.add(dialogLayout);
        typeDialog.open();
    }
    
    /**
     * Create a document instance of the appropriate subclass based on type
     */
    private Document createDocumentByType(Document.DocumentType type) {
        switch (type) {
            case ARTICLE:
                return Article.builder().build();
            case REPORT:
                return Report.builder().build();
            case CONTRACT:
                return Contract.builder().build();
            case MANUAL:
                return Manual.builder().build();
            case PRESENTATION:
                return Presentation.builder().build();
            case TRIP_REPORT:
                return TripReport.builder().build();
            default:
                return Article.builder().build();
        }
    }
    
    /**
     * Handles error reporting in a consistent way across the application.
     * 
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    private void showError(String operation, Exception error) {
        // Log the error
        error.printStackTrace();
        
        // Show user-friendly notification
        Notification.show(
            operation + " failed: " + error.getMessage(),
            3000, 
            Notification.Position.BOTTOM_START
        ).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    /**
     * Format bytes to human readable string
     */
    private String formatBytes(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
