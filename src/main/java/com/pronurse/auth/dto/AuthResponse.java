package com.pronurse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing security tokens and user details")
public class AuthResponse {

    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "User role", example = "PATIENT")
    private String role;

    @Schema(description = "Refresh Token used to get new access token", example = "4e6f3b7c-...")
    private String refreshToken;

    @Schema(description = "User unique identifier", example = "1")
    private Long userId;

    @Schema(description = "User display name", example = "Rahul Sharma")
    private String name;

    @Schema(description = "User mobile number", example = "9876543210")
    private String mobile;

    public AuthResponse(String accessToken, String role) {
        this.accessToken = accessToken;
        this.role = role;
    }

    public AuthResponse(String accessToken, String role, String refreshToken) {
        this.accessToken = accessToken;
        this.role = role;
        this.refreshToken = refreshToken;
    }
}