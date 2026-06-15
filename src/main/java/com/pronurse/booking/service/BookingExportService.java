package com.pronurse.booking.service;

import com.pronurse.booking.dto.BookingHistoryFilterRequest;

public interface BookingExportService {
    
    /**
     * Export patient booking history to CSV
     */
    byte[] exportPatientHistoryToCSV(String patientMobile, BookingHistoryFilterRequest filter);
    
    /**
     * Export nurse booking history to CSV
     */
    byte[] exportNurseHistoryToCSV(String nurseMobile, BookingHistoryFilterRequest filter);
    
    /**
     * Export patient booking history to PDF
     */
    byte[] exportPatientHistoryToPDF(String patientMobile, BookingHistoryFilterRequest filter);
    
    /**
     * Export nurse booking history to PDF
     */
    byte[] exportNurseHistoryToPDF(String nurseMobile, BookingHistoryFilterRequest filter);
}


