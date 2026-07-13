package com.pronurse.nurse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationUpdateRequest {
    @NotBlank(message = "Latitude coordinate streaming point required")
    private String latitude;

    @NotBlank(message = "Longitude coordinate streaming point required")
    private String longitude;
}