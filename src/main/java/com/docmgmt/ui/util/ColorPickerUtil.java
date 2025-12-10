package com.docmgmt.ui.util;

import com.vaadin.flow.component.combobox.ComboBox;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for creating color picker components
 */
public class ColorPickerUtil {
    
    private static final List<String> COLOR_VALUES = Arrays.asList(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
        "#DFE6E9", "#A29BFE", "#FD79A8", "#FDCB6E", "#6C5CE7"
    );
    
    /**
     * Create a color picker ComboBox with preset colors
     * @param label The label for the color picker
     * @param currentColor The current color value (can be null)
     * @return Configured ComboBox for color selection
     */
    public static ComboBox<String> createColorPicker(String label, String currentColor) {
        ComboBox<String> colorCombo = new ComboBox<>(label);
        colorCombo.setItems(COLOR_VALUES);
        colorCombo.setValue(currentColor);
        colorCombo.setPlaceholder("Select color...");
        colorCombo.setClearButtonVisible(true);
        colorCombo.setWidthFull();
        colorCombo.setItemLabelGenerator(ColorPickerUtil::getColorLabel);
        return colorCombo;
    }
    
    /**
     * Create a color picker ComboBox with default label "Color"
     * @param currentColor The current color value (can be null)
     * @return Configured ComboBox for color selection
     */
    public static ComboBox<String> createColorPicker(String currentColor) {
        return createColorPicker("Color", currentColor);
    }
    
    /**
     * Get display label for a color hex value
     * @param color The color hex code
     * @return Human-readable color name
     */
    private static String getColorLabel(String color) {
        if (color == null) return "None";
        switch(color) {
            case "#FF6B6B": return "Red";
            case "#4ECDC4": return "Teal";
            case "#45B7D1": return "Blue";
            case "#96CEB4": return "Green";
            case "#FFEAA7": return "Yellow";
            case "#DFE6E9": return "Gray";
            case "#A29BFE": return "Purple";
            case "#FD79A8": return "Pink";
            case "#FDCB6E": return "Orange";
            case "#6C5CE7": return "Indigo";
            default: return color;
        }
    }
}
