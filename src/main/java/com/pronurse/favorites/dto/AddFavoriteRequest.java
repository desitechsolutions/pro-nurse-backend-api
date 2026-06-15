package com.pronurse.favorites.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add nurse to favorites request")
public class AddFavoriteRequest {

    @NotNull(message = "Nurse user ID is required")
    @Schema(description = "Nurse user ID to add to favorites", example = "5")
    private Long nurseUserId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Personal notes about this nurse", example = "Very caring and professional")
    private String notes;
}


