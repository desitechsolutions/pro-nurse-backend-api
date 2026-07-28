package com.pronurse.notification.service;

import com.google.firebase.messaging.*;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.notification.dto.FCMTokenRequest;
import com.pronurse.notification.dto.NotificationPreferenceRequest;
import com.pronurse.notification.entity.FCMToken;
import com.pronurse.notification.entity.NotificationPreference;
import com.pronurse.notification.repository.FCMTokenRepository;
import com.pronurse.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMServiceImpl implements FCMService {

    private final FCMTokenRepository tokenRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerToken(String mobile, FCMTokenRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        // Check if token already exists
        FCMToken existingToken = tokenRepository.findByDeviceToken(request.getDeviceToken())
                .orElse(null);

        if (existingToken != null) {
            // Update existing token
            existingToken.setUser(user);
            existingToken.setDeviceType(request.getDeviceType());
            existingToken.setDeviceName(request.getDeviceName());
            existingToken.setIsActive(true);
            existingToken.setLastUsedAt(LocalDateTime.now());
            tokenRepository.save(existingToken);
            log.info("Updated existing FCM token for user: {}", mobile);
        } else {
            // Create new token
            FCMToken token = new FCMToken();
            token.setUser(user);
            token.setDeviceToken(request.getDeviceToken());
            token.setDeviceType(request.getDeviceType());
            token.setDeviceName(request.getDeviceName());
            token.setIsActive(true);
            tokenRepository.save(token);
            log.info("Registered new FCM token for user: {}", mobile);
        }

        // Deactivate other tokens for this user (optional - keep only one active token per user)
        // tokenRepository.deactivateOtherTokens(user.getId(), request.getDeviceToken());
    }

    @Override
    public void sendNotification(String mobile, String title, String body, Map<String, String> data) {
        try {
            User user = userRepository.findByMobile(mobile)
                    .orElseThrow(() -> new ApplicationException("User not found"));

            // Check notification preferences
            NotificationPreference prefs = preferenceRepository.findByUserId(user.getId())
                    .orElse(createDefaultPreferences(user));

            if (!shouldSendNotification(prefs, data)) {
                log.debug("Notification blocked by user preferences for: {}", mobile);
                return;
            }

            List<FCMToken> tokens = tokenRepository.findByUserIdAndIsActiveTrue(user.getId());
            
            if (tokens.isEmpty()) {
                log.warn("No active FCM tokens found for user: {}", mobile);
                return;
            }

            for (FCMToken token : tokens) {
                try {
                    Message message = Message.builder()
                            .setToken(token.getDeviceToken())
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .putAllData(data != null ? data : Map.of())
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .build())
                            .setApnsConfig(ApnsConfig.builder()
                                    .setAps(Aps.builder()
                                            .setSound("default")
                                            .build())
                                    .build())
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("Successfully sent notification to {}: {}", mobile, response);
                    
                    // Update last used timestamp
                    token.setLastUsedAt(LocalDateTime.now());
                    tokenRepository.save(token);
                    
                } catch (FirebaseMessagingException e) {
                    log.error("Failed to send notification to token: {}", token.getDeviceToken(), e);
                    
                    // Deactivate invalid tokens
                    if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT ||
                        e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                        token.setIsActive(false);
                        tokenRepository.save(token);
                        log.info("Deactivated invalid token for user: {}", mobile);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error sending notification to user: {}", mobile, e);
        }
    }

    @Override
    public void sendBulkNotification(List<String> mobiles, String title, String body, Map<String, String> data) {
        List<String> tokens = new ArrayList<>();
        
        for (String mobile : mobiles) {
            try {
                User user = userRepository.findByMobile(mobile).orElse(null);
                if (user == null) continue;

                NotificationPreference prefs = preferenceRepository.findByUserId(user.getId())
                        .orElse(createDefaultPreferences(user));

                if (!shouldSendNotification(prefs, data)) continue;

                List<FCMToken> userTokens = tokenRepository.findByUserIdAndIsActiveTrue(user.getId());
                tokens.addAll(userTokens.stream()
                        .map(FCMToken::getDeviceToken)
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                log.error("Error processing user {} for bulk notification", mobile, e);
            }
        }

        if (tokens.isEmpty()) {
            log.warn("No valid tokens found for bulk notification");
            return;
        }

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            log.info("Bulk notification sent. Success: {}, Failure: {}", 
                    response.getSuccessCount(), response.getFailureCount());
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send bulk notification", e);
        }
    }

    @Override
    @Transactional
    public void updatePreferences(String mobile, NotificationPreferenceRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        NotificationPreference prefs = preferenceRepository.findByUserId(user.getId())
                .orElse(createDefaultPreferences(user));

        if (request.getBookingAlerts() != null) {
            prefs.setBookingAlerts(request.getBookingAlerts());
        }
        if (request.getPaymentAlerts() != null) {
            prefs.setPaymentAlerts(request.getPaymentAlerts());
        }
        if (request.getReviewAlerts() != null) {
            prefs.setReviewAlerts(request.getReviewAlerts());
        }
        if (request.getPromotionalAlerts() != null) {
            prefs.setPromotionalAlerts(request.getPromotionalAlerts());
        }

        preferenceRepository.save(prefs);
        log.info("Updated notification preferences for user: {}", mobile);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreference getPreferences(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        return preferenceRepository.findByUserId(user.getId())
                .orElse(createDefaultPreferences(user));
    }

    @Override
    @Transactional
    public void deactivateToken(String deviceToken) {
        FCMToken token = tokenRepository.findByDeviceToken(deviceToken).orElse(null);
        if (token != null) {
            token.setIsActive(false);
            tokenRepository.save(token);
            log.info("Deactivated FCM token: {}", deviceToken);
        }
    }

    @Override
    @Transactional
    public void cleanupInactiveTokens(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        tokenRepository.deleteInactiveTokensByUserId(user.getId());
        log.info("Cleaned up inactive tokens for user: {}", mobile);
    }

    private NotificationPreference createDefaultPreferences(User user) {
        NotificationPreference prefs = new NotificationPreference();
        prefs.setUser(user);
        prefs.setBookingAlerts(true);
        prefs.setPaymentAlerts(true);
        prefs.setReviewAlerts(true);
        prefs.setPromotionalAlerts(false);
        return prefs;
    }

    private boolean shouldSendNotification(NotificationPreference prefs, Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return true;
        }

        String notificationType = data.get("type");
        if (notificationType == null) {
            return true;
        }

        return switch (notificationType) {
            case "BOOKING" -> prefs.getBookingAlerts();
            case "PAYMENT" -> prefs.getPaymentAlerts();
            case "REVIEW" -> prefs.getReviewAlerts();
            case "PROMOTIONAL" -> prefs.getPromotionalAlerts();
            case "ONBOARDING" -> true; // Onboarding alerts are always sent — nurses must receive these
            default -> true;
        };
    }
}


