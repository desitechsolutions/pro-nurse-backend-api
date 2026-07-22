package com.pronurse.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Request payload for creating or updating a medical service in the catalog")
public class UpdateServiceRequest {
    @NotBlank(message = "Service item nomenclature required")
    @Schema(description = "Name of the medical service procedure", example = "General Dressing", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed description of the medical service", example = "Wound cleaning and dressing using sterile technique.")
    private String description;

    @Schema(description = "Base price of the service in local currency", example = "500.00")
    private BigDecimal basePrice;

    @Schema(description = "Estimated duration of the service in minutes", example = "45")
    private Integer estimatedDurationMinutes;

    @NotNull(message = "Active operational availability status field required")
    @Schema(description = "Availability status of the service (active or inactive)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;

    @Schema(description = "ID of the parent service if categorizing hierarchically", example = "2")
    private Long parentServiceId;
}