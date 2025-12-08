package com.docmgmt.ui.util;

import com.docmgmt.model.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metadata-driven utility for rendering document fields dynamically.
 * Uses reflection to extract and display both base and type-specific fields.
 */
public class DocumentFieldRenderer {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Field metadata for rendering
     */
    public static class FieldInfo {
        private final String name;
        private final String label;
        private final Object value;
        private final Class<?> type;
        private final boolean isCollection;
        
        public FieldInfo(String name, String label, Object value, Class<?> type, boolean isCollection) {
            this.name = name;
            this.label = label;
            this.value = value;
            this.type = type;
            this.isCollection = isCollection;
        }
        
        public String getName() { return name; }
        public String getLabel() { return label; }
        public Object getValue() { return value; }
        public Class<?> getType() { return type; }
        public boolean isCollection() { return isCollection; }
    }
    
    /**
     * Extract all fields (base and type-specific) from a document
     */
    public static List<FieldInfo> extractFields(Document document) {
        List<FieldInfo> fields = new ArrayList<>();
        
        if (document == null) {
            return fields;
        }
        
        // Get all fields from the document class hierarchy
        Class<?> currentClass = document.getClass();
        Set<String> processedFields = new HashSet<>();
        
        while (currentClass != null && Document.class.isAssignableFrom(currentClass)) {
            for (Field field : currentClass.getDeclaredFields()) {
                String fieldName = field.getName();
                
                // Skip if already processed or if it's a system field
                if (processedFields.contains(fieldName) || shouldSkipField(fieldName)) {
                    continue;
                }
                
                processedFields.add(fieldName);
                
                try {
                    // Try to get getter method
                    String getterName = "get" + capitalize(fieldName);
                    Method getter = null;
                    
                    try {
                        getter = currentClass.getMethod(getterName);
                    } catch (NoSuchMethodException e) {
                        // Try "is" prefix for boolean fields
                        getterName = "is" + capitalize(fieldName);
                        try {
                            getter = currentClass.getMethod(getterName);
                        } catch (NoSuchMethodException ex) {
                            // Skip if no getter found
                            continue;
                        }
                    }
                    
                    Object value = getter.invoke(document);
                    String label = fieldNameToLabel(fieldName);
                    
                    boolean isCollection = Collection.class.isAssignableFrom(field.getType()) ||
                                         Set.class.isAssignableFrom(field.getType());
                    
                    fields.add(new FieldInfo(fieldName, label, value, field.getType(), isCollection));
                    
                } catch (Exception e) {
                    // Skip fields that can't be accessed
                    continue;
                }
            }
            
            currentClass = currentClass.getSuperclass();
        }
        
        // Sort fields: base fields first, then type-specific fields
        return sortFields(fields, document);
    }
    
    /**
     * Render fields as read-only info rows in a VerticalLayout
     */
    public static void renderReadOnlyFields(Document document, VerticalLayout container) {
        // Debug: log document class
        System.out.println("DEBUG: DocumentFieldRenderer rendering document of class: " + document.getClass().getName());
        
        List<FieldInfo> fields = extractFields(document);
        System.out.println("DEBUG: Extracted " + fields.size() + " fields");
        for (FieldInfo field : fields) {
            System.out.println("DEBUG: Field: " + field.getName() + " = " + field.getValue());
        }
        
        for (FieldInfo field : fields) {
            String displayValue;
            
            // Show empty type-specific fields with a placeholder
            if (field.getValue() == null || 
                (field.getValue() instanceof String && ((String) field.getValue()).trim().isEmpty()) ||
                (field.getValue() instanceof Collection && ((Collection<?>) field.getValue()).isEmpty())) {
                
                // Skip base fields if empty (name, description, keywords, tags)
                if (isBaseField(field.getName())) {
                    continue;
                }
                
                // Show type-specific fields even if empty
                displayValue = "(not set)";
            } else {
                displayValue = formatValue(field.getValue());
            }
            
            if (displayValue != null && !displayValue.trim().isEmpty()) {
                addInfoRow(container, field.getLabel() + ":", displayValue);
            }
        }
    }
    
