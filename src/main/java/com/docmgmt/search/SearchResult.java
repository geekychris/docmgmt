package com.docmgmt.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a search result from Lucene
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private Long documentId;
    private float score;
    private String name;
    private String description;
    private String keywords;
    private String tags;
}
