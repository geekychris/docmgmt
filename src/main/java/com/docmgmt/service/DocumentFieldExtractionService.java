package com.docmgmt.service;

import com.docmgmt.dto.FieldSuggestionDTO;
import com.docmgmt.model.*;
import com.docmgmt.model.Document.DocumentType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting document fields using AI (Ollama)
 */
@Service
public class DocumentFieldExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentFieldExtractionService.class);
    
    private final ChatClient chatClient;
    private final DocumentService documentService;
    private final ContentService contentService;
    private final ObjectMapper objectMapper;
    
    private static final String EXTRACTION_PROMPT = """
            Analyze the following document content and extract metadata fields.
            
            Document content:
            {content}
            
            IMPORTANT: Provide your response as valid JSON with the following fields:
            
            Required base fields (always include):
            - description: A concise summary of the document (max 500 characters)
            - keywords: Space-separated keywords relevant to the content
            - tags: Array of 3-5 relevant categorization terms
            - documentType: ONE_OF: ARTICLE, REPORT, CONTRACT, MANUAL, PRESENTATION, TRIP_REPORT, OTHER
            
            {typeSpecificFields}
            
            EXTRACTION RULES:
            1. Base fields: Always provide all 4 base fields
            2. Type-specific fields: Extract from content if explicitly mentioned, or infer reasonable values based on context
            3. Dates: Use YYYY-MM-DD format (e.g., "2024-01-15")
            4. Numbers: Provide as numeric values without symbols (e.g., 1500 not "$1,500")
            5. Arrays: Use JSON array format (e.g., ["item1", "item2"])
            6. Missing data: If you cannot determine a type-specific field value, you may omit it OR provide a reasonable estimate
            7. Document type: Analyze content carefully to choose the most appropriate type
            
            RESPOND WITH VALID JSON ONLY - no explanatory text before or after.
            """;
    
    @Autowired
    public DocumentFieldExtractionService(ChatClient.Builder chatClientBuilder,
                                         DocumentService documentService,
                                         ContentService contentService,
                                         ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.documentService = documentService;
        this.contentService = contentService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Extract field suggestions for a document based on its content
     * 
     * @param documentId The document ID
     * @return Field suggestions including current and suggested values
     * @throws IllegalArgumentException if document has no text content
     */
    @Transactional(readOnly = true)
    public FieldSuggestionDTO extractFieldsFromDocument(Long documentId) {
        Document document = documentService.findById(documentId);
        
        // Get text content from the document
        String textContent = getDocumentTextContent(document);
        
        if (textContent == null || textContent.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Document has no text content available for extraction. " +
                "Please upload a text file or transform PDF content to text first using the 'Transform PDF' button."
            );
        }
        
        // Truncate content if too long (keep first 4000 characters to avoid token limits)
        if (textContent.length() > 4000) {
            textContent = textContent.substring(0, 4000) + "...";
        }
        
        // Build current fields with type-specific data
        FieldSuggestionDTO.DocumentFields currentFields = buildCurrentFields(document);
        
        // Extract suggested fields using AI with type-specific prompt
        FieldSuggestionDTO.DocumentFields suggestedFields = extractFieldsUsingAI(textContent, document);
        
        return FieldSuggestionDTO.builder()
                .currentFields(currentFields)
                .suggestedFields(suggestedFields)
                .build();
    }
    
    /**
     * Apply selected field suggestions to a document
     * 
     * @param documentId The document ID
     * @param fieldsToApply Map of field names to boolean indicating whether to apply
     * @param suggestions The field suggestions
     * @return Updated document
     */
    @Transactional
    public Document applyFieldSuggestions(Long documentId, 
                                         Map<String, Boolean> fieldsToApply,
                                         FieldSuggestionDTO.DocumentFields suggestions) {
        Document document = documentService.findById(documentId);
        
        // Apply base fields
        if (Boolean.TRUE.equals(fieldsToApply.get("description")) && suggestions.getDescription() != null) {
            document.setDescription(suggestions.getDescription());
        }
        
        if (Boolean.TRUE.equals(fieldsToApply.get("keywords")) && suggestions.getKeywords() != null) {
            document.setKeywords(suggestions.getKeywords());
        }
        
        if (Boolean.TRUE.equals(fieldsToApply.get("tags")) && suggestions.getTags() != null) {
            document.setTags(new HashSet<>(suggestions.getTags()));
        }
        
        if (Boolean.TRUE.equals(fieldsToApply.get("documentType")) && suggestions.getDocumentType() != null) {
            document.setDocumentType(suggestions.getDocumentType());
        }
        
        // Apply type-specific fields using the interface pattern
        if (document instanceof DocumentFieldExtractor && suggestions.getTypeSpecificFields() != null) {
            Map<String, Object> fieldsToApplyMap = new HashMap<>();
            
            // Filter to only include fields the user selected
            for (Map.Entry<String, Object> entry : suggestions.getTypeSpecificFields().entrySet()) {
                if (Boolean.TRUE.equals(fieldsToApply.get(entry.getKey()))) {
                    fieldsToApplyMap.put(entry.getKey(), entry.getValue());
                }
            }
            
            // Use the interface method to apply type-specific fields
            if (!fieldsToApplyMap.isEmpty()) {
                ((DocumentFieldExtractor) document).applyExtractedFields(fieldsToApplyMap);
            }
        }
        
        return documentService.save(document);
    }
    
    /**
     * Get text content from document's primary content or text rendition
     */
    private String getDocumentTextContent(Document document) {
        List<Content> contents = contentService.findBySysObject(document);
        
        if (contents.isEmpty()) {
            return null;
        }
        
        // First, try to find a text/plain rendition
        Optional<Content> textRendition = contents.stream()
                .filter(c -> "text/plain".equals(c.getContentType()) && c.isIndexable())
                .findFirst();
        
        if (textRendition.isPresent()) {
            try {
                byte[] bytes = contentService.getContentBytes(textRendition.get().getId());
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.warn("Failed to read text rendition: {}", e.getMessage());
            }
        }
        
        // If no text rendition, try primary content if it's text
        Optional<Content> primaryContent = contents.stream()
                .filter(Content::isPrimary)
                .findFirst();
        
        if (primaryContent.isPresent()) {
            Content content = primaryContent.get();
            if (content.getContentType() != null && content.getContentType().startsWith("text/")) {
                try {
                    byte[] bytes = contentService.getContentBytes(content.getId());
                    return new String(bytes, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    logger.warn("Failed to read primary content: {}", e.getMessage());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Build current fields including type-specific fields
     */
    private FieldSuggestionDTO.DocumentFields buildCurrentFields(Document document) {
        Map<String, Object> typeSpecificFields = new HashMap<>();
        
        // Use interface to get type-specific fields if available
        if (document instanceof DocumentFieldExtractor) {
            typeSpecificFields = ((DocumentFieldExtractor) document).getCurrentFieldValues();
        }
        
        return FieldSuggestionDTO.DocumentFields.builder()
                .description(document.getDescription())
                .keywords(document.getKeywords())
                .tags(document.getTags() != null ? new ArrayList<>(document.getTags()) : new ArrayList<>())
                .documentType(document.getDocumentType())
                .typeSpecificFields(typeSpecificFields)
                .build();
    }
    
    /**
     * Get type-specific field definitions for prompt from document
     */
    private String getTypeSpecificFieldDefinitions(Document document) {
        if (document instanceof DocumentFieldExtractor) {
            return ((DocumentFieldExtractor) document).getFieldExtractionRules();
        }
        return "";
    }
    
    /**
     * Extract fields using AI with type-specific prompt
     */
    private FieldSuggestionDTO.DocumentFields extractFieldsUsingAI(String content, Document document) {
        try {
            // Build dynamic prompt with type-specific fields from document
            String typeSpecificDef = getTypeSpecificFieldDefinitions(document);
            String fullPrompt = EXTRACTION_PROMPT.replace("{typeSpecificFields}", typeSpecificDef);
            
            logger.info("Extracting fields for document type: {}", document.getDocumentType());
            logger.debug("Type-specific field definitions added to prompt: {}", typeSpecificDef.length() > 0);
            
            PromptTemplate promptTemplate = new PromptTemplate(fullPrompt);
            Prompt prompt = promptTemplate.create(Map.of("content", content));
            
            String response = chatClient.prompt(prompt).call().content();
            
            logger.info("AI Response received, length: {} chars", response.length());
            logger.debug("AI Response: {}", response);
            
            // Parse the JSON response
            return parseAIResponse(response);
            
        } catch (Exception e) {
            logger.error("Failed to extract fields using AI: {}", e.getMessage(), e);
            // Return empty suggestions on error
            return FieldSuggestionDTO.DocumentFields.builder()
                    .description("Error: Unable to extract fields")
                    .keywords("")
                    .tags(new ArrayList<>())
                    .documentType(DocumentType.OTHER)
                    .build();
        }
    }
    
    /**
     * Parse AI response into DocumentFields
     */
    private FieldSuggestionDTO.DocumentFields parseAIResponse(String response) {
        try {
            // Clean up response - remove markdown code blocks if present
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
            
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            
            String description = jsonNode.has("description") ? jsonNode.get("description").asText() : null;
            String keywords = jsonNode.has("keywords") ? jsonNode.get("keywords").asText() : null;
            
            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                jsonNode.get("tags").forEach(tag -> tags.add(tag.asText()));
            }
            
            DocumentType documentType = DocumentType.OTHER;
            if (jsonNode.has("documentType")) {
                try {
                    documentType = DocumentType.valueOf(jsonNode.get("documentType").asText().toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid document type in AI response: {}", jsonNode.get("documentType").asText());
                }
            }
            
            // Extract type-specific fields from JSON into map
            Map<String, Object> typeSpecificFields = new HashMap<>();
            jsonNode.fieldNames().forEachRemaining(fieldName -> {
                // Skip base fields we've already extracted
                if (!fieldName.equals("description") && !fieldName.equals("keywords") && 
                    !fieldName.equals("tags") && !fieldName.equals("documentType")) {
                    JsonNode fieldValue = jsonNode.get(fieldName);
                    
                    // Convert JSON node to appropriate Java type
                    if (fieldValue.isArray()) {
                        List<String> arrayValues = new ArrayList<>();
                        fieldValue.forEach(item -> arrayValues.add(item.asText()));
                        typeSpecificFields.put(fieldName, arrayValues);
                        logger.debug("Extracted array field {}: {}", fieldName, arrayValues);
                    } else if (fieldValue.isNumber()) {
                        if (fieldValue.isIntegralNumber()) {
                            typeSpecificFields.put(fieldName, fieldValue.asInt());
                            logger.debug("Extracted int field {}: {}", fieldName, fieldValue.asInt());
                        } else {
                            typeSpecificFields.put(fieldName, fieldValue.asDouble());
                            logger.debug("Extracted double field {}: {}", fieldName, fieldValue.asDouble());
                        }
                    } else if (fieldValue.isTextual()) {
                        typeSpecificFields.put(fieldName, fieldValue.asText());
                        logger.debug("Extracted text field {}: {}", fieldName, fieldValue.asText());
                    }
                }
            });
            
            logger.info("Total type-specific fields extracted: {}", typeSpecificFields.size());
            if (!typeSpecificFields.isEmpty()) {
                logger.info("Type-specific fields: {}", typeSpecificFields.keySet());
            }
            
            return FieldSuggestionDTO.DocumentFields.builder()
                    .description(description)
                    .keywords(keywords)
                    .tags(tags)
                    .documentType(documentType)
                    .typeSpecificFields(typeSpecificFields)
                    .build();
                    
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AI response as JSON: {}", e.getMessage());
            logger.debug("Raw response: {}", response);
            
            // Return a fallback response
            return FieldSuggestionDTO.DocumentFields.builder()
                    .description("Error parsing AI response")
                    .keywords("")
                    .tags(new ArrayList<>())
                    .documentType(DocumentType.OTHER)
                    .build();
        }
    }
}
