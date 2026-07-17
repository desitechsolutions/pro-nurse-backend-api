package com.pronurse.booking;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.controller.MobileNurseActionController;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class MobileNurseActionControllerTest {

    @Autowired
    private MobileNurseActionController mobileNurseActionController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingAssignmentRepository assignmentRepository;

    private User nurseUser;
    private Booking testBooking;
    private BookingAssignment assignment;

    @BeforeEach
    void setUp() {
        // Create nurse user
        nurseUser = new User();
        nurseUser.setMobile("9999991234");
        nurseUser.setName("Test Nurse User");
        nurseUser.setRole(Role.NURSE);
        nurseUser = userRepository.save(nurseUser);

        // Create patient user
        User patientUser = new User();
        patientUser.setMobile("9999995678");
        patientUser.setName("Test Patient User");
        patientUser.setRole(Role.PATIENT);
        patientUser = userRepository.save(patientUser);

        // Create booking
        testBooking = new Booking();
        testBooking.setBookingNo("PN-TEST-MOBI-001");
        testBooking.setPatientUser(patientUser);
        testBooking.setBookingDate(LocalDate.now());
        testBooking.setBookingTime("10:00 AM");
        testBooking.setRawAddress("123 Test Street");
        testBooking.setLatitude(28.61);
        testBooking.setLongitude(77.20);
        testBooking.setBookingStatus("PENDING");
        testBooking = bookingRepository.save(testBooking);

        // Create assignment offer
        assignment = new BookingAssignment();
        assignment.setBooking(testBooking);
        assignment.setNurseUser(nurseUser);
        assignment.setStatus("RINGING");
        assignment.setNotifiedAt(LocalDateTime.now());
        assignment.setExpiresAt(LocalDateTime.now().plusHours(1));
        assignment = assignmentRepository.save(assignment);
    }

    @Test
    void testAcceptBookingSuccess() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                nurseUser.getMobile(), 
                null, 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_NURSE")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, Object> body = new HashMap<>();
        body.put("booking_no", testBooking.getBookingNo());
        body.put("nurse_id", nurseUser.getId());

        ResponseEntity<Map<String, Object>> response = mobileNurseActionController.acceptBooking(body, auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("status"));
        assertEquals("Booking accepted successfully.", response.getBody().get("message"));

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertNull(data.get("booking_id")); // Verify DB ID is not exposed
        assertEquals(testBooking.getBookingNo(), data.get("booking_no"));
        assertEquals("Accepted", data.get("status"));
    }

    @Test
    void testRejectBookingSuccess() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                nurseUser.getMobile(), 
                null, 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_NURSE")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, Object> body = new HashMap<>();
        body.put("booking_no", testBooking.getBookingNo());
        body.put("reason_id", 2);
        body.put("remarks", "Busy right now");

        ResponseEntity<Map<String, Object>> response = mobileNurseActionController.rejectBooking(body, auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("status"));
        assertEquals("Booking rejected successfully.", response.getBody().get("message"));

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertNull(data.get("booking_id")); // Verify DB ID is not exposed
        assertEquals(testBooking.getBookingNo(), data.get("booking_no"));
    }

    @Test
    void testCompleteBookingSuccess() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                nurseUser.getMobile(), 
                null, 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_NURSE")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Assign nurse to booking
        testBooking.setAssignedNurseUser(nurseUser);
        testBooking.setBookingStatus("ACCEPTED");
        testBooking = bookingRepository.save(testBooking);

        Map<String, Object> body = new HashMap<>();
        body.put("booking_no", testBooking.getBookingNo());
        body.put("remarks", "Patient was treated well");

        ResponseEntity<Map<String, Object>> response = mobileNurseActionController.completeBooking(body, auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("status"));
        assertEquals("Booking completed successfully.", response.getBody().get("message"));

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertNull(data.get("booking_id")); // Verify DB ID is not exposed
        assertEquals(testBooking.getBookingNo(), data.get("booking_no"));
        assertEquals("COMPLETED", data.get("booking_status"));
    }

    @Test
    void testCompleteBookingUnauthorized() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "9999990000", 
                null, 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_NURSE")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Assign nurse to booking
        testBooking.setAssignedNurseUser(nurseUser);
        testBooking.setBookingStatus("ACCEPTED");
        testBooking = bookingRepository.save(testBooking);

        Map<String, Object> body = new HashMap<>();
        body.put("booking_no", testBooking.getBookingNo());
        body.put("remarks", "Treated");

        assertThrows(com.pronurse.common.exception.ApplicationException.class, () -> {
            mobileNurseActionController.completeBooking(body, auth);
        });
    }

    @Test
    void testAcceptBookingMissingBookingNo() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                nurseUser.getMobile(), 
                null, 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_NURSE")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, Object> body = new HashMap<>(); // Missing booking_no

        ResponseEntity<Map<String, Object>> response = mobileNurseActionController.acceptBooking(body, auth);
        assertNotNull(response);
        assertFalse((Boolean) response.getBody().get("status"));
        assertEquals("Booking number is required.", response.getBody().get("message"));
    }

    @Test
    void testAcceptBookingInvalidBookingNo() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                nurseUser.getMobile(), 
                null, 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_NURSE")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, Object> body = new HashMap<>();
        body.put("booking_no", "NON_EXISTENT_BOOKING_123");

        assertThrows(com.pronurse.common.exception.ApplicationException.class, () -> {
            mobileNurseActionController.acceptBooking(body, auth);
        });
    }
}
