package com.docmgmt.repository;

import com.docmgmt.model.TripReport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for TripReport entities
 */
@Repository
public interface TripReportRepository extends BaseSysObjectRepository<TripReport> {
    
    /**
     * Find trip reports by destination
     * @param destination The destination
     * @return List of trip reports to the destination
     */
    List<TripReport> findByDestination(String destination);
    
    /**
     * Find trip reports by destination containing a search term
     * @param destination The destination search term
     * @return List of matching trip reports
     */
    List<TripReport> findByDestinationContaining(String destination);
    
    /**
     * Find trip reports within a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return List of trip reports within the date range
     */
    List<TripReport> findByTripStartDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find trip reports by author
     * @param author The author name
     * @return List of trip reports by the author
     */
    List<TripReport> findByAuthor(String author);
}
