package com.docmgmt.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

/**
 * Application shell configuration.
 * Enables Push for async UI updates across the application.
 */
@Push
public class AppShell implements AppShellConfigurator {
}
