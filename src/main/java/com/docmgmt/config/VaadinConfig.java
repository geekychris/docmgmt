package com.docmgmt.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Vaadin configuration.
 * 
 * Note: Vaadin automatically excludes paths starting with /api from its routing,
 * so Swagger UI at /api/swagger-ui.html is accessible without additional configuration.
 */
@Component
public class VaadinConfig implements VaadinServiceInitListener {
    
    @Override
    public void serviceInit(ServiceInitEvent event) {
        // Vaadin automatically excludes /api/* paths from routing
        // No additional configuration needed for Swagger UI access
    }
}
