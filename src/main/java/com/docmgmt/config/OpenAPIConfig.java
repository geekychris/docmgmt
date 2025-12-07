package com.docmgmt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for REST API documentation
 */
@Configuration
public class OpenAPIConfig {
    
    @Value("${server.servlet.context-path:/docmgmt}")
    private String contextPath;
    
    @Value("${server.port:8082}")
    private String serverPort;
    
    @Bean
    public OpenAPI documentManagementOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort + contextPath);
        localServer.setDescription("Local development server");
        
        Contact contact = new Contact();
        contact.setName("Document Management System");
        contact.setEmail("support@docmgmt.com");
        
        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");
        
        Info info = new Info()
                .title("Document Management System API")
                .version("1.0.0")
                .contact(contact)
                .description("""
                        A comprehensive Document Management System REST API with features including:
                        
                        - **Document Management**: Create, read, update, and delete documents
                        - **Content Management**: Upload and download content with flexible storage options
                        - **Versioning**: Major and minor version control for documents
                        - **Content Renditions**: Primary and secondary content renditions with transformations
                        - **Full-Text Search**: Lucene-powered search with fielded queries
                        - **File Store Management**: Manage multiple file storage locations
                        - **Content Transformations**: PDF-to-text and other content transformations
                        
                        ## Key Features
                        
                        ### Document Types
                        - ARTICLE, MANUAL, REPORT, SPREADSHEET, PRESENTATION, IMAGE, VIDEO, AUDIO, OTHER
                        
                        ### Storage Options
                        - Database storage for small files
                        - File system storage for large files
                        - Automatic migration between storage types
                        
                        ### Search Capabilities
                        - Simple text search across all fields
                        - Fielded search (name, description, keywords, tags, content)
                        - Boolean operators (AND, OR)
                        - Phrase search, wildcard search
                        - Indexes all document metadata and indexable content
                        
                        ### Content Renditions
                        - Primary renditions (original content)
                        - Secondary renditions (transformed content)
                        - Automatic text extraction from PDFs
                        - Extensible transformer plugin framework
                        """)
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
