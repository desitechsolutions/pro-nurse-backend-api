package com.pronurse.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Payload for updating the nurse's live GPS coordinates")
public class LocationUpdateRequest {
    @NotBlank(message = "Latitude coordinate streaming point required")
    @Schema(description = "Latitude of the current location", example = "28.4595", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;

    @NotBlank(message = "Longitude coordinate streaming point required")
    @Schema(description = "Longitude of the current location", example = "77.0266", requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;
}