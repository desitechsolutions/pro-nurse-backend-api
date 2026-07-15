package com.pronurse.nurse.dto;

import com.pronurse.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Public nurse profile information (sensitive data excluded)")
public class PublicNurseProfileResponse {

    @Schema(description = "Nurse ID", example = "NUR-001")
    private String nurseId;

    @Schema(description = "Nurse name", example = "Jane Smith")
    private String name;

    @Schema(description = "Gender", example = "Female")
    private Gender gender;

    @Schema(description = "Qualification", example = "GNM")
    private String qualification;

    @Schema(description = "Years of experience", example = "5 Years")
    private String experience;

    @Schema(description = "Specialization", example = "Post Surgery Care")
    private String specialization;

    @Schema(description = "Languages spoken", example = "Hindi, English")
    private String languages;

    @Schema(description = "Profile image URL", example = "/api/files/profile-image/1")
    private String profileImage;

    @Schema(description = "Average rating (1-5)", example = "4.5")
    private Double averageRating;

    @Schema(description = "Total number of reviews", example = "25")
    private Integer totalReviews;

    @Schema(description = "Currently on duty", example = "true")
    private Boolean isOnDuty;

    @Schema(description = "Distance from search location in km", example = "2.5")
    private Double distanceKm;

    @Schema(description = "User unique identifier (numeric)", example = "12")
    private Long id;

    @Schema(description = "Distance in text format compatible with Flutter model", example = "2.5 km")
    private String distance;

    @Schema(description = "Estimated arrival time", example = "10 mins")
    private String time;

    @Schema(description = "Rating compatible with Flutter model", example = "4.5")
    private Double rating;

    @Schema(description = "Verification status", example = "Approved")
    private String verificationStatus;
}


