package com.pronurse.auth.controller;

import com.pronurse.auth.dto.*;
import com.pronurse.auth.entity.RefreshToken;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.auth.security.RateLimitingService;
import com.pronurse.auth.service.AuthService;
import com.pronurse.auth.service.RefreshTokenService;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.payload.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RateLimitingService rateLimitingService;

    /**
     * Step 1: Send/Initiate OTP workflow for a mobile number
     */
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(@Valid @RequestBody OtpRequest request) {
        logger.info("Received OTP generation request for mobile: {}", request.getMobile());

        // Rate Limiting Interceptor
        if (!rateLimitingService.isAllowed(request.getMobile())) {
            logger.warn("Rate limit tripped for mobile number: {}", request.getMobile());
            return ResponseEntity.status(429) // HTTP 429 Too Many Requests
                    .body(new ApiResponse<>(false, "Too many requests. Please wait before requesting another OTP.", null));
        }

        authService.initiateOtpWorkflow(request.getMobile());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "OTP sent successfully. For local testing use default OTP: 123456",
                null
        ));
    }
    /**
     * Step 2: Verify OTP and log in / register the user seamlessly
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyAndAuthenticate(@Valid @RequestBody OtpVerificationRequest request) {
        logger.info("Processing OTP verification for mobile: {}", request.getMobile());

        AuthResponse authResponse = authService.verifyOtpAndAuthenticate(request);

        // Clear rate limits upon successful verification handshake
        rateLimitingService.resetLimits(request.getMobile());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new ApiResponse<>(true, "Authentication successful", authResponse));
    }

    /**
     * Step 3: Silent Access Token Refresh using HttpOnly cookie rotation
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        try {
            if (refreshToken == null) {
                throw new ApplicationException("Refresh token missing from cookies");
            }

            // Rotate the refresh token in database
            RefreshToken newTokenEntity = authService.rotateRefreshToken(refreshToken);
            User user = authService.getUserByMobile(newTokenEntity.getMobile());

            // Generate clean Access Token (without old tenant parameters)
            String newAccessToken = jwtUtil.generateAccessToken(user);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", newTokenEntity.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(Duration.ofDays(7))
                    .build();

            AuthResponse authResponse = new AuthResponse(newAccessToken, user.getRole().name());
            logger.info("Token rotation completed for user: {}", user.getMobile());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new ApiResponse<>(true, "Token refreshed successfully", authResponse));

        } catch (Exception e) {
            logger.error("Token refresh sequence failed: {}", e.getMessage());

            // Instantly wipe out stale/malicious cookies
            ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(0)
                    .build();

            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(new ApiResponse<>(false, "Session expired, please log in again", null));
        }
    }

    /**
     * Step 4: Logout and Session Invalidation
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
            logger.info("Successfully dropped session token from DB.");
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new ApiResponse<>(true, "Logged out successfully", null));
    }
}