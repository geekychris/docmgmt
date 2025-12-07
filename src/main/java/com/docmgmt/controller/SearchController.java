package com.docmgmt.controller;

import com.docmgmt.search.LuceneIndexService;
import com.docmgmt.search.SearchResult;
import com.docmgmt.service.DocumentService;
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
public class SearchController {
    
    @Autowired
    private LuceneIndexService searchService;
    
    @Autowired
    private DocumentService documentService;
    
    @GetMapping
    public ResponseEntity<List<SearchResult>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<SearchResult> results = searchService.search(q, limit);
            return ResponseEntity.ok(results);
        } catch (IOException | ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/fields")
    public ResponseEntity<List<SearchResult>> searchFields(
            @RequestBody Map<String, String> fieldQueries,
            @RequestParam(defaultValue = "50") int limit,
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
    
    @PostMapping("/rebuild")
    public ResponseEntity<String> rebuildIndex() {
        try {
            searchService.rebuildIndex(documentService.findAll());
            return ResponseEntity.ok("Index rebuilt successfully");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to rebuild index");
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            return ResponseEntity.ok(searchService.getIndexStats());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
