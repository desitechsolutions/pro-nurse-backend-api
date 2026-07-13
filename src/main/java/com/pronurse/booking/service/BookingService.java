package com.pronurse.booking.service;

import com.pronurse.booking.dto.CreateBookingRequest;
import com.pronurse.booking.dto.NurseDashboardResponse;
import com.pronurse.booking.dto.CompleteBookingRequest;
import com.pronurse.booking.dto.BookingHistoryResponse;
import com.pronurse.booking.dto.BookingHistoryFilterRequest;
import com.pronurse.booking.dto.BookWithFavoriteRequest;
import com.pronurse.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BookingService {

    /**
     * Processes incoming patient payload requests containing multiple sub-services,
     * calculates validation sums, and generates a unified booking tracking ticket.
     */
    String createMultiItemBooking(String patientMobile, CreateBookingRequest request);

    /**
     * Handles service booking creation alongside a multi-part medical prescription attachment.
     */
    String createMultiItemBookingWithPrescription(String patientMobile, CreateBookingRequest request, MultipartFile prescriptionFile);

    /**
     * Retrieves raw entities mapping back to a patient's historical transaction logs.
     */
    List<Booking> getPatientBookingHistory(String patientMobile);

    /**
     * Retrieves optimized data wrappers containing processed fee data for patient mobile tracking lists.
     */
    List<BookingHistoryResponse> getPatientCompleteHistory(String patientMobile);

    /**
     * Retrieves optimized historical assignment logs containing patient data cards for the nurse app.
     */
    List<BookingHistoryResponse> getNurseCompleteHistory(String nurseMobile);

    /**
     * Fetches details for a single target transaction ticket.
     */
    BookingHistoryResponse getSingleBookingDetails(String bookingNo);

    /**
     * Initializes a transaction record with Razorpay-ready payment payloads.
     */
    Map<String, Object> createRazorpayOrder(String bookingNo);

    /**
     * Validates cryptographic signature hashes against gateway payment hooks.
     */
    void verifyRazorpayPayment(String bookingNo, String paymentId, String orderId, String signature);

    /**
     * Determines the next geographically optimal, on-duty available practitioner
     * for a booking, completely bypassing anyone who has already timed out or rejected it.
     */
    void triggerChainedDispatch(Booking booking);

    /**
     * Processes transactional accept/reject inputs from targeted nurses, locking down
     * assignments upon acceptance or triggering immediate re-routing cascades on rejection.
     */
    void processNurseResponse(String bookingNo, String mobile, boolean accept, String reason);

    /**
     * Fetches the current live operations pipeline matching the nurse's profile tracking parameters.
     */
    List<NurseDashboardResponse> getNurseDashboardFeed(String nurseMobile);

    /**
     * Transitions a booking status to COMPLETED and releases the assigned nurse back into the available pool.
     */
    void completeBookingService(String nurseMobile, CompleteBookingRequest request);

    /**
     * Get filtered and paginated booking history for a patient
     */
    Page<BookingHistoryResponse> getFilteredPatientHistory(String patientMobile, BookingHistoryFilterRequest filter);

    /**
     * Get filtered and paginated booking history for a nurse
     */
    Page<BookingHistoryResponse> getFilteredNurseHistory(String nurseMobile, BookingHistoryFilterRequest filter);

    /**
     * Quick book with a favorite nurse (direct assignment, no dispatch)
     */
    String bookWithFavoriteNurse(String patientMobile, BookWithFavoriteRequest request);
}