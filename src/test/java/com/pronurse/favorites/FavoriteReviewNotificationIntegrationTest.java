package com.pronurse.favorites;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.config.TestConfig;
import com.pronurse.favorites.dto.AddFavoriteRequest;
import com.pronurse.favorites.repository.FavoriteNurseRepository;
import com.pronurse.notification.dto.FCMTokenRequest;
import com.pronurse.notification.repository.FCMTokenRepository;
import com.pronurse.review.dto.AddReviewRequest;
import com.pronurse.review.repository.NurseReviewRepository;
import com.pronurse.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class FavoriteReviewNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FavoriteNurseRepository favoriteNurseRepository;

    @Autowired
    private NurseReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FCMTokenRepository fcmTokenRepository;

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
    private String patientToken;
    private Booking booking;

    @BeforeEach
    void setUp() {
        fcmTokenRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        favoriteNurseRepository.deleteAll();
        serviceRepository.deleteAll();
        nurseProfileRepository.deleteAll();
        userRepository.deleteAll();

        patientUser = TestDataFactory.createUser("9876543210", Role.PATIENT);
        patientUser = userRepository.save(patientUser);
        patientToken = jwtUtil.generateAccessToken(patientUser);

        nurseUser = TestDataFactory.createUser("9876543211", Role.NURSE);
        nurseUser = userRepository.save(nurseUser);

        // Save nurse profile so the nurse exists in search/favorites
        nurseProfile = TestDataFactory.createNurseProfile(nurseUser, "NUR-77788");
        nurseProfile.setVerificationStatus("Approved");
        nurseProfile.setVerified(true);
        nurseProfile = nurseProfileRepository.save(nurseProfile);

        // Save booking for reviews
        booking = TestDataFactory.createBooking(patientUser, "BOOK-REV-100");
        booking.setBookingStatus("COMPLETED");
        booking.setAssignedNurseUser(nurseUser);
        booking = bookingRepository.save(booking);

        MedicalService service = TestDataFactory.createMedicalService("ICU Care", BigDecimal.valueOf(1000.00));
        service = serviceRepository.save(service);

        com.pronurse.booking.entity.BookingItem item = TestDataFactory.createBookingItem(booking, service);
        booking.getSelectedItems().add(item);
        booking = bookingRepository.save(booking);
    }

    @Test
    void testFavoriteLifecycle() throws Exception {
        AddFavoriteRequest addReq = new AddFavoriteRequest();
        addReq.setNurseUserId(nurseUser.getId());

        // Add
        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, favoriteNurseRepository.count());

        // Delete
        mockMvc.perform(delete("/api/favorites/" + nurseUser.getId())
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(0, favoriteNurseRepository.count());
    }

    @Test
    void testSubmitReviewSuccess() throws Exception {
        AddReviewRequest reviewRequest = new AddReviewRequest();
        reviewRequest.setBookingId(booking.getBookingNo());
        reviewRequest.setRating(5);
        reviewRequest.setReview("Excellent clinical care, very supportive.");

        mockMvc.perform(post("/api/review/add")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, reviewRepository.count());
    }

    @Test
    void testRegisterFCMTokenSuccess() throws Exception {
        FCMTokenRequest fcmRequest = new FCMTokenRequest();
        fcmRequest.setDeviceToken("fcm-device-token-12345");
        fcmRequest.setDeviceType("ANDROID");
        fcmRequest.setDeviceName("Pixel 7");

        mockMvc.perform(post("/api/notifications/register-token")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fcmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertTrue(fcmTokenRepository.findByDeviceToken("fcm-device-token-12345").isPresent());
    }
}
