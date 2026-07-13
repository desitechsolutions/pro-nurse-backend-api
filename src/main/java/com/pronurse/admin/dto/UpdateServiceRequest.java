package com.pronurse.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateServiceRequest {
    @NotBlank(message = "Service item nomenclature required")
    private String name;

    private String description;

    @NotNull(message = "Base price formulation configuration required")
    private BigDecimal basePrice;

    @NotNull(message = "Estimated processing duration window required")
    private Integer estimatedDurationMinutes;

    @NotNull(message = "Active operational availability status field required")
    private Boolean isActive;
}