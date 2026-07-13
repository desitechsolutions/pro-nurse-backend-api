package com.pronurse.nurse.controller;

import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.dto.LiveLocationPayload;
import com.pronurse.nurse.dto.LocationUpdateRequest;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.nurse.service.NurseProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nurse/location")
@Tag(
        name = "05. Nurse Tracking",
        description = "Nurse tracking APIs"
)
@RequiredArgsConstructor
public class NurseTrackingController {

    private static final Logger log = LoggerFactory.getLogger(NurseTrackingController.class);

    private final NurseProfileService nurseProfileService;
    private final NurseProfileRepository nurseProfileRepository; // Added for quick DB reads
    private final SimpMessagingTemplate messagingTemplate;       // Added for WebSocket bouncing

    /**
     * 1. THE REST SOURCE OF TRUTH (Your Existing Code)
     * Flutter App Background Service Hook: Streams live GPS coordinate packets
     * every 30–60 seconds to update the master database.
     */
    @PostMapping("/update")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<Void>> syncLiveTrackingCoordinates(
            @Valid @RequestBody LocationUpdateRequest request,
            Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        nurseProfileService.updateLiveCoordinates(nurseMobile, request.getLatitude(), request.getLongitude());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "GPS location packet coordinates synchronized cleanly.",
                null
        ));
    }

    /**
     * 2. THE WEBSOCKET RELAY (Zero Database Load)
     * Nurse App streams JSON here every 2 seconds via STOMP to: /app/track/location
     */
    @MessageMapping("/track/location")
    public void streamNurseLocationToPatient(@Payload LiveLocationPayload locationUpdate) {
        // Instantly bounce the GPS coordinates to the patient's open map
        String destination = "/topic/tracking/" + locationUpdate.getBookingNo();
        messagingTemplate.convertAndSend(destination, locationUpdate);
    }

    /**
     * 3. THE PATIENT INITIAL LOAD (REST)
     * Patient calls this once when they open the map, to drop the initial pin
     * before the next 2-second WebSocket ping arrives.
     */
    @GetMapping("/last-known/{nurseMobile}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<LiveLocationPayload>> getLastKnownLocation(@PathVariable String nurseMobile) {

        NurseProfile profile = nurseProfileRepository.findByUserMobile(nurseMobile)
                .orElseThrow(() -> new ApplicationException("Practitioner tracking context unavailable."));

        LiveLocationPayload lastLocation = LiveLocationPayload.builder()
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .build();

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Retrieved last known structural coordinates.",
                lastLocation
        ));
    }
}