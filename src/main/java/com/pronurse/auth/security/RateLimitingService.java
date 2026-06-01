package com.pronurse.auth.security;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Tracks: Mobile Number -> Last Sent Timestamp
    private final Map<String, Instant> otpCooldownMap = new ConcurrentHashMap<>();

    // Tracks: Mobile Number -> Request Count within window
    private final Map<String, Integer> requestCounterMap = new ConcurrentHashMap<>();

    private static final int COOLDOWN_SECONDS = 60; // 1 minute between consecutive requests
    private static final int MAX_ATTEMPTS_PER_WINDOW = 3;

    /**
     * Evaluates whether a phone number is behaving maliciously.
     * @return true if the mobile can proceed, false if rate-limited.
     */
    public boolean isAllowed(String mobile) {
        Instant now = Instant.now();

        // 1. Enforce strict 1-minute cooldown rule between direct clicks
        if (otpCooldownMap.containsKey(mobile)) {
            Instant lastSent = otpCooldownMap.get(mobile);
            if (now.isBefore(lastSent.plusSeconds(COOLDOWN_SECONDS))) {
                return false;
            }
        }

        // 2. Track window caps (Max 3 attempts per session block before dynamic block)
        int attempts = requestCounterMap.getOrDefault(mobile, 0);
        if (attempts >= MAX_ATTEMPTS_PER_WINDOW) {
            return false;
        }

        // Increment attempts and update cooldown tracking timestamp
        requestCounterMap.put(mobile, attempts + 1);
        otpCooldownMap.put(mobile, now);
        return true;
    }

    /**
     * Resets counters whenever the user successfully verifies their OTP token.
     */
    public void resetLimits(String mobile) {
        otpCooldownMap.remove(mobile);
        requestCounterMap.remove(mobile);
    }
}