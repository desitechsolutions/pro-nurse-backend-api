package com.pronurse.auth;

import com.pronurse.auth.dto.OtpVerificationRequest;
import com.pronurse.auth.dto.AuthResponse;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.service.AuthService;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testOtpVerificationAndUserCreation() {
        // Arrange
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setMobile("9999999999");
        request.setOtp("123456");
        request.setName("Test User");
        request.setRoleType(Role.PATIENT);

        // Act
        AuthResponse response = authService.verifyOtpAndAuthenticate(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("PATIENT", response.getRole());
        assertNotNull(response.getRefreshToken());

        // Verify user was created in database
        var user = userRepository.findByMobile("9999999999");
        assertTrue(user.isPresent());
        assertEquals("Test User", user.get().getName());
    }

    @Test
    void testOtpVerificationForExistingUser() {
        // Arrange - Create user first
        OtpVerificationRequest firstRequest = new OtpVerificationRequest();
        firstRequest.setMobile("8888888888");
        firstRequest.setOtp("123456");
        firstRequest.setName("Existing User");
        firstRequest.setRoleType(Role.NURSE);
        authService.verifyOtpAndAuthenticate(firstRequest);

        // Act - Login again
        OtpVerificationRequest secondRequest = new OtpVerificationRequest();
        secondRequest.setMobile("8888888888");
        secondRequest.setOtp("123456");
        secondRequest.setRoleType(Role.NURSE);
        
        AuthResponse response = authService.verifyOtpAndAuthenticate(secondRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("NURSE", response.getRole());
    }
}

// Made with Bob
