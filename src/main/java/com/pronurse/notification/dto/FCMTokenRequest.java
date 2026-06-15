package com.pronurse.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FCM token registration request")
public class FCMTokenRequest {

    @NotBlank(message = "Device token is required")
    @Schema(description = "FCM device token", example = "dXJlIGFuZCBzZWN1cmUgdG9rZW4...")
    private String deviceToken;

    @NotBlank(message = "Device type is required")
    @Schema(description = "Device type", example = "ANDROID", allowableValues = {"ANDROID", "IOS", "WEB"})
    private String deviceType;

    @Schema(description = "Device name", example = "Samsung Galaxy S21")
    private String deviceName;
}


