package com.pronurse.notification.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.notification.dto.FCMTokenRequest;
import com.pronurse.notification.dto.NotificationPreferenceRequest;
import com.pronurse.notification.entity.NotificationPreference;
import com.pronurse.notification.service.FCMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Push Notifications", description = "FCM push notification management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final FCMService fcmService;

    @PostMapping("/register-token")
    @Operation(summary = "Register FCM device token", 
               description = "Register a device token for push notifications")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @Valid @RequestBody FCMTokenRequest request,
            Authentication authentication) {
        
        String mobile = authentication.getName();
        fcmService.registerToken(mobile, request);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Device token registered successfully")
                .build());
    }

    @DeleteMapping("/deactivate-token")
    @Operation(summary = "Deactivate FCM device token", 
               description = "Deactivate a device token (e.g., on logout)")
    public ResponseEntity<ApiResponse<Void>> deactivateToken(
            @RequestParam String deviceToken,
            Authentication authentication) {
        
        fcmService.deactivateToken(deviceToken);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Device token deactivated successfully")
                .build());
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences", 
               description = "Update user's notification preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {
        
        String mobile = authentication.getName();
        fcmService.updatePreferences(mobile, request);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Notification preferences updated successfully")
                .build());
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences", 
               description = "Retrieve user's current notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreference>> getPreferences(
            Authentication authentication) {
        
        String mobile = authentication.getName();
        NotificationPreference preferences = fcmService.getPreferences(mobile);
        
        return ResponseEntity.ok(ApiResponse.<NotificationPreference>builder()
                .success(true)
                .message("Preferences retrieved successfully")
                .data(preferences)
                .build());
    }

    @DeleteMapping("/cleanup-tokens")
    @Operation(summary = "Cleanup inactive tokens", 
               description = "Remove all inactive device tokens for the user")
    public ResponseEntity<ApiResponse<Void>> cleanupTokens(
            Authentication authentication) {
        
        String mobile = authentication.getName();
        fcmService.cleanupInactiveTokens(mobile);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Inactive tokens cleaned up successfully")
                .build());
    }
}


