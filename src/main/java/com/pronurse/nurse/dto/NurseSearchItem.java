package com.pronurse.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representation of a nurse item in search/discovery results")
public class NurseSearchItem {
    @Schema(description = "User database ID of the nurse", example = "1")
    private Long id;

    @Schema(description = "Name of the nurse", example = "Jane Doe")
    private String name;

    @Schema(description = "URL path to nurse profile image", example = "/api/files/profile-image/1")
    private String profile_image;

    @Schema(description = "Gender of the nurse", example = "FEMALE")
    private String gender;

    @Schema(description = "Total experience in years", example = "5")
    private int experience;

    @Schema(description = "Qualification details", example = "B.Sc. Nursing")
    private String qualification;

    @Schema(description = "Specialization details", example = "Critical Care")
    private String specialization;

    @Schema(description = "List of languages spoken")
    private List<String> languages;

    @Schema(description = "City where the nurse operates", example = "Lucknow")
    private String city;

    @Schema(description = "Average rating", example = "4.8")
    private double rating;

    @Schema(description = "Total reviews received", example = "25")
    private int total_reviews;

    @Schema(description = "Base consultation fee", example = "500.0")
    private double consultation_fee;

    @Schema(description = "Active availability status", example = "true")
    private boolean availability;

    @Schema(description = "Next available time slot representation", example = "2026-07-20 10:00 AM")
    private String next_available;
}
