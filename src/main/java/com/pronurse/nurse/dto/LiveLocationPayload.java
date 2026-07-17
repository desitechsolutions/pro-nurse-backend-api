package com.pronurse.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload representing live coordinate streaming updates for WebSocket/Map tracking")
public class LiveLocationPayload {
    @Schema(description = "Unique booking number", example = "BK-12345")
    private String bookingNo;

    @Schema(description = "Latitude coordinate", example = "28.4595")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "77.0266")
    private Double longitude;

    @Schema(description = "Optional: Heading angle in degrees to rotate the map marker", example = "180.0")
    private Double heading; // Optional: Direction the car is facing to rotate the map marker
}