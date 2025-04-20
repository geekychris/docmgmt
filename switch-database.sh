#!/bin/bash

# Script to switch between H2 and MySQL database configurations
# for the document management application

set -e

# Default MySQL configuration
MYSQL_HOST=${MYSQL_HOST:-localhost}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_DB=${MYSQL_DB:-docmgmt}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-root}

# Define file locations
POM_FILE="pom.xml"
PROPS_FILE="src/main/resources/application.properties"
BACKUP_DIR="config_backups"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Function to display usage information
usage() {
    echo "Usage: $0 [h2|mysql] [options]"
    echo ""
    echo "Arguments:"
    echo "  h2                Switch to H2 database configuration"
    echo "  mysql             Switch to MySQL database configuration"
    echo ""
    echo "Environment variables for MySQL configuration:"
    echo "  MYSQL_HOST        MySQL host (default: localhost)"
    echo "  MYSQL_PORT        MySQL port (default: 3306)"
    echo "  MYSQL_DB          MySQL database name (default: docmgmt)"
    echo "  MYSQL_USER        MySQL username (default: root)"
    echo "  MYSQL_PASSWORD    MySQL password (default: root)"
    echo ""
    echo "Examples:"
    echo "  $0 h2             Switch to H2 database"
    echo "  $0 mysql          Switch to MySQL database with default settings"
    echo "  MYSQL_USER=admin MYSQL_PASSWORD=secret $0 mysql"
    echo "                    Switch to MySQL with custom credentials"
}

# Function to backup files
backup_files() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    echo "Backing up configuration files..."
    
    # Backup pom.xml
    cp "$POM_FILE" "$BACKUP_DIR/pom.xml.${timestamp}"
    
    # Backup application.properties
    cp "$PROPS_FILE" "$BACKUP_DIR/application.properties.${timestamp}"
    
    echo "Backup created at $BACKUP_DIR with timestamp ${timestamp}"
}

# Function to switch to H2
switch_to_h2() {
    echo "Switching to H2 database configuration..."
    
    # Backup files
    backup_files
    
    # Check if MySQL dependency exists in pom.xml
    if grep -q "<groupId>com.mysql</groupId>" "$POM_FILE"; then
        # Remove MySQL dependency from pom.xml (remove the entire dependency block)
        sed -i.tmp '/<!-- MySQL -->/{N;N;N;N;N;d;}' "$POM_FILE"
        rm "${POM_FILE}.tmp"
    fi
    
    # Update application.properties for H2
    cat > "$PROPS_FILE" << EOL
# Database Configuration
spring.datasource.url=jdbc:h2:file:./docmgmt_db;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA / Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# File Upload Configuration
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
spring.servlet.multipart.enabled=true

# Server Configuration
server.port=8082
server.servlet.context-path=/docmgmt

# Logging Configuration
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.com.docmgmt=DEBUG

# Application-specific Configuration
docmgmt.file-storage.temp-dir=./temp-uploads
EOL

    echo "Successfully switched to H2 database configuration"
    echo "You'll need to restart the application for changes to take effect"
    echo "Remember to run 'mvn clean install' to update dependencies"
}

# Function to switch to MySQL
switch_to_mysql() {
    echo "Switching to MySQL database configuration..."
    
    # Backup files
    backup_files
    
    # Check if MySQL dependency exists in pom.xml
    if ! grep -q "<groupId>com.mysql</groupId>" "$POM_FILE"; then
        # Add MySQL dependency to pom.xml after H2 dependency
        sed -i.tmp '/<\/dependency>/{
            /<!-- Database -->/,/<groupId>com.h2database<\/groupId>/{
                /<scope>runtime<\/scope>/{
                    a\
        \
        <!-- MySQL -->\
        <dependency>\
            <groupId>com.mysql</groupId>\
            <artifactId>mysql-connector-j</artifactId>\
            <scope>runtime</scope>\
        </dependency>
                }
            }
        }' "$POM_FILE"
        rm "${POM_FILE}.tmp"
    fi
    
    # Update application.properties for MySQL
    cat > "$PROPS_FILE" << EOL
# Database Configuration
spring.datasource.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=${MYSQL_USER}
spring.datasource.password=${MYSQL_PASSWORD}

# JPA / Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# File Upload Configuration
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
spring.servlet.multipart.enabled=true

# Server Configuration
server.port=8082
server.servlet.context-path=/docmgmt

# Logging Configuration
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.com.docmgmt=DEBUG

# Application-specific Configuration
docmgmt.file-storage.temp-dir=./temp-uploads
EOL

    echo "Successfully switched to MySQL database configuration with:"
    echo "  Host: ${MYSQL_HOST}"
    echo "  Port: ${MYSQL_PORT}"
    echo "  Database: ${MYSQL_DB}"
    echo "  Username: ${MYSQL_USER}"
    echo "  Password: ${MYSQL_PASSWORD}"
    echo ""
    echo "You'll need to restart the application for changes to take effect"
    echo "Remember to run 'mvn clean install' to update dependencies"
    echo "Make sure your MySQL server is running and the database '${MYSQL_DB}' exists"
}

# Main script logic
if [ $# -lt 1 ]; then
    usage
    exit 1
fi

case "$1" in
    h2)
        switch_to_h2
        ;;
    mysql)
        switch_to_mysql
        ;;
    *)
        echo "Error: Invalid database type specified"
        usage
        exit 1
        ;;
esac

exit 0

