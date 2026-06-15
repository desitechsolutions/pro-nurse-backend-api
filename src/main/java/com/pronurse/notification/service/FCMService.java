package com.pronurse.notification.service;

import com.pronurse.notification.dto.FCMTokenRequest;
import com.pronurse.notification.dto.NotificationPreferenceRequest;
import com.pronurse.notification.entity.NotificationPreference;

import java.util.List;
import java.util.Map;

public interface FCMService {
    
    /**
     * Register a new FCM device token for a user
     */
    void registerToken(String mobile, FCMTokenRequest request);
    
    /**
     * Send push notification to a specific user
     */
    void sendNotification(String mobile, String title, String body, Map<String, String> data);
    
    /**
     * Send push notification to multiple users
     */
    void sendBulkNotification(List<String> mobiles, String title, String body, Map<String, String> data);
    
    /**
     * Update user's notification preferences
     */
    void updatePreferences(String mobile, NotificationPreferenceRequest request);
    
    /**
     * Get user's notification preferences
     */
    NotificationPreference getPreferences(String mobile);
    
    /**
     * Deactivate a device token
     */
    void deactivateToken(String deviceToken);
    
    /**
     * Remove all inactive tokens for a user
     */
    void cleanupInactiveTokens(String mobile);
}

// Made with Bob
