package com.docmgmt.ui.components;

import com.docmgmt.plugin.PluginResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.List;
import java.util.Map;

/**
 * Dialog for displaying plugin execution results
 */
public class PluginResultDialog extends Dialog {
    
    public PluginResultDialog(String pluginName, PluginResponse response) {
        setWidth("800px");
        setHeight("80vh");
        
        H2 title = new H2("Results: " + pluginName);
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.getStyle().set("overflow-y", "auto");
        
        // Add all data from response
        if (response.getData() != null) {
            for (Map.Entry<String, Object> entry : response.getData().entrySet()) {
                content.add(createResultSection(entry.getKey(), entry.getValue()));
            }
        }
        
        Button closeButton = new Button("Close", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttons = new HorizontalLayout(closeButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        
        VerticalLayout mainLayout = new VerticalLayout(title, new Hr(), content, new Hr(), buttons);
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        
        add(mainLayout);
    }
    
    private com.vaadin.flow.component.Component createResultSection(String key, Object value) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "4px")
            .set("padding", "10px")
            .set("margin-bottom", "10px")
            .set("background-color", "var(--lumo-contrast-5pct)");
        
        // Title
        H3 sectionTitle = new H3(formatKey(key));
        sectionTitle.getStyle().set("margin", "0 0 10px 0");
        section.add(sectionTitle);
        
        // Value
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.length() > 200 || strValue.contains("\\n")) {
                TextArea textArea = new TextArea();
                textArea.setValue(strValue);
                textArea.setReadOnly(true);
                textArea.setWidthFull();
                textArea.setHeight("200px");
                textArea.getStyle().set("font-family", "monospace");
                section.add(textArea);
            } else {
                Span valueSpan = new Span(strValue);
                valueSpan.getStyle().set("white-space", "pre-wrap");
                section.add(valueSpan);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                Span emptySpan = new Span("(none)");
                emptySpan.getStyle().set("font-style", "italic").set("color", "var(--lumo-disabled-text-color)");
                section.add(emptySpan);
            } else {
                UnorderedList ul = new UnorderedList();
                for (Object item : list) {
                    ListItem li = new ListItem(String.valueOf(item));
                    ul.add(li);
                }
                section.add(ul);
            }
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                HorizontalLayout row = new HorizontalLayout();
                row.setSpacing(true);
                
                Span keySpan = new Span(String.valueOf(entry.getKey()) + ":");
                keySpan.getStyle().set("font-weight", "bold").set("min-width", "150px");
                
                Span valueSpan = new Span(String.valueOf(entry.getValue()));
                
                row.add(keySpan, valueSpan);
                section.add(row);
            }
        } else {
            Span valueSpan = new Span(String.valueOf(value));
            section.add(valueSpan);
        }
        
        return section;
    }
    
    private String formatKey(String key) {
        // Convert camelCase to Title Case
        return key.replaceAll("([A-Z])", " $1")
                  .replaceAll("([a-z])([A-Z])", "$1 $2")
                  .trim()
                  .substring(0, 1).toUpperCase() + 
               key.replaceAll("([A-Z])", " $1")
                  .replaceAll("([a-z])([A-Z])", "$1 $2")
                  .trim()
                  .substring(1);
    }
}
