package com.docmgmt.cli;

import com.docmgmt.dto.ContentDTO;
import com.docmgmt.dto.DocumentDTO;
import com.docmgmt.dto.FileStoreDTO;
import com.docmgmt.dto.FolderDTO;
import com.docmgmt.model.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
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
 * CLI tool for bulk importing documents into the Document Management System via REST API.
 * This CLI connects to a running instance of the document management server.
 * 
 * Usage:
 *   java -jar document-management.jar --spring.profiles.active=import \
 *     --import.api-base-url=http://localhost:8080 \
 *     --import.root-dir=/path/to/docs \
 *     --import.file-types=pdf,docx,txt \
 *     --import.filestore-id=1 \
 *     --import.transform=true
 *
 * Or with Maven:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=import \
 *     -Dspring-boot.run.arguments="--import.api-base-url=http://localhost:8080 --import.root-dir=/path/to/docs"
 */
@Component
@Profile("import")
public class DocumentImportCli implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentImportCli.class);
    
    @Value("${import.api-base-url:http://localhost:8080}")
    private String apiBaseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConfigurableApplicationContext context;
    
    public DocumentImportCli(ConfigurableApplicationContext context) {
        this.context = context;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For Java 8 date/time support
    }
    
    // Configuration properties
    private String rootDir;
    private Set<String> fileTypes;
    private Long fileStoreId;
    private boolean transformToText = true;
    private boolean indexDocuments = true;
    private boolean createFolders = true;
    
    // Statistics
    private final AtomicInteger foldersCreated = new AtomicInteger(0);
    private final AtomicInteger documentsCreated = new AtomicInteger(0);
    private final AtomicInteger filesUploaded = new AtomicInteger(0);
    private final AtomicInteger transformations = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("=".repeat(80));
        logger.info("Document Import CLI");
        logger.info("=".repeat(80));
        
        try {
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
            
        } catch (Exception e) {
            logger.error("Import failed: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            // Shutdown the application context
            SpringApplication.exit(context, () -> 0);
        }
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
        
        // Required arguments
        rootDir = argMap.get("import.root-dir");
        if (rootDir == null) {
            throw new IllegalArgumentException("Missing required argument: --import.root-dir");
        }
        
        // File types
        String fileTypesStr = argMap.getOrDefault("import.file-types", "pdf,docx,txt,doc,rtf,md");
        fileTypes = new HashSet<>(Arrays.asList(fileTypesStr.toLowerCase().split(",")));
        
        // File store ID (optional)
        String fileStoreIdStr = argMap.get("import.filestore-id");
        if (fileStoreIdStr != null) {
            fileStoreId = Long.parseLong(fileStoreIdStr);
        }
        
        // Optional flags
        transformToText = Boolean.parseBoolean(argMap.getOrDefault("import.transform", "true"));
        indexDocuments = Boolean.parseBoolean(argMap.getOrDefault("import.index", "true"));
        createFolders = Boolean.parseBoolean(argMap.getOrDefault("import.create-folders", "true"));
    }
    
    private void validateConfiguration() throws IOException {
        logger.info("Validating configuration...");
        
        // Check API connectivity
        try {
            String url = apiBaseUrl + "/actuator/health";
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
        logger.info("  Root Directory: {}", rootDir);
        logger.info("  File Types: {}", fileTypes);
        logger.info("  File Store ID: {}", fileStoreId != null ? fileStoreId : "N/A (database storage)");
        logger.info("  Transform to Text: {}", transformToText);
        logger.info("  Index Documents: {}", indexDocuments);
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
        
        // Note: Transform and index operations would need separate API endpoints
        // For now, these are skipped. The server can handle transformation and indexing
        // through its own background processes or triggered separately.
        
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
                String addItemUrl = apiBaseUrl + "/api/folders/" + folder.getId() + "/items/" + createdDocument.getId();
                restTemplate.put(addItemUrl, null);
            }
            
            return createdDocument;
        } catch (Exception e) {
            logger.error("Failed to create document {}: {}", nameWithoutExt, e.getMessage());
            throw new RuntimeException("Failed to create document", e);
        }
    }
    
    private ContentDTO uploadContent(Path filePath, DocumentDTO document) throws IOException {
        String fileName = filePath.getFileName().toString();
        String contentType = inferContentType(fileName);
        
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
    
    private boolean isTransformable(Path filePath) {
        String extension = getFileExtension(filePath.getFileName().toString()).toLowerCase();
        return extension.equals("pdf");
    }
    
    private boolean isTextBased(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return extension.equals("txt") || extension.equals("md") || 
               extension.equals("rtf") || extension.equals("text");
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
    
    private String inferContentType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "rtf" -> "application/rtf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "csv" -> "text/csv";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
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
        logger.info("  Transformations:     {}", transformations.get());
        logger.info("  Errors:              {}", errors.get());
        logger.info("=".repeat(80));
    }
    
}
