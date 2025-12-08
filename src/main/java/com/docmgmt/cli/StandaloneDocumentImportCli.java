package com.docmgmt.cli;

import com.docmgmt.dto.ContentDTO;
import com.docmgmt.dto.DocumentDTO;
import com.docmgmt.dto.FileStoreDTO;
import com.docmgmt.dto.FolderDTO;
import com.docmgmt.model.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone CLI tool for bulk importing documents into the Document Management System via REST API.
 * This CLI does NOT require database access - it only makes REST calls to a running server.
 * 
 * Usage:
 *   java -cp document-management.jar com.docmgmt.cli.StandaloneDocumentImportCli \
 *     --api-base-url=http://localhost:8080 \
 *     --root-dir=/path/to/docs \
 *     --file-types=pdf,docx,txt \
 *     --filestore-id=1
 */
public class StandaloneDocumentImportCli {
    
    private static final Logger logger = LoggerFactory.getLogger(StandaloneDocumentImportCli.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Configuration properties
    private String apiBaseUrl = "http://localhost:8082/docmgmt";
    private String rootDir;
    private Set<String> fileTypes;
    private Long fileStoreId;
    private boolean createFolders = true;
    
    // Statistics
    private final AtomicInteger foldersCreated = new AtomicInteger(0);
    private final AtomicInteger documentsCreated = new AtomicInteger(0);
    private final AtomicInteger filesUploaded = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    
    public StandaloneDocumentImportCli() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public static void main(String[] args) {
        StandaloneDocumentImportCli cli = new StandaloneDocumentImportCli();
        
        try {
            cli.run(args);
            System.exit(0);
        } catch (Exception e) {
            logger.error("Import failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    public void run(String[] args) throws Exception {
        logger.info("=".repeat(80));
        logger.info("Standalone Document Import CLI");
        logger.info("=".repeat(80));
        
        // Parse arguments
        parseArguments(args);
        
        // Validate configuration
        validateConfiguration();
        
        // Display configuration
        displayConfiguration();
        
        // Perform import
        performImport();
        
        // Display statistics
        displayStatistics();
        
        logger.info("Import completed successfully!");
    }
    
    private void parseArguments(String[] args) {
        logger.info("Parsing command line arguments...");
        
        Map<String, String> argMap = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    argMap.put(parts[0], parts[1]);
                }
            }
        }
        
        // API base URL
        apiBaseUrl = argMap.getOrDefault("api-base-url", "http://localhost:8082/docmgmt");
        
        // Required arguments
        rootDir = argMap.get("root-dir");
        if (rootDir == null) {
            throw new IllegalArgumentException("Missing required argument: --root-dir");
        }
        
        // File types
        String fileTypesStr = argMap.getOrDefault("file-types", "pdf,docx,txt,doc,rtf,md");
        fileTypes = new HashSet<>(Arrays.asList(fileTypesStr.toLowerCase().split(",")));
        
        // File store ID (optional)
        String fileStoreIdStr = argMap.get("filestore-id");
        if (fileStoreIdStr != null) {
            fileStoreId = Long.parseLong(fileStoreIdStr);
        }
        
        // Optional flags
        createFolders = Boolean.parseBoolean(argMap.getOrDefault("create-folders", "true"));
    }
    
    private void validateConfiguration() throws IOException {
        logger.info("Validating configuration...");
        
        // Check API connectivity by checking if /api/documents endpoint is accessible
        try {
            String url = apiBaseUrl + "/api/documents";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("API server not available at: " + apiBaseUrl);
            }
            logger.info("Connected to API server at: {}", apiBaseUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to connect to API server at: " + apiBaseUrl + ". " +
                "Make sure the server is running.", e);
        }
        
