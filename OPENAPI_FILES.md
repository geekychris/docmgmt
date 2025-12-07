# OpenAPI Specification Files

## Available Files

Your project now has the complete OpenAPI specification in two formats:

### 📄 openapi.yaml (28 KB, 1104 lines)
YAML format - human-readable, great for:
- Manual editing
- Version control diffs
- Documentation tools
- Schema validation

### 📄 openapi.json (21 KB)
JSON format - machine-readable, great for:
- Client SDK generation
- API testing tools
- Automated processing
- Import into tools like Postman

## Where These Come From

These files are **generated dynamically** by SpringDoc when the application runs. They are automatically created from:
- Controller annotations (@Operation, @Tag, @Parameter)
- OpenAPI configuration (OpenAPIConfig.java)
- Spring REST endpoints
- Data models and DTOs

## Accessing Live Specs

When the application is running, you can always get the latest specs:

**YAML:**
```bash
curl http://localhost:8082/docmgmt/api/v3/api-docs.yaml
```

**JSON:**
```bash
curl http://localhost:8082/docmgmt/api/v3/api-docs
```

## Regenerating Files

To regenerate the specification files after making changes:

```bash
# Start the application
mvn spring-boot:run

# In another terminal, download the specs
curl http://localhost:8082/docmgmt/api/v3/api-docs.yaml -o openapi.yaml
curl http://localhost:8082/docmgmt/api/v3/api-docs -o openapi.json
```

## Using the Specs

### 1. Import into Postman
1. Open Postman
2. File → Import
3. Select `openapi.yaml` or `openapi.json`
4. All endpoints will be imported as a collection

### 2. Generate Client SDK

**Python:**
```bash
openapi-generator-cli generate \
  -i openapi.yaml \
  -g python \
  -o ./generated-clients/python
```

**TypeScript:**
```bash
openapi-generator-cli generate \
  -i openapi.yaml \
  -g typescript-axios \
  -o ./generated-clients/typescript
```

**Java:**
```bash
openapi-generator-cli generate \
  -i openapi.yaml \
  -g java \
  -o ./generated-clients/java
```

### 3. Validate API Compliance

```bash
# Install swagger-cli
npm install -g @apidevtools/swagger-cli

# Validate the spec
swagger-cli validate openapi.yaml
```

### 4. Generate Documentation

```bash
# Generate static HTML documentation
npx redoc-cli bundle openapi.yaml -o api-docs.html
```

## Specification Contents

The OpenAPI spec includes:

### Info
- Title: Document Management System API
- Version: 1.0.0
- Description: Comprehensive API documentation
- Contact: support@docmgmt.com
- License: Apache 2.0

### Tags (4 groups)
1. **Documents** - 13 endpoints
2. **Search** - 4 endpoints
3. **Content** - 7 endpoints
4. **File Stores** - 11 endpoints

### Components
- 20+ schema definitions (DTOs, models)
- Request/response examples
- Error responses

## Version Control

**Should you commit these files?**

**Option 1: Don't commit** (Recommended)
- Add to `.gitignore`
- Generate fresh each time from running application
- Always up-to-date with code

**Option 2: Commit**
- Track API changes over time
- Enable API versioning
- Allow offline access
- Useful for external consumers

If committing, add this to your build process:
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.4</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Tools That Use OpenAPI

- **Postman** - API testing
- **Insomnia** - API client
- **Swagger UI** - Interactive documentation (already integrated!)
- **ReDoc** - Beautiful documentation
- **OpenAPI Generator** - Client SDK generation
- **Spectral** - API linting
- **Prism** - Mock server
- **Bruno** - Open source API client

## Resources

- OpenAPI Specification: https://swagger.io/specification/
- OpenAPI Generator: https://openapi-generator.tech/
- Swagger Editor: https://editor.swagger.io/
- ReDoc: https://github.com/Redocly/redoc

## Quick Access

- **Swagger UI**: http://localhost:8082/docmgmt/api/swagger-ui.html
- **YAML Spec**: http://localhost:8082/docmgmt/api/v3/api-docs.yaml
- **JSON Spec**: http://localhost:8082/docmgmt/api/v3/api-docs
