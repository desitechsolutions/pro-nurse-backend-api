package com.pronurse.booking.controller;

import com.pronurse.booking.dto.EmergencySOSRequest;
import com.pronurse.booking.service.EmergencySOSService;
import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "11. Emergency SOS", description = "Emergency booking and SOS alerts")
@SecurityRequirement(name = "bearerAuth")
public class EmergencySOSController {

    private final EmergencySOSService emergencySOSService;

    @PostMapping("/sos")
    @Operation(summary = "Create emergency SOS booking", 
               description = "Creates an emergency booking with priority dispatch to 5 nearest nurses")
    public ResponseEntity<ApiResponse<String>> createEmergencySOS(
            @Valid @RequestBody EmergencySOSRequest request,
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        log.info("Emergency SOS request received from patient: {}", patientMobile);
        
        String bookingNo = emergencySOSService.createEmergencyBooking(patientMobile, request);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Emergency SOS booking created successfully. Help is on the way!")
                .data(bookingNo)
                .build());
    }
}


