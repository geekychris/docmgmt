package com.docmgmt.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper class that contains search results and metadata about the search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultsWrapper {
    private List<SearchResult> results;
    private long totalHits;
    
    /**
     * Check if there are more results than what was returned
     */
    public boolean hasMoreResults() {
        return totalHits > results.size();
    }
    
    /**
     * Get the number of results returned
     */
    public int getResultCount() {
        return results.size();
    }
}
