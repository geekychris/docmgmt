package com.docmgmt.controller;

import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.search.SearchResult;
import com.docmgmt.service.DocumentService;
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
    
    @Operation(
        summary = "Simple text search",
        description = "Search across all document fields (name, description, keywords, tags, content) and indexable content using Lucene query syntax. Supports wildcards (*), phrase queries (\"exact phrase\"), and boolean operators (AND, OR, NOT)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query syntax")
    })
    @GetMapping
    public ResponseEntity<List<SearchResult>> search(
            @Parameter(description = "Search query using Lucene syntax", required = true, 
                      example = "spring framework") 
            @RequestParam String q,
            @Parameter(description = "Maximum number of results to return", example = "50") 
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<SearchResult> results = searchService.search(q, limit);
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
    public ResponseEntity<List<SearchResult>> searchFields(
            @RequestBody Map<String, String> fieldQueries,
            @Parameter(description = "Maximum number of results", example = "50") 
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Boolean operator: AND or OR", example = "AND") 
            @RequestParam(defaultValue = "AND") String operator) {
        try {
            BooleanClause.Occur occur = "OR".equalsIgnoreCase(operator) ? 
                BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST;
            
            List<SearchResult> results = searchService.searchFieldsWithOperator(
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
}
