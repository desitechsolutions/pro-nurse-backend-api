package com.pronurse.auth.security;

import com.pronurse.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:36000000}") // 10 Hours default fallback
    private long jwtExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        if (keyBytes.length < 64) { // HS512 requires at least 512 bits (64 bytes)
            throw new IllegalArgumentException("JWT secret key is too weak for HS512 algorithm! Must be at least 512 bits.");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a clean Access Token.
     * Injects the phone identity as the primary subject and embeds Roles explicitly for Security Context integration.
     */
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getMobile())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(secretKey, Jwts.SIG.HS512) // Modern JJWT 0.12.x signing mechanism
                .compact();
    }

    // --- EXTRACTION METHODS ---

    public String extractMobile(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractName(String token) {
        return parseClaims(token).get("name", String.class);
    }

    // --- VALIDATION ---

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT payload handshake encountered: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}