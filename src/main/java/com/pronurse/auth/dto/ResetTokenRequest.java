package com.pronurse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Schema(description = "Request payload containing the reset token details")
public class ResetTokenRequest {

    @Schema(description = "Token string for security reset sequence Verification", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private String token;
}
