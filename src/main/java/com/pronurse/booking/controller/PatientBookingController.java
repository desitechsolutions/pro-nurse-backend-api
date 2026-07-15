package com.pronurse.booking.controller;

import com.pronurse.booking.dto.CreateBookingRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.service.BookingService;
import com.pronurse.catalog.dto.ServiceCatalogResponse;
import com.pronurse.catalog.service.CatalogService;
import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import com.pronurse.booking.dto.BookingHistoryResponse;

@RestController
@RequestMapping("/api")
@Tag(
        name = "06. Patient Booking",
        description = "Patient booking APIs for service catalog retrieval, booking creation, and history log access"
)
@RequiredArgsConstructor
public class PatientBookingController {

    private final CatalogService catalogService;
    private final BookingService bookingService;

    @Operation(
            summary = "Get medical services catalog",
            description = "Retrieves the active hierarchical tree structure of all medical procedures, specialties, and baseline service fees."
    )
    @GetMapping("/services")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<List<ServiceCatalogResponse>>> getServices() {
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Fetched catalog successfully", catalogService.getActiveHierarchyTree()));
    }

    @Operation(
            summary = "Create service booking",
            description = "Registers a new multi-item booking request under the active patient session credentials."
    )
    @PostMapping("/booking/create")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Map<String, String>>> createBooking(
            @Valid @RequestBody CreateBookingRequest request, Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        String bookingNo = bookingService.createMultiItemBooking(mobile, request);

        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Booking registered", Map.of("bookingId", bookingNo, "status", "PENDING")));
    }

    @Operation(
            summary = "Create service booking with prescription attachment",
            description = "Registers a new booking request while attaching a physical prescription image document payload."
    )
    @PostMapping(value = "/booking/create-with-prescription", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Map<String, String>>> createBookingWithFile(
            @RequestPart("data") @jakarta.validation.Valid CreateBookingRequest request,
            @RequestPart(value = "prescription", required = false) org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        String bookingNo = bookingService.createMultiItemBookingWithPrescription(mobile, request, file);

        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Booking registered successfully", Map.of("bookingId", bookingNo)));
    }

    @Operation(
            summary = "Get patient booking history list",
            description = "Retrieves the historical list of all past, upcoming, and current bookings initiated by the active patient."
    )
    @GetMapping("/booking/patient/list")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<List<BookingHistoryResponse>>> getMyHistoryLog(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "History ledger compiled", bookingService.getPatientCompleteHistory(mobile)));
    }

    @Operation(
            summary = "Get single booking details",
            description = "Retrieves complete booking details and status workflow tracks matching the unique booking number reference."
    )
    @GetMapping("/booking/{bookingNo}")
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<BookingHistoryResponse>> getBookingDetails(@PathVariable String bookingNo) {
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Booking details retrieved successfully", bookingService.getSingleBookingDetails(bookingNo)));
    }
}