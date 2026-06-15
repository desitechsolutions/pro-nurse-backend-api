package com.pronurse.favorites.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Favorite nurse details")
public class FavoriteNurseResponse {

    @Schema(description = "Favorite ID", example = "1")
    private Long id;

    @Schema(description = "Nurse user ID", example = "5")
    private Long nurseUserId;

    @Schema(description = "Nurse full name", example = "Jane Smith")
    private String nurseName;

    @Schema(description = "Nurse mobile", example = "9876543210")
    private String nurseMobile;

    @Schema(description = "Nurse specialization", example = "ICU Care")
    private String specialization;

    @Schema(description = "Nurse rating", example = "4.8")
    private Double rating;

    @Schema(description = "Total reviews", example = "45")
    private Integer totalReviews;

    @Schema(description = "Years of experience", example = "8")
    private Integer yearsOfExperience;

    @Schema(description = "Profile image URL", example = "/api/files/download/profiles/nurse_5.jpg")
    private String profileImageUrl;

    @Schema(description = "Personal notes about this nurse", example = "Very caring and professional")
    private String notes;

    @Schema(description = "When added to favorites", example = "2024-01-15T10:30:00")
    private LocalDateTime addedAt;
}


