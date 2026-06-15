package com.pronurse.booking.controller;

import com.pronurse.booking.dto.BookingHistoryResponse;
import com.pronurse.booking.service.BookingService;
import com.pronurse.common.payload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class BookingHistoryController {

    private final BookingService bookingService;

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
     * Unified Tracking: Pull deep real-time status details for a single target ticket
     */
    @GetMapping("/details/{bookingNo}")
    @PreAuthorize("hasAnyRole('PATIENT', 'NURSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingHistoryResponse>> getIndividualTicketDetails(@PathVariable String bookingNo) {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Detailed status telemetry payload mapped.", bookingService.getSingleBookingDetails(bookingNo)
        ));
    }
}