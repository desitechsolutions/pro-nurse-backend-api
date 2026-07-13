package com.pronurse.booking.service;

import com.pronurse.booking.dto.BookingHistoryResponse;
import com.pronurse.booking.dto.BookingHistoryFilterRequest;
import com.pronurse.booking.dto.BookWithFavoriteRequest;
import com.pronurse.booking.dto.CompleteBookingRequest;
import com.pronurse.booking.dto.CreateBookingRequest;
import com.pronurse.booking.dto.NurseDashboardResponse;
import com.pronurse.booking.entity.*;
import com.pronurse.booking.repository.*;
import com.pronurse.booking.specification.BookingSpecification;
import com.pronurse.favorites.repository.FavoriteNurseRepository;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.util.LocalFileStorageServiceUtil;
import com.pronurse.nurse.repository.NurseProfileRepository;

import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.repository.NurseWalletRepository;
import com.pronurse.wallet.service.WalletService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    @Value("${razorpay.key.id}")
    private String razorpayKey;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    private final BookingRepository bookingRepository;
    private final BookingAssignmentRepository assignmentRepository;
    private final MedicalServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final NurseProfileRepository nurseProfileRepository;
    private final LocalFileStorageServiceUtil fileStorageUtil;
    private final WalletService walletService;
    private final DispatchAlertService alertService;
    private final NurseWalletRepository walletRepository;
    private final FavoriteNurseRepository favoriteRepository;

    @Override
    @Transactional
    public String createMultiItemBooking(String patientMobile, CreateBookingRequest request) {
        var patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("User match error."));

        List<MedicalService> targetServices = serviceRepository.findAllById(request.getSelectedServiceIds());

        String ticketNo = "BOOK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = Booking.builder()
                .bookingNo(ticketNo)
                .patientUser(patient)
                .bookingStatus("PENDING")
                .paymentStatus("PENDING")
                .bookingDate(LocalDate.parse(request.getBookingDate()))
                .bookingTime(request.getBookingTime())
                .remarks(request.getRemarks())
                .latitude(Double.parseDouble(request.getLatitude()))
                .longitude(Double.parseDouble(request.getLongitude()))
                .rawAddress(request.getAddress())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<BookingItem> items = targetServices.stream().map(service -> BookingItem.builder()
                .booking(booking)
                .service(service)
                .itemName(service.getName())
                .priceCharged(service.getBasePrice())
                .build()
        ).collect(Collectors.toList());

        booking.setSelectedItems(items);
        bookingRepository.save(booking);

        // Instantly trigger initial dispatch engine matching
        triggerChainedDispatch(booking);

        return ticketNo;
    }

    @Override
    @Transactional
    public String createMultiItemBookingWithPrescription(String patientMobile, CreateBookingRequest request, MultipartFile prescriptionFile) {
        // 1. Invoke baseline initialization to process the core schema tables
        String bookingNo = createMultiItemBooking(patientMobile, request);

        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Error reloading initialized ticket tracking reference."));

        // 2. Stream multi-part asset files directly onto secure local storage disk structures
        if (prescriptionFile != null && !prescriptionFile.isEmpty()) {
            try {
                String savedFileName = fileStorageUtil.storeFile(prescriptionFile, "prescriptions", booking.getId());
                booking.setPrescriptionFilePath(savedFileName);
                bookingRepository.save(booking);
                log.info("Medical prescription document linked successfully to booking reference: [{}]", bookingNo);
            } catch (Exception e) {
                log.error("Asset tracking exception while storing patient prescription attachment logs.", e);
                throw new ApplicationException("Prescription medical file upload mapping failure runtime exception.");
            }
        }
        return bookingNo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getPatientBookingHistory(String patientMobile) {
        return bookingRepository.findByPatientUserMobileOrderByCreatedAtDesc(patientMobile);
    }

    @Override
    @Transactional
    public Map<String, Object> createRazorpayOrder(String bookingNo) {
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Booking not found"));

        BigDecimal total = booking.getSelectedItems().stream()
                .map(BookingItem::getPriceCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKey, razorpaySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", total.multiply(new BigDecimal(100))); // Razorpay expects amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", booking.getBookingNo());

            Order order = razorpay.orders.create(orderRequest);

            // Save the real Order ID from Razorpay
            booking.setRazorpayOrderId(order.get("id"));
            bookingRepository.save(booking);

            return Map.of(
                    "orderId", order.get("id"),
                    "amount", total,
                    "key", razorpayKey
            );
        } catch (Exception e) {
            throw new ApplicationException("Payment gateway connection failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void verifyRazorpayPayment(String bookingNo, String paymentId, String orderId, String signature) {
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Booking lookup failed"));

        try {
            // The payload for verification is always "orderId|paymentId"
            String payload = orderId + "|" + paymentId;

            // Use the direct cryptographic verification method you found
            boolean isValid = Utils.verifySignature(payload, signature, razorpaySecret);

            if (isValid) {
                booking.setPaymentStatus("PAID");
                booking.setBookingStatus("ACCEPTED");
                booking.setRazorpayPaymentId(paymentId);
                bookingRepository.save(booking);
                log.info("Payment signature verified successfully for booking: {}", bookingNo);
            } else {
                log.error("Security Alert: Cryptographic mismatch for booking: {}", bookingNo);
                throw new ApplicationException("Invalid payment signature detected!");
            }
        } catch (Exception e) {
            log.error("Payment verification process error", e);
            throw new ApplicationException("Payment verification failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void triggerChainedDispatch(Booking booking) {
        Optional<Long> nextNurseId = bookingRepository.findNextClosestNurseId(
                booking.getLatitude(), booking.getLongitude(), booking.getId());

        if (nextNurseId.isEmpty()) {
            booking.setBookingStatus("NO_NURSE_AVAILABLE");
            bookingRepository.save(booking);
            log.warn("System out of nearby service options for ticket: {}", booking.getBookingNo());
            return;
        }

        var candidateUser = userRepository.findById(nextNurseId.get())
                .orElseThrow(() -> new ApplicationException("System user structural entity missing"));

        NurseWallet wallet = walletRepository.findByNurseUserMobile(candidateUser.getMobile()).orElse(null);
        if (wallet != null && wallet.isSuspended()) {
            log.info("Skipping suspended nurse: {}", candidateUser.getMobile());
            return; // Don't dispatch to them
        }
        BookingAssignment directOffer = BookingAssignment.builder()
                .booking(booking)
                .nurseUser(candidateUser)
                .status("RINGING")
                .notifiedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(30)) // Strict 30 second window countdown
                .build();

        assignmentRepository.save(directOffer);
        log.info("Dispatched live request offer ticket [{}] out to Practitioner ID: {}", booking.getBookingNo(), candidateUser.getMobile());
        alertService.broadcastNewOfferToNurse(candidateUser.getMobile(), directOffer);
    }

    @Override
    @Transactional
    public void processNurseResponse(String bookingNo, String mobile, boolean accept, String reason) {
        BookingAssignment assignment = assignmentRepository.findByBookingBookingNoAndNurseUserMobile(bookingNo, mobile)
                .orElseThrow(() -> new ApplicationException("No active offering reference located for selection parameters."));

        if (!"RINGING".equals(assignment.getStatus())) {
            throw new ApplicationException("Action deadline expired. Request already processed.");
        }

        Booking booking = assignment.getBooking();

        if (accept) {
            assignment.setStatus("ACCEPTED");
            booking.setBookingStatus("ACCEPTED");
            booking.setAssignedNurseUser(assignment.getNurseUser());
            log.info("Ticket [{}] locked and confirmed by nurse: {}", bookingNo, mobile);
            alertService.broadcastAcceptanceToPatient(
                    booking.getPatientUser().getMobile(),
                    bookingNo,
                    assignment.getNurseUser().getName()
            );
        } else {
            assignment.setStatus("REJECTED");
            log.info("Ticket [{}] rejected by nurse: {}. Reason: {}", bookingNo, mobile, reason);
            triggerChainedDispatch(booking);
        }

        assignmentRepository.save(assignment);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseDashboardResponse> getNurseDashboardFeed(String nurseMobile) {
        List<BookingAssignment> activeAssignments = assignmentRepository.findActiveNurseDashboardFeeds(nurseMobile);

        return activeAssignments.stream().map(assignment -> {
            Booking booking = assignment.getBooking();

            BigDecimal combinedPrice = booking.getSelectedItems().stream()
                    .map(BookingItem::getPriceCharged)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<String> serviceNames = booking.getSelectedItems().stream()
                    .map(BookingItem::getItemName)
                    .collect(Collectors.toList());

            return NurseDashboardResponse.builder()
                    .bookingId(booking.getBookingNo())
                    .patientName(booking.getPatientUser().getName())
                    .patientMobile(booking.getPatientUser().getMobile())
                    .bookingDate(booking.getBookingDate().toString())
                    .bookingTime(booking.getBookingTime())
                    .address(booking.getRawAddress())
                    .remarks(booking.getRemarks())
                    .totalPrice(combinedPrice)
                    .bookingStatus(booking.getBookingStatus())
                    .assignmentStatus(assignment.getStatus())
                    .selectedServices(serviceNames)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void completeBookingService(String nurseMobile, CompleteBookingRequest request) {
        Booking booking = bookingRepository.findByBookingNo(request.getBookingId())
                .orElseThrow(() -> new ApplicationException("Booking record not found with ID: " + request.getBookingId()));

        if (booking.getAssignedNurseUser() == null || !booking.getAssignedNurseUser().getMobile().equals(nurseMobile)) {
            throw new ApplicationException("Security validation exception: Unauthorized provider completion update attempt.");
        }

        if ("COMPLETED".equals(booking.getBookingStatus())) {
            throw new ApplicationException("Operational collision: This booking transaction has already been closed.");
        }

        booking.setBookingStatus("COMPLETED");
        booking.setRemarks(booking.getRemarks() + " | Final Nurse Execution Remarks: " + request.getRemarks());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        walletService.processServiceCompletionEarnings(booking);

        nurseProfileRepository.updateDutyAvailability(booking.getAssignedNurseUser().getId(), true);

        log.info("Ticket [{}] transitioned successfully to COMPLETED state by practitioner: {}", booking.getBookingNo(), nurseMobile);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<BookingHistoryResponse> getPatientCompleteHistory(String patientMobile) {
        List<Booking> historicalRecords = bookingRepository.findByPatientUserMobileOrderByCreatedAtDesc(patientMobile);
        return mapBookingsToHistoryResponse(historicalRecords, true);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<BookingHistoryResponse> getNurseCompleteHistory(String nurseMobile) {
        List<Booking> historicalRecords = bookingRepository.findByAssignedNurseUserMobileOrderByCreatedAtDesc(nurseMobile);
        return mapBookingsToHistoryResponse(historicalRecords, false);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BookingHistoryResponse getSingleBookingDetails(String bookingNo) {
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Booking transaction not found for ticker hash: " + bookingNo));

        return convertToHistoryItem(booking, true); // Default mapping strategy context
    }

    // Private helper to process the conversions efficiently
    private List<BookingHistoryResponse> mapBookingsToHistoryResponse(List<Booking> bookings, boolean isPatientView) {
        return bookings.stream()
                .map(b -> convertToHistoryItem(b, isPatientView))
                .collect(Collectors.toList());
    }

    private BookingHistoryResponse convertToHistoryItem(Booking b, boolean isPatientView) {
        BigDecimal total = b.getSelectedItems().stream()
                .map(BookingItem::getPriceCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> services = b.getSelectedItems().stream()
                .map(BookingItem::getItemName)
                .collect(Collectors.toList());

        String counterpartyName = "Searching for Provider...";
        String counterpartyMobile = "";

        // Default coordinates to null if no provider is locked down yet
        Double liveLat = null;
        Double liveLon = null;

        if (isPatientView) {
            if (b.getAssignedNurseUser() != null) {
                counterpartyName = b.getAssignedNurseUser().getName();
                counterpartyMobile = b.getAssignedNurseUser().getMobile();

                // --- NEW: Dynamically fetch the provider's current real-time GPS coordinates ---
                if (b.getAssignedNurseUser().getNurseProfile() != null) {
                    liveLat = b.getAssignedNurseUser().getNurseProfile().getLatitude();
                    liveLon = b.getAssignedNurseUser().getNurseProfile().getLongitude();
                }
            }
        } else {
            counterpartyName = b.getPatientUser().getName();
            counterpartyMobile = b.getPatientUser().getMobile();
        }

        return BookingHistoryResponse.builder()
                .bookingId(b.getBookingNo())
                .bookingDate(b.getBookingDate().toString())
                .bookingTime(b.getBookingTime())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .address(b.getRawAddress())
                .remarks(b.getRemarks())
                .prescriptionUrl(b.getPrescriptionFilePath())
                .CounterpartyName(counterpartyName)
                .CounterpartyMobile(counterpartyMobile)
                .nurseLatitude(liveLat)
                .nurseLongitude(liveLon)

                .totalAmount(total)
                .serviceNames(services)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingHistoryResponse> getFilteredPatientHistory(String patientMobile, BookingHistoryFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.buildPatientSpec(patientMobile, filter);
        
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy())
        );
        
        Page<Booking> bookings = bookingRepository.findAll(spec, pageable);
        return bookings.map(b -> convertToHistoryItem(b, true));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingHistoryResponse> getFilteredNurseHistory(String nurseMobile, BookingHistoryFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.buildNurseSpec(nurseMobile, filter);
        
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy())
        );
        
        Page<Booking> bookings = bookingRepository.findAll(spec, pageable);
        return bookings.map(b -> convertToHistoryItem(b, false));
    }
    @Override
    @Transactional
    public String bookWithFavoriteNurse(String patientMobile, BookWithFavoriteRequest request) {
        var patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        var favoriteNurse = userRepository.findById(request.getFavoriteNurseUserId())
                .orElseThrow(() -> new ApplicationException("Favorite nurse not found"));

        // Verify nurse is actually in favorites
        if (!favoriteRepository.existsByPatientIdAndNurseId(patient.getId(), favoriteNurse.getId())) {
            throw new ApplicationException("This nurse is not in your favorites");
        }

        // Verify nurse is available
        var nurseProfile = nurseProfileRepository.findByUserMobile(favoriteNurse.getMobile())
                .orElseThrow(() -> new ApplicationException("Nurse profile not found"));

        if (!nurseProfile.isOnDuty()) {
            throw new ApplicationException("Your favorite nurse is currently off-duty");
        }

        List<MedicalService> targetServices = serviceRepository.findAllById(request.getSelectedServiceIds());

        String ticketNo = "FAV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = Booking.builder()
                .bookingNo(ticketNo)
                .patientUser(patient)
                .assignedNurseUser(favoriteNurse) // Direct assignment
                .bookingStatus("CONFIRMED") // Skip dispatch, directly confirmed
                .paymentStatus("PENDING")
                .bookingDate(LocalDate.parse(request.getBookingDate()))
                .bookingTime(request.getBookingTime())
                .remarks(request.getRemarks())
                .latitude(Double.parseDouble(request.getLatitude()))
                .longitude(Double.parseDouble(request.getLongitude()))
                .rawAddress(request.getAddress())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<BookingItem> items = targetServices.stream().map(service -> BookingItem.builder()
                .booking(booking)
                .service(service)
                .itemName(service.getName())
                .priceCharged(service.getBasePrice())
                .build()
        ).collect(Collectors.toList());

        booking.setSelectedItems(items);
        bookingRepository.save(booking);

        // Notify the favorite nurse directly
        alertService.broadcastNewOfferToNurse(favoriteNurse.getMobile(), 
            BookingAssignment.builder()
                .booking(booking)
                .nurseUser(favoriteNurse)
                .status("CONFIRMED")
                .notifiedAt(LocalDateTime.now())
                .build());

        log.info("Quick booking with favorite nurse created: {} assigned to {}", ticketNo, favoriteNurse.getMobile());

        return ticketNo;
    }
}