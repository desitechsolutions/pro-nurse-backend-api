package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quick booking with favorite nurse request")
public class BookWithFavoriteRequest {

    @NotNull(message = "Favorite nurse user ID is required")
    @Schema(description = "Favorite nurse user ID", example = "5")
    private Long favoriteNurseUserId;

    @NotNull(message = "Service IDs are required")
    @Schema(description = "List of service IDs", example = "[1, 2]")
    private List<Long> selectedServiceIds;

    @NotBlank(message = "Booking date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in format YYYY-MM-DD")
    @Schema(description = "Booking date", example = "2024-01-20")
    private String bookingDate;

    @NotBlank(message = "Booking time is required")
    @Schema(description = "Booking time", example = "14:30")
    private String bookingTime;

    @NotBlank(message = "Address is required")
    @Schema(description = "Service address", example = "123 Main St, Apartment 4B")
    private String address;

    @NotBlank(message = "Latitude is required")
    @Schema(description = "Address latitude", example = "28.6139")
    private String latitude;

    @NotBlank(message = "Longitude is required")
    @Schema(description = "Address longitude", example = "77.2090")
    private String longitude;

    @Schema(description = "Additional remarks", example = "Please bring necessary equipment")
    private String remarks;
}

// Made with Bob