    /**
     * Render fields as editable form components in a FormLayout
     */
    public static Map<String, Component> renderEditableFields(Document document, FormLayout formLayout, 
                                                               ComboBox<User> ownerCombo, 
                                                               MultiSelectComboBox<User> authorsCombo,
                                                               List<User> allUsers) {
        Map<String, Component> fieldComponents = new HashMap<>();
        List<FieldInfo> fields = extractFields(document);
        
        for (FieldInfo field : fields) {
            // Skip fields that are handled separately or shouldn't be edited
            if (shouldSkipForEditing(field.getName())) {
                continue;
            }
            
            Component component = createEditableComponent(field, document);
            if (component != null) {
                fieldComponents.put(field.getName(), component);
                formLayout.add(component);
            }
        }
        
        return fieldComponents;
    }
    
    /**
     * Create editable component based on field type
     */
    private static Component createEditableComponent(FieldInfo field, Document document) {
        if (LocalDate.class.equals(field.getType())) {
            DatePicker datePicker = new DatePicker(field.getLabel());
            if (field.getValue() instanceof LocalDate) {
                datePicker.setValue((LocalDate) field.getValue());
            }
            datePicker.setWidthFull();
            return datePicker;
            
        } else if (Double.class.equals(field.getType()) || double.class.equals(field.getType())) {
            NumberField numberField = new NumberField(field.getLabel());
            if (field.getValue() != null) {
                numberField.setValue((Double) field.getValue());
            }
            numberField.setWidthFull();
            return numberField;
            
        } else if (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) {
            NumberField numberField = new NumberField(field.getLabel());
            if (field.getValue() != null) {
                numberField.setValue(((Integer) field.getValue()).doubleValue());
            }
            numberField.setWidthFull();
            return numberField;
            
        } else if (field.isCollection() && field.getValue() instanceof Set && 
                   !((Set<?>) field.getValue()).isEmpty() && 
                   ((Set<?>) field.getValue()).iterator().next() instanceof String) {
            // String collection (tags, parties, attendees, etc.)
            TextArea textArea = new TextArea(field.getLabel() + " (comma separated)");
            Set<String> values = (Set<String>) field.getValue();
            textArea.setValue(String.join(", ", values));
            textArea.setWidthFull();
            textArea.setHeight("80px");
            return textArea;
            
        } else if (String.class.equals(field.getType())) {
            // Use TextArea for potentially long fields
            if (field.getName().toLowerCase().contains("description") ||
                field.getName().toLowerCase().contains("purpose") ||
                field.getName().toLowerCase().contains("summary") ||
                field.getName().toLowerCase().contains("actions")) {
                TextArea textArea = new TextArea(field.getLabel());
                textArea.setValue(field.getValue() != null ? (String) field.getValue() : "");
                textArea.setWidthFull();
                textArea.setHeight("100px");
                return textArea;
            } else {
                TextField textField = new TextField(field.getLabel());
                textField.setValue(field.getValue() != null ? (String) field.getValue() : "");
                textField.setWidthFull();
                return textField;
            }
        }
        
        return null;
    }
    
