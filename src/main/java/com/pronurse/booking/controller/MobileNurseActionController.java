package com.pronurse.booking.controller;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.entity.BookingItem;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.booking.service.BookingService;
import com.pronurse.patient.entity.PatientProfile;
import com.pronurse.patient.repository.PatientProfileRepository;
import com.pronurse.nurse.service.NurseProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nurse")
@Tag(name = "07. Mobile Nurse Actions (Flutter)", description = "Dedicated mobile app nurse actions: accept offers (30s window), reject with reason, start service (IN_PROGRESS), complete, and live GPS streaming.")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Slf4j
public class MobileNurseActionController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookingAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final NurseProfileService nurseProfileService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/booking/accept")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Accept booking offer (Mobile)",
        description = "Accepts a pending booking assignment offer sent to the nurse within the 30-second window.",
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
        @ApiResponse(responseCode = "200", description = "Booking offer processed successfully", content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or booking already accepted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> acceptBooking(
            @RequestBody Map<String, Object> body, Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        log.info("Mobile accept request received from nurse mobile: {}, payload: {}", nurseMobile, body);

        Object bookingNoObj = body.get("booking_no");
        if (bookingNoObj == null) {
            bookingNoObj = body.get("bookingNo");
        }

        if (bookingNoObj == null || bookingNoObj.toString().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", "Booking number is required."
            ));
        }

        String bookingNo = bookingNoObj.toString().trim();
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Booking not found."));

        User nurseUser = userRepository.findByMobile(nurseMobile)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Nurse user profile not found."));

        // If already assigned to someone else
        if (booking.getAssignedNurseUser() != null && !booking.getAssignedNurseUser().getId().equals(nurseUser.getId())) {
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", "Booking is already accepted by another nurse."
            ));
        }

        if ("ACCEPTED".equals(booking.getBookingStatus())) {
            if (booking.getAssignedNurseUser() != null && booking.getAssignedNurseUser().getId().equals(nurseUser.getId())) {
                // Already accepted by me, return success
                return ResponseEntity.ok(Map.of(
                        "status", true,
                        "message", "Booking accepted successfully.",
                        "data", Map.of(
                                "booking_no", booking.getBookingNo(),
                                "status", "Accepted",
                                "accepted_at", booking.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        )
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "status", false,
                        "message", "Booking is already accepted by another nurse."
                ));
            }
        }

        // Check active assignment
        BookingAssignment assignment = assignmentRepository.findByBookingIdOrderByNotifiedAtAsc(booking.getId()).stream()
                .filter(ba -> ba.getNurseUser().getId().equals(nurseUser.getId()))
                .findFirst()
                .orElse(null);

        if (assignment == null) {
            List<BookingAssignment> existingAssignments = assignmentRepository.findByBookingIdOrderByNotifiedAtAsc(booking.getId());
            String msg = existingAssignments.isEmpty()
                    ? "No active booking offer found. No dispatch offer was generated for this booking (verify nurse online status and coordinates)."
                    : "No active booking offer found for this nurse (offer dispatched to another practitioner).";
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", msg
            ));
        }

        if (!"RINGING".equals(assignment.getStatus())) {
            if ("TIMEOUT".equalsIgnoreCase(assignment.getStatus())) {
                return ResponseEntity.ok(Map.of(
                        "status", false,
                        "message", "Booking offer expired (30-second response window passed)."
                ));
            } else if ("REJECTED".equalsIgnoreCase(assignment.getStatus())) {
                return ResponseEntity.ok(Map.of(
                        "status", false,
                        "message", "Booking offer was already rejected."
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", "Booking is already accepted by another nurse."
            ));
        }

        // Process acceptance
        bookingService.processNurseResponse(booking.getBookingNo(), nurseMobile, true, null);

        // Fetch refreshed booking
        Booking updatedBooking = bookingRepository.findById(booking.getId()).get();

        Map<String, Object> data = new HashMap<>();
        data.put("booking_no", updatedBooking.getBookingNo());
        data.put("status", "Accepted");
        data.put("accepted_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Booking accepted successfully.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/booking/reject")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Reject booking offer (Mobile)",
        description = "Rejects a booking offer with a predefined reason code (1=Not Available, 2=Already Busy, 3=Too Far Away, 4=Emergency, 5=Personal Reason) and optional remarks.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Booking rejection payload with reason ID",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"booking_no\": \"PN-TEST-BOOK-001\",\n  \"reason_id\": 1,\n  \"remarks\": \"Currently at hospital\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking offer rejected successfully", content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or offer already processed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> rejectBooking(
            @RequestBody Map<String, Object> body, Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        log.info("Mobile reject request received from nurse mobile: {}, payload: {}", nurseMobile, body);

        Object bookingNoObj = body.get("booking_no");
        if (bookingNoObj == null) {
            bookingNoObj = body.get("bookingNo");
        }

        if (bookingNoObj == null || bookingNoObj.toString().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", "Booking number is required."
            ));
        }

        String bookingNo = bookingNoObj.toString().trim();
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Booking not found."));

        Object reasonObj = body.get("reason_id") != null ? body.get("reason_id") : body.get("reasonId");
        int reasonId = 1;
        if (reasonObj != null) {
            try {
                reasonId = Integer.parseInt(reasonObj.toString().trim());
            } catch (Exception ignored) {}
        }
        String remarks = (String) body.get("remarks");

        String reasonText = getPredefinedReason(reasonId);
        if (remarks != null && !remarks.trim().isEmpty()) {
            reasonText += " - " + remarks;
        }

        User nurseUser = userRepository.findByMobile(nurseMobile)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Nurse user profile not found."));

        BookingAssignment assignment = assignmentRepository.findByBookingIdOrderByNotifiedAtAsc(booking.getId()).stream()
                .filter(ba -> ba.getNurseUser().getId().equals(nurseUser.getId()))
                .findFirst()
                .orElse(null);

        if (assignment == null || !"RINGING".equals(assignment.getStatus())) {
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", "Active offer not found or already processed."
            ));
        }

        // Process rejection
        bookingService.processNurseResponse(booking.getBookingNo(), nurseMobile, false, reasonText);

        Map<String, Object> data = new HashMap<>();
        data.put("booking_no", booking.getBookingNo());
        data.put("booking_status", "REJECTED");
        data.put("rejected_at", LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Booking rejected successfully.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Get nurse bookings list", description = "Fetches paginated list of bookings mapped to the nurse's dashboard.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of bookings retrieved successfully", content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getBookings(
            @Parameter(description = "Filter by booking status") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (1-indexed)") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") Integer limit,
            Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        log.info("Fetch bookings request for nurse: {}, status filter: {}, page: {}, limit: {}", nurseMobile, status, page, limit);

        // Fetch using active dashboard feeds
        List<BookingAssignment> activeFeed = assignmentRepository.findActiveNurseDashboardFeeds(nurseMobile);

        List<Map<String, Object>> bookingList = new ArrayList<>();
        for (BookingAssignment ba : activeFeed) {
            Booking b = ba.getBooking();
            
            // Map status
            String mappedStatus = b.getBookingStatus();
            if ("PENDING".equals(mappedStatus)) {
                mappedStatus = "ASSIGNED";
            }

            // Filter status if requested
            if (status != null && !status.equalsIgnoreCase(mappedStatus)) {
                continue;
            }

            String serviceName = b.getSelectedItems().stream()
                    .map(BookingItem::getItemName)
                    .collect(Collectors.joining(", "));
            if (serviceName.isEmpty()) {
                serviceName = "Home Nursing Service";
            }

            double totalAmount = b.getSelectedItems().stream()
                    .map(BookingItem::getPriceCharged)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .doubleValue();

            Map<String, Object> item = new HashMap<>();
            item.put("booking_id", b.getId());
            item.put("booking_no", b.getBookingNo());
            item.put("patient_name", b.getPatientUser().getName());
            item.put("patient_mobile", b.getPatientUser().getMobile());
            item.put("service_name", serviceName);
            item.put("booking_date", b.getBookingDate().toString());
            item.put("booking_time", b.getBookingTime());
            item.put("address", b.getRawAddress());
            item.put("amount", totalAmount > 0 ? totalAmount : 1000.0);
            item.put("booking_status", mappedStatus);

            bookingList.add(item);
        }

        // Apply pagination
        int total = bookingList.size();
        int pageZeroBased = Math.max(0, page - 1);
        int start = Math.min(pageZeroBased * limit, total);
        int end = Math.min(start + limit, total);
        List<Map<String, Object>> paginated = bookingList.subList(start, end);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Bookings fetched successfully.");
        response.put("current_page", page);
        response.put("total_records", total);
        response.put("data", paginated);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{booking_id}")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Get booking details", description = "Fetches detailed information for a specific booking assignment.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking details retrieved successfully", content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getBookingDetails(
            @Parameter(description = "The database ID of the booking") @PathVariable("booking_id") Long bookingId, Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        log.info("Fetch booking details request for nurse: {}, bookingId: {}", nurseMobile, bookingId);

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Booking not found."));

        PatientProfile patientProfile = patientProfileRepository.findByUserMobile(b.getPatientUser().getMobile())
                .orElse(null);

        int age = 45;
        String gender = "Male";
        if (patientProfile != null) {
            gender = patientProfile.getGender() != null ? patientProfile.getGender().name() : "Male";
            if (patientProfile.getDob() != null) {
                try {
                    java.time.LocalDate birthDate = patientProfile.getDob();
                    age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("name", b.getPatientUser().getName());
        patientMap.put("mobile", b.getPatientUser().getMobile());
        patientMap.put("age", age);
        patientMap.put("gender", gender);

        String serviceName = b.getSelectedItems().stream()
                .map(BookingItem::getItemName)
                .collect(Collectors.joining(", "));
        if (serviceName.isEmpty()) {
            serviceName = "Home Nursing Service";
        }

        double totalAmount = b.getSelectedItems().stream()
                .map(BookingItem::getPriceCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
        if (totalAmount == 0) {
            totalAmount = 1000.0;
        }

        Map<String, Object> serviceMap = new HashMap<>();
        serviceMap.put("name", serviceName);
        serviceMap.put("duration", "2 Hours");
        serviceMap.put("price", totalAmount);

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("address", b.getRawAddress());
        addressMap.put("latitude", b.getLatitude() != null ? b.getLatitude() : 28.6289);
        addressMap.put("longitude", b.getLongitude() != null ? b.getLongitude() : 77.3649);

        // Map status
        String mappedStatus = b.getBookingStatus();
        if ("PENDING".equals(mappedStatus)) {
            mappedStatus = "ASSIGNED";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("booking_id", b.getId());
        data.put("booking_no", b.getBookingNo());
        data.put("patient", patientMap);
        data.put("service", serviceMap);
        data.put("address", addressMap);
        data.put("booking_status", mappedStatus);
        data.put("payment_status", b.getPaymentStatus() != null ? b.getPaymentStatus() : "PENDING");

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/booking/complete")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Complete booking (Mobile)",
        description = "Marks an active booking as completed by providing final remarks.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Completion remarks payload",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"booking_no\": \"PN-TEST-BOOK-001\",\n  \"remarks\": \"Patient vitals checked and medication administered successfully.\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking completed successfully", content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or booking already completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> completeBooking(
            @RequestBody Map<String, Object> body, Authentication authentication) {

        String nurseMobile = (String) authentication.getPrincipal();
        log.info("Mobile complete request received from nurse mobile: {}, payload: {}", nurseMobile, body);

        Object bookingNoObj = body.get("booking_no");
        if (bookingNoObj == null) {
            bookingNoObj = body.get("bookingNo");
        }

        if (bookingNoObj == null || bookingNoObj.toString().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "message", "Booking number is required."
            ));
        }

        String bookingNo = bookingNoObj.toString().trim();
        String remarks = (String) body.get("remarks");
        if (remarks == null) {
            remarks = "";
        }

        com.pronurse.booking.dto.CompleteBookingRequest request = new com.pronurse.booking.dto.CompleteBookingRequest();
        request.setBookingId(bookingNo);
        request.setRemarks(remarks);

        bookingService.completeBookingService(nurseMobile, request);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Booking completed successfully.");
        response.put("data", Map.of(
                "booking_no", bookingNo,
                "booking_status", "COMPLETED",
                "completed_at", LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toString()
        ));

        return ResponseEntity.ok(response);
    }

    private String getPredefinedReason(int reasonId) {
        return switch (reasonId) {
            case 1 -> "Not Available";
            case 2 -> "Already Busy";
            case 3 -> "Too Far Away";
            case 4 -> "Emergency";
            case 5 -> "Personal Reason";
            default -> "Other";
        };
    }


    @PostMapping("/booking/start-service")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Start booking service (Mobile)",
        description = "Transitions an accepted booking to IN_PROGRESS. Requires the assigned nurse credentials.",
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
            @ApiResponse(responseCode = "200", description = "Booking marked as IN_PROGRESS successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Booking number missing or booking not in ACCEPTED state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> startService(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        String nurseMobile = (String) authentication.getPrincipal();
        Object bookingNoObj = body.get("booking_no") != null ? body.get("booking_no") : body.get("bookingNo");
        if (bookingNoObj == null || bookingNoObj.toString().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("status", false, "message", "Booking number is required."));
        }
        String bookingNo = bookingNoObj.toString().trim();
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Booking not found."));
        User nurseUser = userRepository.findByMobile(nurseMobile)
                .orElseThrow(() -> new com.pronurse.common.exception.ApplicationException("Nurse profile not found."));
        if (booking.getAssignedNurseUser() == null || !booking.getAssignedNurseUser().getId().equals(nurseUser.getId())) {
            return ResponseEntity.ok(Map.of("status", false, "message", "You are not assigned to this booking."));
        }
        if (!"ACCEPTED".equalsIgnoreCase(booking.getBookingStatus())) {
            return ResponseEntity.ok(Map.of("status", false, "message", "Booking must be in ACCEPTED state to start service."));
        }
        booking.setBookingStatus("IN_PROGRESS");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Service started successfully. Booking is now IN_PROGRESS.",
                "data", Map.of(
                        "booking_no", booking.getBookingNo(),
                        "booking_status", "IN_PROGRESS",
                        "started_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
        ));
    }

    @PostMapping("/duty-status")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Toggle nurse on-duty status (Mobile shortcut)",
        description = "Updates whether nurse is available on duty to receive new dispatch offers.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Duty status boolean payload",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"isOnDuty\": true\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Duty status updated successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "isOnDuty boolean parameter missing"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> toggleDutyStatus(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        String nurseMobile = (String) authentication.getPrincipal();
        Object onDutyObj = body.get("isOnDuty") != null ? body.get("isOnDuty") : body.get("is_on_duty");
        if (onDutyObj == null) {
            return ResponseEntity.ok(Map.of("status", false, "message", "isOnDuty boolean is required."));
        }
        boolean isOnDuty = Boolean.parseBoolean(onDutyObj.toString().trim());
        nurseProfileService.updateDutyStatus(nurseMobile, isOnDuty);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Duty status updated to: " + (isOnDuty ? "ON DUTY" : "OFF DUTY"),
                "is_on_duty", isOnDuty
        ));
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('NURSE')")
    @Operation(
        summary = "Update live location (Mobile shortcut)",
        description = "Updates current nurse GPS coordinates in DB and relays live ping to patient tracking WebSocket topic if booking_no is passed.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "GPS coordinates and optional active booking number payload",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"latitude\": 28.6289,\n  \"longitude\": 77.3649,\n  \"booking_no\": \"PN-TEST-BOOK-001\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location updated and relayed successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Latitude or longitude coordinates missing"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> updateLocation(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        String nurseMobile = (String) authentication.getPrincipal();
        Object latObj = body.get("latitude") != null ? body.get("latitude") : body.get("lat");
        Object lngObj = body.get("longitude") != null ? body.get("longitude") : body.get("lng");

        if (latObj == null || lngObj == null) {
            return ResponseEntity.ok(Map.of("status", false, "message", "latitude and longitude are required."));
        }
        Double latitude = Double.parseDouble(latObj.toString().trim());
        Double longitude = Double.parseDouble(lngObj.toString().trim());

        nurseProfileService.updateLiveCoordinates(nurseMobile, latObj.toString().trim(), lngObj.toString().trim());

        // Optional: relay live ping to active patient booking tracking channel
        Object bookingNoObj = body.get("booking_no") != null ? body.get("booking_no") : body.get("bookingNo");
        if (bookingNoObj != null && !bookingNoObj.toString().trim().isEmpty()) {
            String bookingNo = bookingNoObj.toString().trim();
            Map<String, Object> trackingPayload = Map.of(
                    "bookingNo", bookingNo,
                    "nurseMobile", nurseMobile,
                    "latitude", latitude,
                    "longitude", longitude,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            messagingTemplate.convertAndSend("/topic/tracking/" + bookingNo, (Object) trackingPayload);
        }

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Location updated successfully.",
                "data", Map.of("latitude", latitude, "longitude", longitude)
        ));
    }
}
