package com.pronurse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Data Transfer Object representing user account details")
public class UserDto {
    @Schema(description = "Unique ID of the user", example = "10")
    private Long id;

    @Schema(description = "Username / Identifier of the user", example = "9876543210")
    private String username;

    @Schema(description = "Role associated with the user account", example = "PATIENT")
    private String role;

    @Schema(description = "Flag representing whether the user account is active", example = "true")
    private boolean active;

    @Schema(description = "First name of the user", example = "John")
    private String firstName;

    @Schema(description = "Last name of the user", example = "Doe")
    private String lastName;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Phone number of the user", example = "9876543210")
    private String phone;

    @Schema(description = "Shop/Facility numeric identifier context", example = "2")
    private Long shopId;

    @Schema(description = "Shop/Facility name context", example = "DesiTech Health Clinic")
    private String shopName;

    @Schema(description = "Timestamp when user account record was created", example = "2026-07-15T12:00:00")
    private LocalDateTime createdAt;
}