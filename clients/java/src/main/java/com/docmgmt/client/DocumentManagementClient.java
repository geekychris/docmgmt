package com.docmgmt.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Java client for the Document Management System REST API.
 * 
 * This client demonstrates copy-on-write versioning operations.
 */
public class DocumentManagementClient {
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Create a new client with default base URL.
     */
    public DocumentManagementClient() {
        this("http://[::1]:8082/docmgmt/api");
    }
    
    /**
     * Create a new client with specified base URL.
     * 
     * @param baseUrl Base URL for the API
     */
    public DocumentManagementClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    // =========================================================================
    // Document Operations
    // =========================================================================
    
    /**
     * Create a new document.
     * 
     * @param document Document data
     * @return Created document with ID and version
     * @throws IOException If request fails
     */
    public Map<String, Object> createDocument(Map<String, Object> document) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(document);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Get document by ID.
     * 
     * @param documentId Document ID
     * @return Document data
     * @throws IOException If request fails
     */
    public Map<String, Object> getDocument(long documentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents/" + documentId))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Update document.
     * 
     * @param documentId Document ID
     * @param document Updated document data
     * @return Updated document
     * @throws IOException If request fails
     */
    public Map<String, Object> updateDocument(long documentId, Map<String, Object> document) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(document);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents/" + documentId))
            .header("Content-Type", "application/json")
            .PUT(BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Delete document.
     * 
     * @param documentId Document ID
     * @throws IOException If request fails
     */
    public void deleteDocument(long documentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents/" + documentId))
            .DELETE()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    // =========================================================================
    // Versioning Operations
    // =========================================================================
    
    /**
     * Create a major version (e.g., 1.0 → 2.0).
     * 
     * @param documentId ID of document to version
     * @return New version document
     * @throws IOException If request fails
     */
    public Map<String, Object> createMajorVersion(long documentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents/" + documentId + "/versions/major"))
            .POST(BodyPublishers.noBody())
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Create a minor version (e.g., 1.0 → 1.1).
     * 
     * @param documentId ID of document to version
     * @return New version document
     * @throws IOException If request fails
     */
    public Map<String, Object> createMinorVersion(long documentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents/" + documentId + "/versions/minor"))
            .POST(BodyPublishers.noBody())
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Get version history for a document.
     * 
     * @param documentId Document ID
     * @return List of version information
     * @throws IOException If request fails
     */
    public List<Map<String, Object>> getVersionHistory(long documentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents/" + documentId + "/versions/history"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), List.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    // =========================================================================
    // Content Operations
    // =========================================================================
    
    /**
     * Upload content from bytes.
     * 
     * @param filename Filename
     * @param contentBytes Content bytes
     * @param sysObjectId Parent document/sys_object ID
     * @param storeInDatabase True to store in database, false for file store
     * @param fileStoreId File store ID (required if storeInDatabase is false)
     * @return Created content metadata
     * @throws IOException If request fails
     */
    public Map<String, Object> uploadContent(
            String filename,
            byte[] contentBytes,
            long sysObjectId,
            boolean storeInDatabase,
            Long fileStoreId) throws IOException, InterruptedException {
        
        String boundary = "----" + UUID.randomUUID().toString().replace("-", "");
        
        StringBuilder body = new StringBuilder();
        
        // Add file part
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        body.append("Content-Type: application/octet-stream\r\n\r\n");
        String bodyPrefix = body.toString();
        
        String bodySuffix = "\r\n";
        
        // Add form fields
        bodySuffix += "--" + boundary + "\r\n";
        bodySuffix += "Content-Disposition: form-data; name=\"sysObjectId\"\r\n\r\n";
        bodySuffix += sysObjectId + "\r\n";
        
        bodySuffix += "--" + boundary + "\r\n";
        bodySuffix += "Content-Disposition: form-data; name=\"storeInDatabase\"\r\n\r\n";
        bodySuffix += storeInDatabase + "\r\n";
        
        if (fileStoreId != null) {
            bodySuffix += "--" + boundary + "\r\n";
            bodySuffix += "Content-Disposition: form-data; name=\"fileStoreId\"\r\n\r\n";
            bodySuffix += fileStoreId + "\r\n";
        }
        
        bodySuffix += "--" + boundary + "--\r\n";
        
        // Combine all parts
        byte[] prefixBytes = bodyPrefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = bodySuffix.getBytes(StandardCharsets.UTF_8);
        byte[] fullBody = new byte[prefixBytes.length + contentBytes.length + suffixBytes.length];
        
        System.arraycopy(prefixBytes, 0, fullBody, 0, prefixBytes.length);
        System.arraycopy(contentBytes, 0, fullBody, prefixBytes.length, contentBytes.length);
        System.arraycopy(suffixBytes, 0, fullBody, prefixBytes.length + contentBytes.length, suffixBytes.length);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/content/upload"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(BodyPublishers.ofByteArray(fullBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Upload content from file.
     * 
     * @param file File to upload
     * @param sysObjectId Parent document/sys_object ID
     * @param storeInDatabase True to store in database, false for file store
     * @param fileStoreId File store ID (required if storeInDatabase is false)
     * @return Created content metadata
     * @throws IOException If request fails
     */
    public Map<String, Object> uploadContent(
            File file,
            long sysObjectId,
            boolean storeInDatabase,
            Long fileStoreId) throws IOException, InterruptedException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return uploadContent(file.getName(), bytes, sysObjectId, storeInDatabase, fileStoreId);
    }
    
    /**
     * Get content metadata.
     * 
     * @param contentId Content ID
     * @return Content metadata
     * @throws IOException If request fails
     */
    public Map<String, Object> getContent(long contentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/content/" + contentId))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Download content bytes.
     * 
     * @param contentId Content ID
     * @return Content bytes
     * @throws IOException If request fails
     */
    public byte[] downloadContent(long contentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/content/" + contentId + "/download"))
            .GET()
            .build();
        
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("Request failed with status: " + response.statusCode());
        }
    }
    
    /**
     * Get all content for a document/sys_object.
     * 
     * @param sysObjectId Document/sys_object ID
     * @return List of content metadata
     * @throws IOException If request fails
     */
    public List<Map<String, Object>> getContentBySysObject(long sysObjectId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/content/by-sysobject/" + sysObjectId))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), List.class);
        } else {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
    
    /**
     * Delete content.
     * 
     * @param contentId Content ID
     * @throws IOException If request fails
     */
    public void deleteContent(long contentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/content/" + contentId))
            .DELETE()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
    }
}