    /**
     * Format a value for display
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DATE_FORMATTER);
        } else if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return "";
            }
            // Check if it's a collection of users
            if (collection.iterator().next() instanceof User) {
                return ((Collection<User>) collection).stream()
                    .map(User::getUsername)
                    .collect(Collectors.joining(", "));
            } else {
                return collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            }
        } else if (value instanceof Set) {
            Set<?> set = (Set<?>) value;
            if (set.isEmpty()) {
                return "";
            }
            return String.join(", ", set.stream().map(Object::toString).collect(Collectors.toList()));
        } else if (value instanceof User) {
            return ((User) value).getUsername();
        } else if (value instanceof Double) {
            return String.format("%.2f", (Double) value);
        }
        
        return value.toString();
    }
    
    /**
     * Add an info row to a container
     */
    private static void addInfoRow(VerticalLayout layout, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
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
    
    /**
     * Convert field name to readable label
     */
    private static String fieldNameToLabel(String fieldName) {
        // Handle special cases
        if ("doi".equalsIgnoreCase(fieldName)) return "DOI";
        
        // Split camelCase and capitalize
        String[] words = fieldName.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
        StringBuilder label = new StringBuilder();
        
        for (String word : words) {
            if (label.length() > 0) {
                label.append(" ");
            }
            label.append(capitalize(word));
        }
        
        return label.toString();
    }
    
    /**
     * Capitalize first letter
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Check if field should be skipped
     */
    private static boolean shouldSkipField(String fieldName) {
        Set<String> skipFields = new HashSet<>(Arrays.asList(
            "id", "createdAt", "modifiedAt", "majorVersion", "minorVersion",
            "parentVersion", "contents", "folders", "documentType", "serialVersionUID"
        ));
        return skipFields.contains(fieldName);
    }
    
    /**
     * Check if field is a base Document field
     */
    private static boolean isBaseField(String fieldName) {
        Set<String> baseFields = new HashSet<>(Arrays.asList(
            "name", "description", "keywords", "tags", "owner", "authors"
        ));
        return baseFields.contains(fieldName);
    }
    
    /**
     * Check if field should be skipped for editing
     */
    private static boolean shouldSkipForEditing(String fieldName) {
        Set<String> skipFields = new HashSet<>(Arrays.asList(
            "id", "createdAt", "modifiedAt", "majorVersion", "minorVersion",
            "parentVersion", "contents", "folders", "documentType", "owner", "authors",
            "name", "tags", "keywords", "description", "serialVersionUID"
        ));
        return skipFields.contains(fieldName);
    }
    
    /**
     * Sort fields: base fields first, then type-specific
     */
    private static List<FieldInfo> sortFields(List<FieldInfo> fields, Document document) {
        // Define base field order
        List<String> baseFieldOrder = Arrays.asList(
            "name", "documentType", "description", "keywords", "tags", 
            "owner", "authors"
        );
        
        List<FieldInfo> sorted = new ArrayList<>();
        
        // Add base fields first in order
        for (String baseName : baseFieldOrder) {
            fields.stream()
                .filter(f -> f.getName().equals(baseName))
                .findFirst()
                .ifPresent(sorted::add);
        }
        
        // Add type-specific fields
        for (FieldInfo field : fields) {
            if (!baseFieldOrder.contains(field.getName())) {
                sorted.add(field);
            }
        }
        
        return sorted;
    }
    
    /**
     * Get type-specific fields summary (for display in grids)
     */
    public static String getTypeSpecificSummary(Document document) {
        if (document == null) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        
        if (document instanceof Article) {
            Article article = (Article) document;
            if (article.getJournal() != null) {
                summary.append("Journal: ").append(article.getJournal());
            }
            if (article.getPublicationDate() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Published: ").append(article.getPublicationDate().format(DATE_FORMATTER));
            }
        } else if (document instanceof Report) {
            Report report = (Report) document;
            if (report.getReportNumber() != null) {
                summary.append("Report #: ").append(report.getReportNumber());
            }
            if (report.getDepartment() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Dept: ").append(report.getDepartment());
            }
        } else if (document instanceof Contract) {
            Contract contract = (Contract) document;
            if (contract.getContractNumber() != null) {
                summary.append("Contract #: ").append(contract.getContractNumber());
            }
            if (contract.getEffectiveDate() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Effective: ").append(contract.getEffectiveDate().format(DATE_FORMATTER));
            }
            if (contract.getExpirationDate() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Expires: ").append(contract.getExpirationDate().format(DATE_FORMATTER));
            }
        } else if (document instanceof Manual) {
            Manual manual = (Manual) document;
            if (manual.getProductName() != null) {
                summary.append("Product: ").append(manual.getProductName());
            }
            if (manual.getManualVersion() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Manual v").append(manual.getManualVersion());
            }
        } else if (document instanceof Presentation) {
            Presentation presentation = (Presentation) document;
            if (presentation.getVenue() != null) {
                summary.append("Venue: ").append(presentation.getVenue());
            }
            if (presentation.getPresentationDate() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Date: ").append(presentation.getPresentationDate().format(DATE_FORMATTER));
            }
        } else if (document instanceof TripReport) {
            TripReport tripReport = (TripReport) document;
            if (tripReport.getDestination() != null) {
                summary.append("Destination: ").append(tripReport.getDestination());
            }
            if (tripReport.getTripStartDate() != null) {
                if (summary.length() > 0) summary.append("; ");
                summary.append("Start: ").append(tripReport.getTripStartDate().format(DATE_FORMATTER));
            }
        }
        
        return summary.toString();
    }
}
