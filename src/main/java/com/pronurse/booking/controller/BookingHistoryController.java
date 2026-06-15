package com.pronurse.booking.controller;

import com.pronurse.booking.dto.BookingHistoryResponse;
import com.pronurse.booking.dto.BookingHistoryFilterRequest;
import com.pronurse.booking.service.BookingExportService;
import com.pronurse.booking.service.BookingService;
import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "Booking History", description = "Booking history and filtering")
public class BookingHistoryController {

    private final BookingService bookingService;
    private final BookingExportService exportService;

    /**
     * Patient Mobile App Hook: Pull chronological service request history
     */
    @GetMapping("/patient/list")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getPatientLog(Authentication authentication) {
        String patientMobile = (String) authentication.getPrincipal();
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Patient history stream fetched successfully.", bookingService.getPatientCompleteHistory(patientMobile)
        ));
    }

    /**
     * Nurse Mobile App Hook: Fetch completed job cards and historical assignments
     */
    @GetMapping("/nurse/list")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getNurseLog(Authentication authentication) {
        String nurseMobile = (String) authentication.getPrincipal();
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Nurse fulfillment ledger tracking synced.", bookingService.getNurseCompleteHistory(nurseMobile)
        ));
    }

    /**
     * Patient filtered history with pagination
     */
    @PostMapping("/patient/filter")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get filtered patient booking history", 
               description = "Advanced filtering with date range, status, amount, search, and pagination")
    public ResponseEntity<ApiResponse<Page<BookingHistoryResponse>>> getFilteredPatientHistory(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String patientMobile = (String) authentication.getPrincipal();
        Page<BookingHistoryResponse> history = bookingService.getFilteredPatientHistory(patientMobile, filter);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Filtered patient history retrieved successfully", history
        ));
    }

    /**
     * Nurse filtered history with pagination
     */
    @PostMapping("/nurse/filter")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Get filtered nurse booking history", 
               description = "Advanced filtering with date range, status, amount, search, and pagination")
    public ResponseEntity<ApiResponse<Page<BookingHistoryResponse>>> getFilteredNurseHistory(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String nurseMobile = (String) authentication.getPrincipal();
        Page<BookingHistoryResponse> history = bookingService.getFilteredNurseHistory(nurseMobile, filter);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Filtered nurse history retrieved successfully", history
        ));
    }

    /**
     * Export patient history to CSV
     */
    @PostMapping("/patient/export/csv")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Export patient history to CSV", 
               description = "Export filtered booking history as CSV file")
    public ResponseEntity<byte[]> exportPatientHistoryCSV(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String patientMobile = (String) authentication.getPrincipal();
        byte[] csvData = exportService.exportPatientHistoryToCSV(patientMobile, filter);
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=patient-booking-history.csv")
                .body(csvData);
    }

    /**
     * Export patient history to PDF
     */
    @PostMapping("/patient/export/pdf")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Export patient history to PDF", 
               description = "Export filtered booking history as PDF file")
    public ResponseEntity<byte[]> exportPatientHistoryPDF(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String patientMobile = (String) authentication.getPrincipal();
        byte[] pdfData = exportService.exportPatientHistoryToPDF(patientMobile, filter);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=patient-booking-history.pdf")
                .body(pdfData);
    }

    /**
     * Export nurse history to CSV
     */
    @PostMapping("/nurse/export/csv")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Export nurse history to CSV", 
               description = "Export filtered booking history as CSV file")
    public ResponseEntity<byte[]> exportNurseHistoryCSV(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String nurseMobile = (String) authentication.getPrincipal();
        byte[] csvData = exportService.exportNurseHistoryToCSV(nurseMobile, filter);
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=nurse-booking-history.csv")
                .body(csvData);
    }

    /**
     * Export nurse history to PDF
     */
    @PostMapping("/nurse/export/pdf")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Export nurse history to PDF", 
               description = "Export filtered booking history as PDF file")
    public ResponseEntity<byte[]> exportNurseHistoryPDF(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String nurseMobile = (String) authentication.getPrincipal();
        byte[] pdfData = exportService.exportNurseHistoryToPDF(nurseMobile, filter);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=nurse-booking-history.pdf")
                .body(pdfData);
    }
}