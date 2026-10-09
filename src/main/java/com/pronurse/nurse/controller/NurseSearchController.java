package com.pronurse.nurse.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.enums.Gender;
import com.pronurse.nurse.dto.NurseSearchRequest;
import com.pronurse.nurse.dto.PublicNurseProfileResponse;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nurses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "04. Nurse Discovery & Search", description = "Public nurse directory search with filtering by keyword, city, gender, rating, and availability date.")
public class NurseSearchController {

    private final NurseProfileRepository nurseProfileRepository;

    @Operation(
            summary = "Search & Filter Nurses",
            description = """
                Search verified practitioners available for patient bookings.
                
                **Supported Query Parameters:**
                - `keyword`: Search by practitioner name, qualification, or specialty
                - `city`: Filter by city (e.g. Lucknow, Delhi, Noida)
                - `gender`: Filter by gender (`MALE`, `FEMALE`, `OTHER`)
                - `rating`: Filter by minimum average review score (e.g. `4.0`, `4.5`)
                - `available_date`: Target service date (`YYYY-MM-DD`)
                - `page`: Page number (1-indexed, default: 1)
                - `limit`: Number of results per page (default: 10)
                
                **Response:**
                - Returns paginated list of verified nurse cards with experience, specialties, ratings, consultation fees, and on-duty availability.
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Nurses found successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.pronurse.nurse.dto.NurseSearchResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<com.pronurse.nurse.dto.NurseSearchResponse> searchNurses(
            @Parameter(description = "Keyword search for nurse name or specialty", example = "ICU") @RequestParam(required = false) String keyword,
            @Parameter(description = "City filter", example = "Lucknow") @RequestParam(required = false) String city,
            @Parameter(description = "Gender filter (MALE, FEMALE, OTHER)") @RequestParam(required = false) Gender gender,
            @Parameter(description = "Minimum review rating threshold (e.g. 4.0)", example = "4.0") @RequestParam(required = false) Double rating,
            @Parameter(description = "Target booking date (YYYY-MM-DD)", example = "2026-10-15") @RequestParam(name = "available_date", required = false) String availableDate,
            @Parameter(description = "Page number (1-based index)", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

        log.info("Searching nurses with filters - keyword: {}, city: {}, gender: {}, rating: {}, availableDate: {}, page: {}, limit: {}",
                keyword, city, gender, rating, availableDate, page, limit);

        List<NurseProfile> allNurses = nurseProfileRepository.searchNursesMobile(keyword, city, gender, rating);

        String targetDate = availableDate != null ? availableDate : "2026-07-20";

        List<com.pronurse.nurse.dto.NurseSearchItem> items = allNurses.stream()
                .map(np -> {
                    // Split languages
                    List<String> langList = java.util.Collections.emptyList();
                    if (np.getLanguages() != null && !np.getLanguages().trim().isEmpty()) {
                        langList = java.util.Arrays.stream(np.getLanguages().split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
                    }

                    // Experience as int (extract first number or default)
                    int expYears = 0;
                    if (np.getExperience() != null) {
                        try {
                            String numStr = np.getExperience().replaceAll("[^0-9]", "");
                            if (!numStr.isEmpty()) {
                                expYears = Integer.parseInt(numStr);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    // availability
                    boolean availability = np.isOnDuty();

                    // next available time
                    String nextAvailable = targetDate + " 10:00 AM";

                    return com.pronurse.nurse.dto.NurseSearchItem.builder()
                            .id(np.getUser().getId())
                            .name(np.getUser().getName())
                            .profile_image(np.getProfileImage() != null
                                    ? "/api/files/profile-image/" + np.getUser().getId()
                                    : null)
                            .gender(np.getGender() != null ? np.getGender().name() : null)
                            .experience(expYears)
                            .qualification(np.getQualification())
                            .specialization(np.getSpecialization())
                            .languages(langList)
                            .mobile(np.getUser().getMobile() != null ? np.getUser().getMobile() : "")
                            .city(np.getCity() != null ? np.getCity() : "Lucknow")
                            .rating(np.getAverageRating())
                            .total_reviews(np.getTotalReviewsCount())
                            .consultation_fee(np.getConsultationFee() != null ? np.getConsultationFee().doubleValue() : 0.0)
                            .availability(availability)
                            .next_available(nextAvailable)
                            .build();
                })
                .collect(Collectors.toList());

        // Apply pagination
        int total = items.size();
        int pageZeroBased = Math.max(0, page - 1);
        int start = Math.min(pageZeroBased * limit, total);
        int end = Math.min(start + limit, total);
        List<com.pronurse.nurse.dto.NurseSearchItem> paginated = items.subList(start, end);

        if (total == 0) {
            return ResponseEntity.ok(com.pronurse.nurse.dto.NurseSearchResponse.builder()
                    .status(false)
                    .message("No nurses found")
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .data(java.util.Collections.emptyList())
                    .build());
        }

        return ResponseEntity.ok(com.pronurse.nurse.dto.NurseSearchResponse.builder()
                .status(true)
                .message("Nurses found successfully")
                .total(total)
                .page(page)
                .limit(limit)
                .data(paginated)
                .build());
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * Returns distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return Math.round(distance * 10.0) / 10.0; // Round to 1 decimal place
    }
}


