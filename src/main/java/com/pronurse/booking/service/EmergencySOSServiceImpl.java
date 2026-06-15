package com.pronurse.booking.service;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.dto.EmergencySOSRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.entity.BookingItem;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.repository.NurseWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencySOSServiceImpl implements EmergencySOSService {

    private final BookingRepository bookingRepository;
    private final BookingAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final MedicalServiceRepository serviceRepository;
    private final NurseWalletRepository walletRepository;
    private final DispatchAlertService alertService;

    @Override
    @Transactional
    public String createEmergencyBooking(String patientMobile, EmergencySOSRequest request) {
        User patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        MedicalService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ApplicationException("Service not found"));

        String bookingNo = "EMERGENCY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LocalDateTime startTime = request.getPreferredStartTime() != null 
                ? request.getPreferredStartTime() 
                : LocalDateTime.now();

        Booking booking = Booking.builder()
                .bookingNo(bookingNo)
                .patientUser(patient)
                .bookingStatus("EMERGENCY_PENDING")
                .paymentStatus("PENDING")
                .bookingDate(startTime.toLocalDate())
                .bookingTime(startTime.toLocalTime().toString())
                .remarks(request.getNotes())
                .latitude(request.getLatitude().doubleValue())
                .longitude(request.getLongitude().doubleValue())
                .rawAddress(request.getAddress())
                .isEmergency(true)
                .emergencyDescription(request.getEmergencyDescription())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactMobile(request.getEmergencyContactMobile())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BookingItem item = BookingItem.builder()
                .booking(booking)
                .service(service)
                .itemName(service.getName())
                .priceCharged(service.getBasePrice())
                .build();

        booking.setSelectedItems(List.of(item));
        bookingRepository.save(booking);

        log.info("Emergency SOS booking created: {}", bookingNo);

        // Trigger emergency dispatch to 5 nearest nurses
        triggerEmergencyDispatch(booking.getId());

        return bookingNo;
    }

    @Override
    @Transactional
    public void triggerEmergencyDispatch(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApplicationException("Booking not found"));

        // Find 5 nearest available nurses
        List<Long> nearestNurseIds = bookingRepository.findNearestNurseIds(
                booking.getLatitude(), 
                booking.getLongitude(), 
                5
        );

        if (nearestNurseIds.isEmpty()) {
            booking.setBookingStatus("NO_NURSE_AVAILABLE");
            bookingRepository.save(booking);
            log.warn("No nurses available for emergency booking: {}", booking.getBookingNo());
            return;
        }

        List<BookingAssignment> assignments = new ArrayList<>();
        
        for (Long nurseId : nearestNurseIds) {
            User nurse = userRepository.findById(nurseId)
                    .orElseThrow(() -> new ApplicationException("Nurse not found"));

            // Skip suspended nurses
            NurseWallet wallet = walletRepository.findByNurseUserMobile(nurse.getMobile()).orElse(null);
            if (wallet != null && wallet.isSuspended()) {
                log.info("Skipping suspended nurse for emergency: {}", nurse.getMobile());
                continue;
            }

            BookingAssignment assignment = BookingAssignment.builder()
                    .booking(booking)
                    .nurseUser(nurse)
                    .status("EMERGENCY_RINGING")
                    .notifiedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusSeconds(20)) // 20 second window for emergency
                    .build();

            assignments.add(assignment);
            assignmentRepository.save(assignment);

            log.info("Emergency dispatch sent to nurse: {} for booking: {}", 
                    nurse.getMobile(), booking.getBookingNo());

            // Send emergency alert via WebSocket
            alertService.broadcastEmergencyAlert(nurse.getMobile(), assignment);
        }

        log.info("Emergency SOS dispatched to {} nurses for booking: {}", 
                assignments.size(), booking.getBookingNo());
    }
}


