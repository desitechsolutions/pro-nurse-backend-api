package com.pronurse.patient;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.config.TestConfig;
import com.pronurse.patient.dto.PatientProfileUpdateRequest;
import com.pronurse.patient.entity.PatientProfile;
import com.pronurse.patient.repository.PatientProfileRepository;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class PatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User patientUser;
    private PatientProfile patientProfile;
    private String token;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE fcm_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE favorite_nurses");
        jdbcTemplate.execute("TRUNCATE TABLE nurse_reviews");
        jdbcTemplate.execute("TRUNCATE TABLE booking_assignments");
        jdbcTemplate.execute("TRUNCATE TABLE bookings");
        jdbcTemplate.execute("TRUNCATE TABLE nurse_profiles");
        jdbcTemplate.execute("TRUNCATE TABLE patient_profiles");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        patientUser = TestDataFactory.createUser("9876543210", Role.PATIENT);
        patientUser = userRepository.save(patientUser);

        patientProfile = TestDataFactory.createPatientProfile(patientUser, "PAT-12345");
        patientProfile = patientProfileRepository.save(patientProfile);

        token = jwtUtil.generateAccessToken(patientUser);
    }

    @Test
    void testGetMyProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/patient/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientId").value("PAT-12345"))
                .andExpect(jsonPath("$.data.name").value("Test User 9876543210"))
                .andExpect(jsonPath("$.data.gender").value("MALE"));
    }

    @Test
    void testGetMyProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/patient/profile/me"))
                .andExpect(status().isForbidden()); // Access Denied / unauth in spring security config
    }

    @Test
    void testUpdatePatientProfileSuccess() throws Exception {
        PatientProfileUpdateRequest updateRequest = new PatientProfileUpdateRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setEmail("newemail@test.com");
        updateRequest.setGender(com.pronurse.enums.Gender.FEMALE);
        updateRequest.setDob(java.time.LocalDate.parse("1992-06-25"));
        updateRequest.setBloodGroup("A-");
        updateRequest.setAddress("Noida Sector 62");
        updateRequest.setLatitude("28.62");
        updateRequest.setLongitude("77.36");
        updateRequest.setChronicDiseases("Diabetes");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateRequest)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "profileImage",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "profile-image-bytes".getBytes()
        );

        MockMultipartFile reportPart = new MockMultipartFile(
                "medicalReport",
                "report.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "medical-report-bytes".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/patient/profile/update")
                        .file(dataPart)
                        .file(imagePart)
                        .file(reportPart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Patient profile records updated successfully"));

        // Verify updates
        PatientProfile updated = patientProfileRepository.findById(patientProfile.getId()).orElseThrow();
        assertEquals("A-", updated.getBloodGroup());
        assertEquals("Diabetes", updated.getChronicDiseases());
        assertEquals(LocalDate.of(1992, 6, 25), updated.getDob());
    }

    @Test
    void testUpdatePatientProfileInvalidDOB() throws Exception {
        String invalidJson = "{\"name\":\"Updated Name\",\"dob\":\"invalid-date-format\"}";

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                invalidJson.getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/patient/profile/update")
                        .file(dataPart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
