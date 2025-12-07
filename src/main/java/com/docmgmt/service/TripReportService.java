package com.docmgmt.service;

import com.docmgmt.model.TripReport;
import com.docmgmt.repository.TripReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for TripReport entity operations
 */
@Service
public class TripReportService extends AbstractSysObjectService<TripReport, TripReportRepository> {
    
    @Autowired
    public TripReportService(TripReportRepository repository) {
        super(repository);
    }
    
    /**
     * Find trip reports by destination
     * @param destination The destination
     * @return List of trip reports to the destination
     */
    @Transactional(readOnly = true)
    public List<TripReport> findByDestination(String destination) {
        return repository.findByDestination(destination);
    }
    
    /**
     * Find trip reports by destination containing a search term
     * @param destination The destination search term
     * @return List of matching trip reports
     */
    @Transactional(readOnly = true)
    public List<TripReport> findByDestinationContaining(String destination) {
        return repository.findByDestinationContaining(destination);
    }
    
    /**
     * Find trip reports within a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return List of trip reports within the date range
     */
    @Transactional(readOnly = true)
    public List<TripReport> findByTripStartDateBetween(LocalDate startDate, LocalDate endDate) {
        return repository.findByTripStartDateBetween(startDate, endDate);
    }
    
    /**
     * Override findAll to ensure collections are initialized
     * @return List of all trip reports
     */
    @Override
    @Transactional(readOnly = true)
    public List<TripReport> findAll() {
        List<TripReport> reports = super.findAll();
        // Initialize collections
        reports.forEach(report -> {
            if (report.getTags() != null) {
                report.getTags().size();
            }
            if (report.getAttendees() != null) {
                report.getAttendees().size();
            }
            if (report.getContents() != null) {
                report.getContents().size();
            }
            // Touch parent version to initialize it
            if (report.getParentVersion() != null) {
                report.getParentVersion().getName();
            }
        });
        return reports;
    }
    
    /**
     * Override findById to ensure collections are initialized
     * @param id The trip report ID
     * @return The found trip report
     */
    @Override
    @Transactional(readOnly = true)
    public TripReport findById(Long id) {
        TripReport report = super.findById(id);
        // Initialize collections
        if (report.getTags() != null) {
            report.getTags().size();
        }
        if (report.getAttendees() != null) {
            report.getAttendees().size();
        }
        if (report.getContents() != null) {
            report.getContents().size();
        }
        // Touch parent version to initialize it
        if (report.getParentVersion() != null) {
            report.getParentVersion().getName();
        }
        return report;
    }
}
