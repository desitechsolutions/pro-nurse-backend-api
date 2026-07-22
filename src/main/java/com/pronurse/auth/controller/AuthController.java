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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "01. Authentication", description = """
    Authentication and authorization endpoints for user registration, login, and session management.
    
    **Flow:**
    1. Send OTP → 2. Verify OTP & Get Tokens → 3. Use Access Token → 4. Refresh when expired → 5. Logout
    """)
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

    @Operation(
            summary = "Register user with password",
            description = "Creates a new User and provisions a corresponding role-based Patient or Nurse profile using password-based credentials.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Password registration attributes compatible with Flutter registration request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordRegisterRequest.class)
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User registration succeeded",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "User registered successfully",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or duplicate user registration request",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody PasswordRegisterRequest request) {
        logger.info("Registering user with password for mobile reference: {}", request.getMobile());
        authService.registerWithPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully", null));
    }

    @Operation(
            summary = "Step 1: Send OTP",
            description = """
                Initiates OTP workflow for user registration or login.
                
                **Process:**
                1. Validates mobile number format
                2. Checks rate limiting (max 3 requests per 5 minutes)
                3. Generates 6-digit OTP
                4. Sends OTP via SMS (in production) or logs it (in development)
                
                **Rate Limiting:** 3 requests per 5 minutes per mobile number
                
                **Test OTP:** For local testing, use OTP: `123456`
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Mobile number to send OTP",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OtpRequest.class),
                            examples = @ExampleObject(
                                    name = "Send OTP Example",
                                    value = """
                                            {
                                              "mobile": "9876543210"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "OTP sent successfully. For local testing use default OTP: 123456",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Too many requests. Please wait before requesting another OTP.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Step 2: Verify OTP & Authenticate",
            description = """
                Verifies OTP and authenticates user. Creates new user if first-time login.
                
                **Process:**
                1. Validates OTP (use `123456` for testing)
                2. Creates new user if mobile not registered
                3. Creates role-specific profile (Patient/Nurse)
                4. Generates JWT access token (10 hours validity)
                5. Creates refresh token (7 days validity)
                6. Sets HttpOnly cookie with refresh token
                7. Clears rate limits on successful verification
                
                **Roles:**
                - `PATIENT`: For patients booking services
                - `NURSE`: For healthcare providers
                - `ADMIN`: For platform administrators
                
                **Returns:** Access token, role, and refresh token
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "OTP verification details with user information",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OtpVerificationRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Patient Registration",
                                            value = """
                                                    {
                                                      "mobile": "9876543210",
                                                      "otp": "123456",
                                                      "name": "John Doe",
                                                      "roleType": "PATIENT"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Nurse Registration",
                                            value = """
                                                    {
                                                      "mobile": "9876543211",
                                                      "otp": "123456",
                                                      "name": "Jane Smith",
                                                      "roleType": "NURSE"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Existing User Login",
                                            value = """
                                                    {
                                                      "mobile": "9876543210",
                                                      "otp": "123456",
                                                      "roleType": "PATIENT"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Authentication successful",
                                              "data": {
                                                "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI5ODc2NTQzMjEwIiwibmFtZSI6IkpvaG4gRG9lIiwicm9sZSI6IlBBVElFTlQiLCJpYXQiOjE3MDMwMDAwMDAsImV4cCI6MTcwMzAzNjAwMH0.signature",
                                                "role": "PATIENT",
                                                "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid OTP",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Invalid OTP token provided."
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Step 3: Silent Access Token Refresh",
            description = "Silent Access Token Refresh using HttpOnly cookie rotation. Validates refresh token and returns a new access token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Session expired or invalid refresh token",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    })
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
    @Operation(
            summary = "Step 4: Logout and Session Invalidation",
            description = "Logs out the user and invalidates the session by removing the refresh token from the database and clearing cookies."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logged out successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
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