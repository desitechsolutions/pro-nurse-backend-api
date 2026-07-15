package com.pronurse.nurse;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.config.TestConfig;
import com.pronurse.nurse.dto.LocationUpdateRequest;
import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class NurseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NurseProfileRepository nurseProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User nurseUser;
    private NurseProfile nurseProfile;
    private String token;

    @BeforeEach
    void setUp() {
        nurseProfileRepository.deleteAll();
        userRepository.deleteAll();

        nurseUser = TestDataFactory.createUser("9876543211", Role.NURSE);
        nurseUser = userRepository.save(nurseUser);

        nurseProfile = TestDataFactory.createNurseProfile(nurseUser, "NUR-12345");
        nurseProfile = nurseProfileRepository.save(nurseProfile);

        token = jwtUtil.generateAccessToken(nurseUser);
    }

    @Test
    void testGetMyProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/nurse/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nurseId").value("NUR-12345"))
                .andExpect(jsonPath("$.data.specialization").value("ICU Care"));
    }

    @Test
    void testUpdateNurseProfileSuccess() throws Exception {
        NurseProfileUpdateRequest updateRequest = new NurseProfileUpdateRequest();
        updateRequest.setName("Test User 9876543211");
        updateRequest.setEmail("nurse@test.com");
        updateRequest.setGender(com.pronurse.enums.Gender.FEMALE);
        updateRequest.setDob(java.time.LocalDate.parse("1996-09-22"));
        updateRequest.setAddress("Lucknow Gomti Nagar");
        updateRequest.setQualification("M.Sc Nursing");
        updateRequest.setExperience("8 Years");
        updateRequest.setSpecialization("Pediatric Care");
        updateRequest.setLanguages("English, French");
        updateRequest.setLatitude("26.8467");
        updateRequest.setLongitude("80.9462");
        updateRequest.setRegistrationNumber("REG-55555");
        updateRequest.setCity("Lucknow");
        updateRequest.setConsultationFee(BigDecimal.valueOf(750.00));

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateRequest)
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/nurse/profile/update")
                        .file(dataPart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        NurseProfile updated = nurseProfileRepository.findById(nurseProfile.getId()).orElseThrow();
        assertEquals("M.Sc Nursing", updated.getQualification());
        assertEquals("Pediatric Care", updated.getSpecialization());
        assertEquals("Lucknow", updated.getCity());
        assertEquals(0, BigDecimal.valueOf(750.00).compareTo(updated.getConsultationFee()));
    }

    @Test
    void testSearchNursesSuccess() throws Exception {
        mockMvc.perform(get("/api/nurses/search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "ICU")
                        .param("city", "Lucknow")
                        .param("gender", "FEMALE")
                        .param("rating", "4.0")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Test User 9876543211"));
    }

    @Test
    void testUpdateLiveCoordinatesAndGetLastKnown() throws Exception {
        LocationUpdateRequest locationRequest = new LocationUpdateRequest();
        locationRequest.setLatitude("26.85");
        locationRequest.setLongitude("80.95");

        // Update Coordinates
        mockMvc.perform(post("/api/nurse/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Get last known location as Patient
        User patientUser = TestDataFactory.createUser("9876543210", Role.PATIENT);
        userRepository.save(patientUser);
        String patientToken = jwtUtil.generateAccessToken(patientUser);

        mockMvc.perform(get("/api/nurse/location/last-known/" + nurseUser.getMobile())
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.latitude").value(26.85))
                .andExpect(jsonPath("$.data.longitude").value(80.95));
    }
}
