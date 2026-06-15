package com.pronurse.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request parameters for searching nurses")
public class NurseSearchRequest {

    @Schema(description = "Filter by specialization", example = "Post Surgery Care")
    private String specialization;

    @Schema(description = "Minimum rating (1-5)", example = "4.0", minimum = "1", maximum = "5")
    private Double minRating;

    @Schema(description = "Patient's latitude for proximity search", example = "28.4595")
    private String latitude;

    @Schema(description = "Patient's longitude for proximity search", example = "77.0266")
    private String longitude;

    @Schema(description = "Search radius in kilometers", example = "10", defaultValue = "50")
    private Integer radiusKm = 50;

    @Schema(description = "Filter by language spoken", example = "Hindi")
    private String language;

    @Schema(description = "Filter by gender", example = "Female", allowableValues = {"Male", "Female", "Other"})
    private String gender;

    @Schema(description = "Only show nurses currently on duty", example = "true")
    private Boolean onDutyOnly = false;
}


