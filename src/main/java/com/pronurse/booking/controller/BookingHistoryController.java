package com.pronurse.booking.controller;

import com.pronurse.booking.dto.BookingHistoryResponse;
import com.pronurse.booking.dto.BookingHistoryFilterRequest;
import com.pronurse.booking.service.BookingExportService;
import com.pronurse.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "10. Booking History", description = "Booking history and filtering")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingHistoryController {

    private final BookingService bookingService;
    private final BookingExportService exportService;

    /**
     * Patient Mobile App Hook: Pull chronological service request history
     */
    @GetMapping("/patient/list")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get patient booking history list", description = "Fetches the full booking history list for the logged-in patient.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patient booking history retrieved successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<List<BookingHistoryResponse>>> getPatientLog(Authentication authentication) {
        String patientMobile = (String) authentication.getPrincipal();
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(
                true, "Patient history stream fetched successfully.", bookingService.getPatientCompleteHistory(patientMobile)
        ));
    }

    /**
     * Nurse Mobile App Hook: Fetch completed job cards and historical assignments
     */
    @GetMapping("/nurse/list")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Get nurse booking history list", description = "Fetches the full booking history/log list for the logged-in nurse.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nurse booking history retrieved successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<List<BookingHistoryResponse>>> getNurseLog(Authentication authentication) {
        String nurseMobile = (String) authentication.getPrincipal();
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filtered history retrieved successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter request details"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Page<BookingHistoryResponse>>> getFilteredPatientHistory(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String patientMobile = (String) authentication.getPrincipal();
        Page<BookingHistoryResponse> history = bookingService.getFilteredPatientHistory(patientMobile, filter);
        
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filtered history retrieved successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter request details"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Page<BookingHistoryResponse>>> getFilteredNurseHistory(
            @Valid @RequestBody BookingHistoryFilterRequest filter,
            Authentication authentication) {
        
        String nurseMobile = (String) authentication.getPrincipal();
        Page<BookingHistoryResponse> history = bookingService.getFilteredNurseHistory(nurseMobile, filter);
        
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV report exported successfully", content = @Content(mediaType = "text/csv")),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF report exported successfully", content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV report exported successfully", content = @Content(mediaType = "text/csv")),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF report exported successfully", content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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