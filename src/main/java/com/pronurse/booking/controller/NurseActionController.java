package com.pronurse.booking.controller;

import com.pronurse.booking.dto.NurseDashboardResponse;
import com.pronurse.booking.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/booking")
@Tag(name = "08. Web Nurse Booking Actions", description = "Web portal nurse booking actions: accept, reject, complete bookings, and retrieve operational dashboard feeds.")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class NurseActionController {

    private final BookingService bookingService;

    @PostMapping("/accept")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Accept booking (Web portal)",
        description = "Accepts a booking offer assigned to the nurse from the web dashboard.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Booking identifier payload",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"booking_no\": \"PN-TEST-BOOK-001\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking accepted successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Booking number is required or invalid request", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Void>> acceptBooking(
            @RequestBody Map<String, String> body, Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        String bookingNo = body.get("booking_no");
        if (bookingNo == null) {
            bookingNo = body.get("bookingNo");
        }

        if (bookingNo == null || bookingNo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new com.pronurse.common.payload.ApiResponse<>(false, "Booking number is required."));
        }

        bookingService.processNurseResponse(bookingNo, mobile, true, null);

        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Booking accepted successfully", null));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Reject booking (Web portal)",
        description = "Rejects a booking offer assigned to the nurse from the web dashboard with an optional reason.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Booking rejection payload",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"booking_no\": \"PN-TEST-BOOK-001\",\n  \"reason\": \"Currently attending another patient\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking rejected successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Booking number is required or invalid request", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Void>> rejectBooking(
            @RequestBody Map<String, String> body, Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        String bookingNo = body.get("booking_no");
        if (bookingNo == null) {
            bookingNo = body.get("bookingNo");
        }

        if (bookingNo == null || bookingNo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new com.pronurse.common.payload.ApiResponse<>(false, "Booking number is required."));
        }

        bookingService.processNurseResponse(bookingNo, mobile, false, body.get("reason"));

        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Booking rejected successfully", null));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Get dashboard bookings", description = "Fetches the current booking feed for the nurse dashboard.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard bookings retrieved successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Complete booking service", description = "Marks a booking as completed by providing final remarks.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service completed successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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