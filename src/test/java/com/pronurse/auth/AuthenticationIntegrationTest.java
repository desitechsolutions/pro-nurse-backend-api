package com.pronurse.auth;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.dto.*;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.config.TestConfig;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testPasswordRegistrationSuccess() throws Exception {
        PasswordRegisterRequest request = new PasswordRegisterRequest();
        request.setLoginType("Patient");
        request.setName("Rahul Sharma");
        request.setMobile("9876543210");
        request.setPassword("ef797c8118f02dfb649607dd5d3f8c76");
        request.setEmail("rahul@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        assertTrue(userRepository.findByMobile("9876543210").isPresent());
    }

    @Test
    void testPasswordRegistrationValidationFailure() throws Exception {
        PasswordRegisterRequest request = new PasswordRegisterRequest();
        request.setLoginType("Patient");
        request.setName(""); // Invalid name
        request.setMobile("12345"); // Invalid mobile format
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendOtpSuccess() throws Exception {
        OtpRequest request = new OtpRequest();
        request.setMobile("9876543210");

        mockMvc.perform(post("/api/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully. For local testing use default OTP: 123456"));
    }

    @Test
    void testVerifyOtpAndRegistrationFlow() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setMobile("9876543215");
        request.setOtp("123456");
        request.setName("Jane Doe");
        request.setRoleType(Role.PATIENT);

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("PATIENT"));

        assertTrue(userRepository.findByMobile("9876543215").isPresent());
    }

    @Test
    void testVerifyOtpInvalidToken() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setMobile("9876543215");
        request.setOtp("999999"); // Wrong OTP
        request.setRoleType(Role.PATIENT);

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLogoutAndRefreshFlows() throws Exception {
        // Create user
        User user = new User();
        user.setMobile("9876543220");
        user.setName("Test User");
        user.setRole(Role.PATIENT);
        userRepository.save(user);

        // Perform login verification
        OtpVerificationRequest verifyReq = new OtpVerificationRequest();
        verifyReq.setMobile("9876543220");
        verifyReq.setOtp("123456");
        verifyReq.setRoleType(Role.PATIENT);

        String loginResult = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract refresh token (it will be set in the cookie and the response body)
        String refreshToken = objectMapper.readTree(loginResult).path("data").path("refreshToken").asText();

        // Logout using cookie
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
