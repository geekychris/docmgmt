# Database Switcher Tool

## Overview

The Database Switcher Script (`switch-database.sh`) is a utility designed to simplify switching between H2 (embedded) and MySQL database configurations for the Document Management application. This tool automatically updates the necessary configuration files (`pom.xml` and `application.properties`) and creates backups of your original configurations to ensure you can always revert changes if needed.

Key features:
- Simple command-line switching between H2 and MySQL databases
- Automatic backup of configuration files before making changes
- Customizable MySQL connection parameters via environment variables
- Error handling and validation to prevent common mistakes

## When to Use Each Database

The Document Management application supports both H2 and MySQL databases, each with its own advantages for different scenarios:

### H2 Database

H2 is an embedded, in-memory database that is ideal for:

- **Development**: Quick setup with no external dependencies
- **Testing**: Fast execution of tests with isolated database instances
- **Demonstrations**: Easy to deploy for demos without additional setup
- **Single-user scenarios**: Works well when only one user needs access
- **Prototyping**: Rapid development without configuring external services
- **CI/CD pipelines**: Simplifies automated testing environments

### MySQL Database

MySQL is a robust, client-server database system that excels in:

- **Production environments**: Provides better durability and reliability
- **Multi-user access**: Supports concurrent access from multiple application instances
- **Larger datasets**: More efficient for storing and querying large volumes of documents
- **Data persistence**: Better for long-term storage of important document data
- **Scalability**: Can be configured for high availability and replication
- **Integration**: Often required when integrating with other enterprise systems
- **Performance monitoring**: Offers more tools for monitoring and optimization

This switching tool allows you to easily toggle between these database types based on your current needs without manually editing configuration files.

## Prerequisites

Before using this script, ensure you have:

1. Bash shell environment (Linux, macOS, or Windows with WSL/Git Bash)
2. A running instance of the Document Management application
3. Maven installed for dependency management
4. For MySQL mode: A running MySQL server instance with appropriate permissions

## Configuration

### Default Settings

The script uses the following default settings for MySQL:
- Host: `localhost`
- Port: `3306`
- Database name: `docmgmt`
- Username: `root`
- Password: `root`

### Customizing MySQL Configuration

You can customize the MySQL configuration using environment variables:

| Environment Variable | Description | Default Value |
|----------------------|-------------|---------------|
| `MYSQL_HOST` | MySQL server hostname or IP address | localhost |
| `MYSQL_PORT` | MySQL server port | 3306 |
| `MYSQL_DB` | MySQL database name | docmgmt |
| `MYSQL_USER` | MySQL username | root |
| `MYSQL_PASSWORD` | MySQL password | root |

## Usage

### Basic Usage

```bash
# Make the script executable (only needed once)
chmod +x switch-database.sh

# Switch to H2 database
./switch-database.sh h2

# Switch to MySQL database with default settings
./switch-database.sh mysql
```

### Advanced Usage with Custom MySQL Settings

```bash
# Using environment variables for custom MySQL configuration
MYSQL_HOST=192.168.1.100 MYSQL_PORT=3307 MYSQL_DB=doc_management MYSQL_USER=admin MYSQL_PASSWORD=secure_password ./switch-database.sh mysql

# Alternatively, you can export variables first
export MYSQL_HOST=192.168.1.100
export MYSQL_PORT=3307
export MYSQL_DB=doc_management
export MYSQL_USER=admin
export MYSQL_PASSWORD=secure_password
./switch-database.sh mysql
```

### Viewing Usage Information

```bash
./switch-database.sh
```

## Backup and Recovery

### Automatic Backups

The script automatically creates backups of your configuration files before making any changes. Backups are stored in the `config_backups` directory with timestamps in the following format:

```
config_backups/
  ├── application.properties.YYYYMMDD_HHMMSS
  └── pom.xml.YYYYMMDD_HHMMSS
```

### Manual Recovery

If you need to revert to a previous configuration:

1. Find the desired backup files in the `config_backups` directory
2. Copy them back to their original locations:

```bash
# Replace TIMESTAMP with the actual timestamp of your backup
cp config_backups/pom.xml.TIMESTAMP pom.xml
cp config_backups/application.properties.TIMESTAMP src/main/resources/application.properties
```

## Troubleshooting

### Common Issues

1. **Script fails with "Permission denied"**
   ```bash
   chmod +x switch-database.sh
   ```

2. **MySQL connection fails**
   - Verify MySQL server is running
   - Check that the database exists
   - Ensure username and password are correct
   - Confirm host and port settings
   - Make sure the user has necessary permissions

3. **Changes not taking effect**
   - Remember to run `mvn clean install` after switching databases
   - Restart the application after switching

4. **Sed command fails**
   - If using macOS and the script fails with sed errors, you may need to install GNU sed:
     ```bash
     brew install gnu-sed
     ```
     Then modify the script to use `gsed` instead of `sed`

### Logs and Debugging

The script displays status information during execution. If you encounter issues, consider running with bash's verbose mode for additional debugging information:

```bash
bash -x ./switch-database.sh mysql
```

## Important Notes

1. **Database Migration**: This script only changes the configuration. It does not migrate data between databases. If you have existing data in the H2 database that you want to preserve in MySQL, you will need to handle the data migration separately.

2. **Database Schema**: When switching database types, the application's Hibernate configuration will attempt to create or update the schema automatically (`spring.jpa.hibernate.ddl-auto=update`). This works for simple applications but may not be suitable for production environments or complex schemas.

3. **Application Restart**: After switching the database configuration, you must restart the Document Management application for changes to take effect.

4. **Maven Dependencies**: After switching, you should run `mvn clean install` to update the dependencies.

5. **MySQL Database Creation**: The script does not create the MySQL database for you. Ensure the specified database exists before switching to MySQL mode.

6. **Production Use**: This script is primarily intended for development and testing environments. For production deployments, consider using more robust configuration management approaches.

7. **Backup Strategy**: While the script creates backups, you should implement a proper backup strategy for your database data, especially in production environments.

