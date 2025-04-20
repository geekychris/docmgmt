package com.docmgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Document Management System application.
 * Uses Spring Boot's auto-configuration and component scanning.
 */
@SpringBootApplication
public class DocumentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentManagementApplication.class, args);
    }
}

