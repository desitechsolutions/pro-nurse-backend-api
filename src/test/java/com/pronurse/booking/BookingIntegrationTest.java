package com.pronurse.booking;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.booking.dto.CreateBookingRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.config.TestConfig;
import com.pronurse.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingAssignmentRepository assignmentRepository;

    @Autowired
    private MedicalServiceRepository serviceRepository;

    @Autowired
    private com.pronurse.nurse.repository.NurseProfileRepository nurseProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User patientUser;
    private User nurseUser;
    private com.pronurse.nurse.entity.NurseProfile nurseProfile;
    private MedicalService service;
    private String patientToken;
    private String nurseToken;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        bookingRepository.deleteAll();
        serviceRepository.deleteAll();
        nurseProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Create standard Patient
        patientUser = TestDataFactory.createUser("9876543210", Role.PATIENT);
        patientUser = userRepository.save(patientUser);
        patientToken = jwtUtil.generateAccessToken(patientUser);

        // Create standard Nurse
        nurseUser = TestDataFactory.createUser("9876543211", Role.NURSE);
        nurseUser = userRepository.save(nurseUser);
        nurseToken = jwtUtil.generateAccessToken(nurseUser);

        // Create and save NurseProfile so the nurse is available for booking dispatch
        nurseProfile = TestDataFactory.createNurseProfile(nurseUser, "NUR-77788");
        nurseProfile.setVerificationStatus("Approved");
        nurseProfile.setVerified(true);
        nurseProfile = nurseProfileRepository.save(nurseProfile);

        // Create service catalog item
        service = TestDataFactory.createMedicalService("ICU Nursing Care", BigDecimal.valueOf(1200.00));
        service = serviceRepository.save(service);
    }

    @Test
    void testCreateBookingSuccess() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setSelectedServiceIds(List.of(service.getId()));
        request.setBookingDate(LocalDate.now().toString());
        request.setBookingTime("10:00 AM");
        request.setRemarks("Need ICU trained nurse");
        request.setLatitude("28.6289");
        request.setLongitude("77.3649");
        request.setAddress("Sector 62, Noida");
        request.setHasInjection(true);
        request.setPrice(BigDecimal.valueOf(1200.00));
        request.setPaymentMode("ONLINE");

        mockMvc.perform(post("/api/booking/create")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        List<Booking> bookings = bookingRepository.findByPatientUserMobileOrderByCreatedAtDesc(patientUser.getMobile());
        assertEquals(1, bookings.size());
        assertEquals("PENDING", bookings.get(0).getBookingStatus());
    }

    @Test
    void testCreateBookingWithPrescriptionSuccess() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setSelectedServiceIds(List.of(service.getId()));
        request.setBookingDate(LocalDate.now().toString());
        request.setBookingTime("10:00 AM");
        request.setLatitude("28.6289");
        request.setLongitude("77.3649");
        request.setAddress("Sector 62, Noida");
        request.setPrice(BigDecimal.valueOf(1200.00));
        request.setPaymentMode("ONLINE");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(request).getBytes()
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "prescription",
                "presc.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy-prescription-bytes".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/booking/create-with-prescription")
                        .file(dataPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testAcceptRejectBookingLifecycle() throws Exception {
        // Create booking
        Booking booking = TestDataFactory.createBooking(patientUser, "BOOK-TEST-123");
        booking.setLatitude(28.6289);
        booking.setLongitude(77.3649);
        booking = bookingRepository.save(booking);

        // Add Booking Item
        com.pronurse.booking.entity.BookingItem item = TestDataFactory.createBookingItem(booking, service);
        booking.getSelectedItems().add(item);
        booking = bookingRepository.save(booking);

        // Create assignment (offer to nurse)
        BookingAssignment assignment = TestDataFactory.createBookingAssignment(booking, nurseUser);
        assignment = assignmentRepository.save(assignment);

        // Nurse accepts offer using mobile controller endpoint
        String acceptBody = "{\"booking_no\":\"" + booking.getBookingNo() + "\",\"nurse_id\":" + nurseUser.getId() + "}";
        mockMvc.perform(post("/api/nurse/booking/accept")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.message").value("Booking accepted successfully."));

        // Verify status is updated to ACCEPTED
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals("ACCEPTED", updated.getBookingStatus());
        assertEquals(nurseUser.getId(), updated.getAssignedNurseUser().getId());
    }

    @Test
    void testGetPatientHistoryLog() throws Exception {
        Booking booking = TestDataFactory.createBooking(patientUser, "BOOK-TEST-555");
        bookingRepository.save(booking);

        mockMvc.perform(get("/api/booking/patient/list")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].bookingId").value("BOOK-TEST-555"));
    }
}
