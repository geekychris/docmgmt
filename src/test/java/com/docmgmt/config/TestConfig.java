package com.docmgmt.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test configuration for the document management system
 * Provides test-specific beans and configuration
 */
@TestConfiguration
public class TestConfig {

    /**
     * Creates a test-specific data source
     * This ensures tests use a dedicated in-memory database
     * @return DataSource configured for testing
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * Creates a test-specific temporary directory for file storage
     * This ensures tests don't interfere with the application's files
     * @return Path to the test-specific temporary directory
     * @throws IOException if directory creation fails
     */
    @Bean
    @Primary
    public Path testTempDirectory() throws IOException {
        Path tempDir = Paths.get("./target/test-temp");
        Files.createDirectories(tempDir);
        return tempDir;
    }
}

