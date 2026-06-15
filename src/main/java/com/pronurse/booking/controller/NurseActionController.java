package com.pronurse.booking.controller;

import com.pronurse.booking.dto.NurseDashboardResponse;
import com.pronurse.booking.service.BookingService;
import com.pronurse.common.payload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class NurseActionController {

    private final BookingService bookingService;

    @PostMapping("/accept")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<Void>> acceptBooking(
            @RequestBody Map<String, String> body, Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        bookingService.processNurseResponse(body.get("bookingId"), mobile, true, null);

        return ResponseEntity.ok(new ApiResponse<>(true, "Booking accepted successfully", null));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<Void>> rejectBooking(
            @RequestBody Map<String, String> body, Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        bookingService.processNurseResponse(body.get("bookingId"), mobile, false, body.get("reason"));

        return ResponseEntity.ok(new ApiResponse<>(true, "Booking rejected successfully", null));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<List<com.pronurse.booking.dto.NurseDashboardResponse>>> getDashboardBookings(
            org.springframework.security.core.Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        List<NurseDashboardResponse> dashboardData = bookingService.getNurseDashboardFeed(nurseMobile);

        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(
                true,
                "Nurse operational dashboard pipeline synchronized successfully.",
                dashboardData
        ));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Void>> completeBooking(
            @jakarta.validation.Valid @RequestBody com.pronurse.booking.dto.CompleteBookingRequest request,
            org.springframework.security.core.Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        bookingService.completeBookingService(nurseMobile, request);

        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(
                true,
                "Service order ticket compiled and marked COMPLETED successfully.",
                null
        ));
    }
}