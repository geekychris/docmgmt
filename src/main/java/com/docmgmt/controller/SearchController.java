package com.docmgmt.controller;

import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.search.SearchResult;
import com.docmgmt.search.SearchResultsWrapper;
import com.docmgmt.service.DocumentService;
import com.docmgmt.service.DocumentSimilarityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Full-text search operations using Apache Lucene")
public class SearchController {
    
    @Autowired
    private LuceneIndexService searchService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private DocumentSimilarityService similarityService;
    
    @Operation(
        summary = "Simple text search",
        description = "Search across all document fields (name, description, keywords, tags, content) and indexable content using Lucene query syntax. Supports wildcards (*), phrase queries (\"exact phrase\"), and boolean operators (AND, OR, NOT)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query syntax")
    })
    @GetMapping
    public ResponseEntity<SearchResultsWrapper> search(
            @Parameter(description = "Search query using Lucene syntax", required = true, 
                      example = "spring framework") 
            @RequestParam String q,
            @Parameter(description = "Maximum number of results to return", example = "50") 
            @RequestParam(defaultValue = "50") int limit) {
        try {
            SearchResultsWrapper results = searchService.search(q, limit);
            return ResponseEntity.ok(results);
        } catch (IOException | ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Field-specific search",
        description = "Search specific fields with individual queries. Supports combining multiple field queries with AND or OR operators."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query syntax")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Map of field names to search queries",
        required = true,
        content = @io.swagger.v3.oas.annotations.media.Content(
            schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(
                name = "Multi-field search",
                value = "{\"name\": \"tutorial\", \"tags\": \"java\"}"
            )
        )
    )
    @PostMapping("/fields")
    public ResponseEntity<SearchResultsWrapper> searchFields(
            @RequestBody Map<String, String> fieldQueries,
            @Parameter(description = "Maximum number of results", example = "50") 
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Boolean operator: AND or OR", example = "AND") 
            @RequestParam(defaultValue = "AND") String operator) {
        try {
            BooleanClause.Occur occur = "OR".equalsIgnoreCase(operator) ? 
                BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST;
            
            SearchResultsWrapper results = searchService.searchFieldsWithOperator(
                fieldQueries, occur, limit);
            return ResponseEntity.ok(results);
        } catch (IOException | ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Rebuild search index",
        description = "Completely rebuild the Lucene search index from all documents in the database. This indexes all document metadata and indexable content."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Index rebuilt successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to rebuild index")
    })
    @PostMapping("/rebuild")
    public ResponseEntity<String> rebuildIndex() {
        try {
            searchService.rebuildIndex(documentService.findAll());
            return ResponseEntity.ok("Index rebuilt successfully");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to rebuild index");
        }
    }
    
    @Operation(
        summary = "Get search index statistics",
        description = "Retrieve statistics about the Lucene search index including document count and deleted documents."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Error retrieving statistics")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            return ResponseEntity.ok(searchService.getIndexStats());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
        summary = "Similarity search by document",
        description = "Find documents similar to a given document using vector embeddings and cosine similarity"
    )
    @GetMapping("/similar/{documentId}")
    public ResponseEntity<List<Map<String, Object>>> findSimilar(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<DocumentSimilarityService.SimilarityResult> results = 
                similarityService.findSimilar(documentId, limit);
            
            List<Map<String, Object>> response = results.stream()
                .map(r -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("documentId", r.getDocument().getId());
                    map.put("name", r.getDocument().getName());
                    map.put("similarity", r.getSimilarity());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
        summary = "Similarity search by text query",
        description = "Find documents similar to the provided text using vector embeddings"
    )
    @PostMapping("/similar")
    public ResponseEntity<List<Map<String, Object>>> findSimilarByText(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestBody(required = false) Map<String, String> filters) {
        try {
            List<DocumentSimilarityService.SimilarityResult> results;
            
            if (filters != null && !filters.isEmpty()) {
                results = similarityService.findSimilarByTextWithFilters(q, filters, limit);
            } else {
                results = similarityService.findSimilarByText(q, limit);
            }
            
            List<Map<String, Object>> response = results.stream()
                .map(r -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("documentId", r.getDocument().getId());
                    map.put("name", r.getDocument().getName());
                    map.put("description", r.getDocument().getDescription() != null ? r.getDocument().getDescription() : "");
                    map.put("similarity", r.getSimilarity());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
        summary = "Generate embedding for a document",
        description = "Generate and store vector embedding for document content"
    )
    @PostMapping("/embeddings/{documentId}")
    public ResponseEntity<String> generateEmbedding(@PathVariable Long documentId) {
        try {
            var doc = documentService.findById(documentId);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }
            similarityService.generateEmbedding(doc);
            return ResponseEntity.ok("Embedding generated");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate embedding");
        }
    }
    
    @Operation(
        summary = "Rebuild all embeddings",
        description = "Regenerate vector embeddings for all documents"
    )
    @PostMapping("/embeddings/rebuild")
    public ResponseEntity<String> rebuildEmbeddings() {
        try {
            similarityService.rebuildAllEmbeddings();
            return ResponseEntity.ok("Embeddings rebuilt");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to rebuild embeddings");
        }
    }
}
