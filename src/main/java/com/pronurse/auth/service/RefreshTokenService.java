package com.pronurse.auth.service;

import com.pronurse.auth.entity.RefreshToken;
import com.pronurse.auth.repository.RefreshTokenRepository;
import com.pronurse.common.exception.TokenExpiredException; // Make sure to map this in your global handler
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    // Updated property key to match standard naming conventions
    @Value("${jwt.refreshExpiration:604800000}") // 7 days default fallback
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for a given mobile number.
     * Enforces a single active session footprint by dropping pre-existing records.
     */
    @Transactional
    public RefreshToken createRefreshToken(String mobile) {
        // Enforce clean session footprint
        refreshTokenRepository.deleteByMobile(mobile);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setMobile(mobile);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        logger.debug("Generating fresh secure session footprint token for device: {}", mobile);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Pulls and validates a token string from client cookie requests.
     * Auto-wipes token metadata from persistent store if expiration threshold has crossed.
     */
    @Transactional
    public RefreshToken validateAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token does not exist or has been invalidated"));

        if (isExpired(refreshToken)) {
            refreshTokenRepository.delete(refreshToken);
            logger.warn("Stale refresh token intercepted and purged for mobile: {}", refreshToken.getMobile());
            throw new TokenExpiredException("Session has expired. Please authenticate via OTP again.");
        }

        return refreshToken;
    }

    /**
     * Utility evaluation helper
     */
    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    /**
     * Invalidate all current active sessions matching a specific phone profile (e.g. forced logouts)
     */
    @Transactional
    public void deleteByMobile(String mobile) {
        refreshTokenRepository.deleteByMobile(mobile);
    }

    @Transactional
    public void delete(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }
}