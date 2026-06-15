package com.pronurse.booking.controller;

import com.pronurse.booking.dto.CreateBookingRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.service.BookingService;
import com.pronurse.catalog.dto.ServiceCatalogResponse;
import com.pronurse.catalog.service.CatalogService;
import com.pronurse.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PatientBookingController {

    private final CatalogService catalogService;
    private final BookingService bookingService;

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getServices() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched catalog successfully", catalogService.getActiveHierarchyTree()));
    }

    @PostMapping("/booking/create")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> createBooking(
            @Valid @RequestBody CreateBookingRequest request, Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        String bookingNo = bookingService.createMultiItemBooking(mobile, request);

        return ResponseEntity.ok(new ApiResponse<>(true, "Booking registered", Map.of("bookingId", bookingNo, "status", "PENDING")));
    }

    @PostMapping(value = "/booking/create-with-prescription", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> createBookingWithFile(
            @RequestPart("data") @jakarta.validation.Valid CreateBookingRequest request,
            @RequestPart(value = "prescription", required = false) org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        String bookingNo = bookingService.createMultiItemBookingWithPrescription(mobile, request, file);

        return ResponseEntity.ok(new ApiResponse<>(true, "Booking registered successfully", Map.of("bookingId", bookingNo)));
    }

    @GetMapping("/booking/patient/list")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyHistoryLog(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        return ResponseEntity.ok(new ApiResponse<>(true, "History ledger compiled", bookingService.getPatientBookingHistory(mobile)));
    }

}