        // Check if root directory exists
        Path root = Paths.get(rootDir);
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Root directory does not exist: " + rootDir);
        }
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root path is not a directory: " + rootDir);
        }
        
        // Check if file store exists (if specified)
        if (fileStoreId != null) {
            try {
                String url = apiBaseUrl + "/api/filestores/" + fileStoreId;
                ResponseEntity<FileStoreDTO> response = restTemplate.getForEntity(url, FileStoreDTO.class);
                FileStoreDTO fileStore = response.getBody();
                if (fileStore != null) {
                    logger.info("Using file store: {} ({})", fileStore.getName(), fileStore.getRootPath());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("File store not found: " + fileStoreId, e);
            }
        } else {
            logger.info("No file store specified - content will be stored in database");
        }
    }
    
    private void displayConfiguration() {
        logger.info("");
        logger.info("Configuration:");
        logger.info("  API Base URL: {}", apiBaseUrl);
        logger.info("  Root Directory: {}", rootDir);
        logger.info("  File Types: {}", fileTypes);
        logger.info("  File Store ID: {}", fileStoreId != null ? fileStoreId : "N/A (database storage)");
        logger.info("  Create Folders: {}", createFolders);
        logger.info("");
    }
    
    private void performImport() throws IOException {
        logger.info("Starting import from: {}", rootDir);
        logger.info("");
        
        Path root = Paths.get(rootDir);
        Map<Path, FolderDTO> folderCache = new HashMap<>();
        
        // Walk the directory tree
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    // Check if file type matches
                    String extension = getFileExtension(file.getFileName().toString());
                    if (fileTypes.contains(extension.toLowerCase())) {
                        processFile(file, root, folderCache);
                    }
                } catch (Exception e) {
                    logger.error("Error processing file {}: {}", file, e.getMessage());
                    errors.incrementAndGet();
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Failed to access file: {}", file);
                errors.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void processFile(Path filePath, Path rootPath, Map<Path, FolderDTO> folderCache) throws IOException {
        logger.info("Processing: {}", filePath);
        
        // Get or create folder structure
        FolderDTO folder = null;
        if (createFolders) {
            Path relativePath = rootPath.relativize(filePath.getParent());
            folder = getOrCreateFolder(relativePath, rootPath, folderCache);
        }
        
        // Create document
        DocumentDTO document = createDocument(filePath, folder);
        documentsCreated.incrementAndGet();
        
        // Upload primary content
        ContentDTO primaryContent = uploadContent(filePath, document);
        filesUploaded.incrementAndGet();
        
        logger.info("  ✓ Created document ID: {}", document.getId());
    }
    
    private FolderDTO getOrCreateFolder(Path relativePath, Path rootPath, Map<Path, FolderDTO> cache) {
        if (relativePath.getNameCount() == 0) {
            return null; // Root level
        }
        
        // Check cache
        if (cache.containsKey(relativePath)) {
            return cache.get(relativePath);
        }
        
        String folderPath = "/" + relativePath.toString().replace(File.separator, "/");
        
        // Try to find existing folder by path
        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/api/folders/by-path")
                    .queryParam("path", folderPath)
                    .toUriString();
            ResponseEntity<FolderDTO[]> response = restTemplate.getForEntity(url, FolderDTO[].class);
            FolderDTO[] folders = response.getBody();
            if (folders != null && folders.length > 0) {
                FolderDTO folder = folders[0];
                cache.put(relativePath, folder);
                return folder;
            }
        } catch (Exception e) {
            // Folder doesn't exist, will create below
        }
        
        // Get or create parent folder
        FolderDTO parentFolder = null;
        if (relativePath.getNameCount() > 1) {
            Path parentPath = relativePath.getParent();
            parentFolder = getOrCreateFolder(parentPath, rootPath, cache);
        }
        
        // Create this folder
        String folderName = relativePath.getFileName().toString();
        FolderDTO folderDTO = new FolderDTO();
        folderDTO.setName(folderName);
        folderDTO.setPath(folderPath);
        folderDTO.setDescription("Imported from: " + rootPath.resolve(relativePath));
        folderDTO.setIsPublic(false);
        
        if (parentFolder != null) {
            folderDTO.setParentFolderId(parentFolder.getId());
        }
        
        // Create folder via REST API
        try {
            String url = apiBaseUrl + "/api/folders";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<FolderDTO> request = new HttpEntity<>(folderDTO, headers);
            
            ResponseEntity<FolderDTO> response = restTemplate.postForEntity(url, request, FolderDTO.class);
            FolderDTO createdFolder = response.getBody();
            
            if (createdFolder != null) {
                cache.put(relativePath, createdFolder);
                foldersCreated.incrementAndGet();
                logger.info("  ✓ Created folder: {}", createdFolder.getPath());
                return createdFolder;
            }
        } catch (Exception e) {
            logger.error("Failed to create folder {}: {}", folderPath, e.getMessage());
            throw new RuntimeException("Failed to create folder", e);
        }
        
        return null;
    }
    
    private DocumentDTO createDocument(Path filePath, FolderDTO folder) {
        String fileName = filePath.getFileName().toString();
        String extension = getFileExtension(fileName);
        String nameWithoutExt = fileName.substring(0, fileName.length() - extension.length() - 1);
        
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(nameWithoutExt);
        documentDTO.setDocumentType(inferDocumentType(extension));
        documentDTO.setDescription("Imported from: " + filePath);
        documentDTO.setKeywords(extension + " import");
        
        Set<String> tags = new HashSet<>();
        tags.add("imported");
        tags.add(extension);
        documentDTO.setTags(tags);
        
        // Create document via REST API
        try {
            String url = apiBaseUrl + "/api/documents";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DocumentDTO> request = new HttpEntity<>(documentDTO, headers);
            
            ResponseEntity<DocumentDTO> response = restTemplate.postForEntity(url, request, DocumentDTO.class);
            DocumentDTO createdDocument = response.getBody();
            
            // Add document to folder if specified
            if (createdDocument != null && folder != null) {
                try {
                    String addItemUrl = apiBaseUrl + "/api/folders/" + folder.getId() + "/items/" + createdDocument.getId();
                    restTemplate.put(addItemUrl, null);
                    logger.debug("  Added document to folder: {}", folder.getPath());
                } catch (Exception folderEx) {
                    logger.warn("  Could not add document {} to folder {}: {}", 
                        createdDocument.getId(), folder.getId(), folderEx.getMessage());
                    // Don't fail the entire document creation if adding to folder fails
                }
            }
            
            return createdDocument;
        } catch (Exception e) {
            logger.error("Failed to create document {}: {}", nameWithoutExt, e.getMessage());
            throw new RuntimeException("Failed to create document", e);
        }
    }
    
    private ContentDTO uploadContent(Path filePath, DocumentDTO document) throws IOException {
        String fileName = filePath.getFileName().toString();
        
        // Upload content via REST API
        try {
            String url = apiBaseUrl + "/api/content/upload";
            
            // Prepare multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(filePath.toFile()));
            body.add("sysObjectId", document.getId());
            if (fileStoreId != null) {
                body.add("fileStoreId", fileStoreId);
                body.add("storageType", "FILESTORE");
            } else {
                body.add("storageType", "DATABASE");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<ContentDTO> response = restTemplate.postForEntity(url, requestEntity, ContentDTO.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to upload content for {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Failed to upload content", e);
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private Document.DocumentType inferDocumentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf", "docx", "doc" -> Document.DocumentType.ARTICLE;
            case "txt", "md" -> Document.DocumentType.ARTICLE;
            case "xlsx", "xls", "csv" -> Document.DocumentType.REPORT;
            case "pptx", "ppt" -> Document.DocumentType.PRESENTATION;
            case "jpg", "jpeg", "png", "gif" -> Document.DocumentType.OTHER;
            case "mp4", "avi", "mov" -> Document.DocumentType.OTHER;
            case "mp3", "wav" -> Document.DocumentType.OTHER;
            default -> Document.DocumentType.OTHER;
        };
    }
    
    private void displayStatistics() {
        logger.info("");
        logger.info("=".repeat(80));
        logger.info("Import Statistics");
        logger.info("=".repeat(80));
        logger.info("  Folders Created:     {}", foldersCreated.get());
        logger.info("  Documents Created:   {}", documentsCreated.get());
        logger.info("  Files Uploaded:      {}", filesUploaded.get());
        logger.info("  Errors:              {}", errors.get());
        logger.info("=".repeat(80));
    }
}
