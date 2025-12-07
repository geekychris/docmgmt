package com.docmgmt.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.docmgmt.ui.views.ContentView;
import com.docmgmt.ui.views.DocumentView;
import com.docmgmt.ui.views.FileStoreView;
import com.docmgmt.ui.views.FolderView;
import com.docmgmt.ui.views.UserView;
import com.docmgmt.ui.views.SearchView;
/**
 * The main layout for the application that contains the navigation drawer
 * and the header with the application title.
 */
public class MainLayout extends AppLayout {

    private final Tabs menu;

    public MainLayout() {
        // Create the main components
        createHeader();
        menu = createMenu();
        
        // Set up the drawer with the menu
        addToDrawer(menu);
        
        // Set drawer to be closed by default on mobile
        setPrimarySection(Section.DRAWER);
    }

    /**
     * Creates the application header with title and drawer toggle
     */
    private void createHeader() {
        H1 appTitle = new H1("Document Management System");
        appTitle.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.NONE
        );

        // Layout for the header content
        HorizontalLayout header = new HorizontalLayout(
            new DrawerToggle(),
            appTitle
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);
    }

    /**
     * Creates the navigation menu with tabs for each main view
     * @return the tabs component containing menu items
     */
    private Tabs createMenu() {
        Tabs tabs = new Tabs();
        tabs.add(
            createMenuTab(VaadinIcon.FOLDER_OPEN, "Documents", DocumentView.class),
            createMenuTab(VaadinIcon.SEARCH, "Search", SearchView.class),
            createMenuTab(VaadinIcon.FOLDER_O, "Folders", FolderView.class),
            createMenuTab(VaadinIcon.DATABASE, "FileStores", FileStoreView.class),
            createMenuTab(VaadinIcon.FILE, "Content", ContentView.class),
            createMenuTab(VaadinIcon.USER, "Users", UserView.class)
        );
        
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        return tabs;
    }

    /**
     * Creates a menu tab with an icon and text
     * @param icon the icon for the tab
     * @param text the label for the tab
     * @param navigationTarget the view class to navigate to
     * @return a configured Tab component
     */
    private Tab createMenuTab(VaadinIcon icon, String text, 
                              Class<? extends Component> navigationTarget) {
        Icon tabIcon = icon.create();
        tabIcon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
        RouterLink link = new RouterLink();
        link.add(tabIcon, new Span(text));
        
        // Set the navigation target for the link
        link.setRoute(navigationTarget);
        
        // Set the page title as a tooltip if available
        PageTitle title = navigationTarget.getAnnotation(PageTitle.class);
        if (title != null) {
            link.setText(title.value());
        }
        
        return new Tab(link);
    }
}

