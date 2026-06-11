package com.pronurse.auth.service;

import com.pronurse.auth.dto.AuthResponse;
import com.pronurse.auth.dto.OtpVerificationRequest;
import com.pronurse.auth.entity.RefreshToken;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.exception.UserInactiveException;
import com.pronurse.common.sms.SmsSender;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.patient.entity.PatientProfile;
import com.pronurse.patient.repository.PatientProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SmsSender smsSender;
    private final NurseProfileRepository nurseProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService,
                       SmsSender smsSender,
                       NurseProfileRepository nurseProfileRepository,
                       PatientProfileRepository patientProfileRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.smsSender = smsSender;
        this.nurseProfileRepository = nurseProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    /**
     * Step 1: Initialize the OTP Workflow.
     * Hooks cleanly into our profile-aware decoupled SmsSender component infrastructure.
     */
    public void initiateOtpWorkflow(String mobile) {
        String testOtp = "123456"; // Static fallback token for local testing profiles
        logger.info("Generating OTP verification handshakes sequence for mobile reference: {}", mobile);

        // Dispatches instantly via the interface (Logs to console in local mode, calls API in production mode)
        smsSender.sendOtp(mobile, testOtp);
    }

    /**
     * Step 2: Validate incoming OTP token and process registration or login.
     * Provisions both the user credentials record and the corresponding domain sub-profile context dynamically.
     */
    @Transactional
    public AuthResponse verifyOtpAndAuthenticate(OtpVerificationRequest request) {
        // 1. Validate the Token
        if (!"123456".equals(request.getOtp())) {
            logger.warn("Failed authentication attempt: Invalid OTP provided for mobile {}", request.getMobile());
            throw new BadCredentialsException("Invalid OTP token provided.");
        }

        // 2. Fetch or provision User record inside a single transactional block
        Optional<User> existingUser = userRepository.findByMobile(request.getMobile());
        User user;

        if (existingUser.isEmpty()) {
            logger.info("No matching record found. Provisioning a new user stub for mobile: {}", request.getMobile());
            user = new User();
            user.setMobile(request.getMobile());

            String structuredName = (request.getName() != null && !request.getName().isBlank())
                    ? request.getName()
                    : "User_" + request.getMobile().substring(request.getMobile().length() - 4);
            user.setName(structuredName);

            try {
                user.setRole(Role.valueOf(request.getRoleType().name()));
            } catch (IllegalArgumentException e) {
                throw new ApplicationException("Invalid role configuration value matched: " + request.getRoleType());
            }

            user.setMobileVerified(true);
            user.setActive(true);
            user = userRepository.save(user);

            // Dynamically bootstrap specialized parallel profiling rows right at user creation stage
            provisionDomainSubProfile(user);

        } else {
            user = existingUser.get();
            if (!user.isActive()) {
                throw new UserInactiveException("User account is disabled or inactive");
            }
            logger.info("Existing user verified successfully for mobile: {}", request.getMobile());
        }

        // 3. Drop existing stale sessions and generate clean access tokens
        refreshTokenService.deleteByMobile(user.getMobile());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getMobile());
        String accessToken = jwtUtil.generateAccessToken(user);

        return new AuthResponse(accessToken, user.getRole().name(), newRefreshToken.getToken());
    }

    /**
     * Isolated helper method to ensure profile database rows coexist seamlessly with security credentials.
     */
    private void provisionDomainSubProfile(User user) {
        if (user.getRole() == Role.NURSE) {
            NurseProfile nurseProfile = new NurseProfile();
            nurseProfile.setUser(user);
            // Generates a clean custom application identifier format string
            nurseProfile.setNurseId("NUR-" + user.getId() + "-" + System.currentTimeMillis() % 1000);
            nurseProfile.setVerificationStatus("Pending");
            nurseProfileRepository.save(nurseProfile);
            logger.info("Initialized matching placeholder clinical profile space for Nurse ID: {}", nurseProfile.getNurseId());

        } else if (user.getRole() == Role.PATIENT) {
            PatientProfile patientProfile = new PatientProfile();
            patientProfile.setUser(user);
            patientProfile.setPatientId("PAT-" + user.getId() + "-" + System.currentTimeMillis() % 1000);
            patientProfileRepository.save(patientProfile);
            logger.info("Initialized matching placeholder tracking profile space for Patient ID: {}", patientProfile.getPatientId());
        }
    }

    /**
     * Step 3: Handle structural cryptographic rotation for Refresh Tokens
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenStr) {
        RefreshToken oldToken = refreshTokenService.validateAndGet(oldTokenStr);
        User user = getUserByMobile(oldToken.getMobile());

        if (!user.isActive()) {
            throw new UserInactiveException("User account is inactive");
        }

        refreshTokenService.delete(oldToken);
        return refreshTokenService.createRefreshToken(user.getMobile());
    }

    /**
     * Core application lookup helper
     */
    public User getUserByMobile(String mobile) {
        return userRepository.findByMobile(mobile)
                .orElseThrow(() -> new UsernameNotFoundException("User record matching phone reference not found"));
    }
}