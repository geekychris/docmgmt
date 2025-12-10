package com.docmgmt.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for how documents should be displayed as tiles in a folder view
 */
@Entity
@Table(name = "tile_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"folder"})
@ToString(exclude = {"folder"})
public class TileConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The folder this configuration applies to
     */
    @OneToOne
    @JoinColumn(name = "folder_id", unique = true)
    private Folder folder;
    
    /**
     * Whether tiles should be grouped by subfolder
     */
    @Column(name = "group_by_subfolder")
    @Builder.Default
    private Boolean groupBySubfolder = false;
    
    /**
     * Fields to display on each tile (comma-separated)
     * e.g., "name,description,url,documentType,tags"
     */
    @Column(name = "visible_fields", columnDefinition = "TEXT")
    @Builder.Default
    private String visibleFields = "name,description,url";
    
    /**
     * Color coding strategy: NONE, BY_FOLDER, BY_TYPE, BY_TAG, CUSTOM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "color_strategy")
    @Builder.Default
    private ColorStrategy colorStrategy = ColorStrategy.NONE;
    
    /**
     * Custom color mappings for subfolders or tags (JSON format)
     * e.g., {"subfolder1": "#FF5733", "subfolder2": "#33FF57"}
     */
    @Column(name = "color_mappings", columnDefinition = "TEXT")
    private String colorMappings;
    
    /**
     * Tile size: SMALL, MEDIUM, LARGE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tile_size")
    @Builder.Default
    private TileSize tileSize = TileSize.MEDIUM;
    
    /**
     * Whether to show the link to document detail view
     */
    @Column(name = "show_detail_link")
    @Builder.Default
    private Boolean showDetailLink = true;
    
    /**
     * Whether to show external URL link
     */
    @Column(name = "show_url_link")
    @Builder.Default
    private Boolean showUrlLink = true;
    
    /**
     * Sort order for tiles: NAME, CREATED_DATE, MODIFIED_DATE, TYPE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sort_order")
    @Builder.Default
    private SortOrder sortOrder = SortOrder.NAME;
    
    /**
     * Whether to hide the left navigation panel (MainLayout's drawer)
     */
    @Column(name = "hide_navigation")
    @Builder.Default
    private Boolean hideNavigation = false;
    
    /**
     * Whether to hide edit buttons on tiles (view and edit actions)
     */
    @Column(name = "hide_edit_buttons")
    @Builder.Default
    private Boolean hideEditButtons = false;
    
    /**
     * Background color opacity (0.0 to 1.0, default 0.05)
     * Controls the intensity of the colored background gradient
     */
    @Column(name = "background_color_opacity")
    @Builder.Default
    private Double backgroundColorOpacity = 0.05;
    
    public enum ColorStrategy {
        NONE,           // No color coding
        BY_FOLDER,      // Color by subfolder
        BY_TYPE,        // Color by document type
        BY_TAG,         // Color by first tag
        CUSTOM          // Use custom color mappings
    }
    
    public enum TileSize {
        SMALL,
        MEDIUM,
        LARGE
    }
    
    public enum SortOrder {
        NAME,
        CREATED_DATE,
        MODIFIED_DATE,
        TYPE
    }
}
