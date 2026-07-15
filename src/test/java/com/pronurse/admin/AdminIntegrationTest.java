package com.pronurse.admin;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.admin.dto.VerifyNurseRequest;
import com.pronurse.admin.dto.UpdateServiceRequest;
import com.pronurse.admin.dto.ReviewModerationRequest;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.catalog.entity.MedicalService;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.config.TestConfig;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.review.entity.NurseReview;
import com.pronurse.review.repository.NurseReviewRepository;
import com.pronurse.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NurseProfileRepository nurseProfileRepository;

    @Autowired
    private MedicalServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NurseReviewRepository reviewRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User nurseUser;
    private NurseProfile nurseProfile;
    private String adminToken;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        nurseProfileRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        // Create Admin User
        adminUser = TestDataFactory.createUser("9876543212", Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateAccessToken(adminUser);

        // Create Unapproved Nurse
        nurseUser = TestDataFactory.createUser("9876543211", Role.NURSE);
        nurseUser = userRepository.save(nurseUser);
        nurseProfile = TestDataFactory.createNurseProfile(nurseUser, "NUR-555");
        nurseProfile.setVerificationStatus("Pending");
        nurseProfile.setVerified(false);
        nurseProfile = nurseProfileRepository.save(nurseProfile);
    }

    @Test
    void testVerifyNurseSuccess() throws Exception {
        VerifyNurseRequest request = new VerifyNurseRequest();
        request.setNurseProfileId(nurseProfile.getId());
        request.setStatus("Approved");
        request.setRejectReason("Credentials checked");

        mockMvc.perform(post("/api/admin/nurse/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        NurseProfile updated = nurseProfileRepository.findById(nurseProfile.getId()).orElseThrow();
        assertEquals("Approved", updated.getVerificationStatus());
        assertTrue(updated.isVerified());
    }

    @Test
    void testSaveCatalogServiceSuccess() throws Exception {
        UpdateServiceRequest request = new UpdateServiceRequest();
        request.setName("General Nursing Care");
        request.setDescription("Daily medical assistance");
        request.setBasePrice(BigDecimal.valueOf(1500.00));
        request.setEstimatedDurationMinutes(240);
        request.setIsActive(true);

        mockMvc.perform(post("/api/admin/services/save")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, serviceRepository.count());
    }

    @Test
    void testModerateNurseReviewSuccess() throws Exception {
        // Create user
        User patientUser = TestDataFactory.createUser("9876543210", Role.PATIENT);
        patientUser = userRepository.save(patientUser);

        // Save booking for reviews
        Booking booking = TestDataFactory.createBooking(patientUser, "BOOK-MOD-100");
        booking = bookingRepository.save(booking);

        NurseReview review = TestDataFactory.createNurseReview(booking, nurseUser, patientUser, 4);
        review = reviewRepository.save(review);

        // 1. Test APPROVE action (no-op/log only)
        ReviewModerationRequest request = new ReviewModerationRequest();
        request.setReviewId(review.getId());
        request.setAction("APPROVE");

        mockMvc.perform(post("/api/admin/reviews/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertTrue(reviewRepository.existsById(review.getId()));

        // 2. Test DELETE action (removes the review)
        request.setAction("DELETE");

        mockMvc.perform(post("/api/admin/reviews/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(reviewRepository.existsById(review.getId()));
    }

    @Test
    void testGetSystemAnalyticsMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
