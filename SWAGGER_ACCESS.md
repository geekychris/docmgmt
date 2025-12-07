# Swagger UI - Quick Access Guide

## 🚀 Accessing Swagger UI

After starting the application with `mvn spring-boot:run`, access the Swagger UI at:

### **Correct URL:**
```
http://localhost:8082/docmgmt/api/swagger-ui.html
```

### **API Specifications:**
```
JSON: http://localhost:8082/docmgmt/api/v3/api-docs
YAML: http://localhost:8082/docmgmt/api/v3/api-docs.yaml
```

## ⚠️ Important Notes

### Path Configuration
The Swagger UI paths have been configured to avoid conflicts with Vaadin:
- Swagger UI: `/swagger-ui.html` (not `/api/swagger-ui.html`)
- API Docs: `/api-docs` (not `/api/api-docs`)

### Why These Paths?
Vaadin (the UI framework) intercepts certain paths for its own routing. To ensure Swagger UI is accessible, we configured SpringDoc to use paths that don't conflict with Vaadin's routing.

## 🔄 After Configuration Changes

If you've just updated the configuration:

1. **Stop the application** (Ctrl+C in the terminal running Maven)
2. **Restart it:**
   ```bash
   mvn spring-boot:run
   ```
3. **Wait for startup** - Look for: "Started DocumentManagementApplication"
4. **Access Swagger UI:** http://localhost:8082/docmgmt/api/swagger-ui.html

## ✅ Verification

### Check if Swagger UI is accessible:
```bash
curl -I http://localhost:8082/docmgmt/api/swagger-ui.html
```

Expected response: `HTTP/1.1 200` or `HTTP/1.1 302` (redirect)

### Check if API docs are accessible:
```bash
curl http://localhost:8082/docmgmt/api/v3/api-docs | jq .info.title
```

Expected output: `"Document Management System API"`

## 🐛 Troubleshooting

### Issue: 404 Not Found
**Solution:** Ensure the application is running:
```bash
# Check if port 8082 is in use
lsof -i :8082
```

### Issue: Vaadin page appears instead
**Solution:** 
1. Clear browser cache
2. Use the correct URL (without `/api/` prefix for swagger-ui.html)
3. Restart the application

### Issue: Changes not taking effect
**Solution:**
1. Stop the application completely
2. Clean and rebuild:
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

## 📚 Full Documentation

For complete Swagger UI documentation, see:
- [SWAGGER_QUICKSTART.md](SWAGGER_QUICKSTART.md) - Quick start guide
- [docs/OPENAPI_SWAGGER.md](docs/OPENAPI_SWAGGER.md) - Complete guide
- [OPENAPI_FILES.md](OPENAPI_FILES.md) - Working with OpenAPI specs

## 🎯 Quick Test

Once the application is running, try this:

1. Open: http://localhost:8082/docmgmt/api/swagger-ui.html
2. Expand **"Documents"** section
3. Click **"GET /api/documents/latest"**
4. Click **"Try it out"**
5. Click **"Execute"**
6. See the list of documents in the response

That's it! You're now using Swagger UI to test your REST API. 🎉
