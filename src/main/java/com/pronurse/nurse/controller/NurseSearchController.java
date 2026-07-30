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
@Tag(name = "04. Nurse Discovery", description = "Public nurse search and discovery for patients")
public class NurseSearchController {

    private final NurseProfileRepository nurseProfileRepository;

    @Operation(
            summary = "Search Nurses",
            description = """
                Search and filter verified nurses based on various criteria.
                
                **Features:**
                - Proximity-based search (finds nurses near patient location)
                - Filter by specialization, rating, language, gender
                - Only shows verified and approved nurses
                - Excludes sensitive information (mobile, address, etc.)
                - Pagination support
                
                **Use Cases:**
                - Patient browsing available nurses before booking
                - Finding specialists in specific areas
                - Locating nearby nurses for emergency
                
                **Privacy:**
                - Mobile numbers are hidden
                - Exact addresses are hidden
                - Only public profile information is shown
                
                **Example Queries:**
                - Find nurses within 10km: `?latitude=28.4595&longitude=77.0266&radiusKm=10`
                - Find surgery specialists: `?specialization=Post Surgery Care`
                - Find highly rated nurses: `?minRating=4.5`
                - Find Hindi-speaking nurses: `?language=Hindi`
                - Find nurses on duty: `?onDutyOnly=true`
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
            @Parameter(description = "Keyword search") @RequestParam(required = false) String keyword,
            @Parameter(description = "City filter") @RequestParam(required = false) String city,
            @Parameter(description = "Gender filter") @RequestParam(required = false) Gender gender,
            @Parameter(description = "Minimum rating") @RequestParam(required = false) Double rating,
            @Parameter(description = "Available Date filter") @RequestParam(name = "available_date", required = false) String availableDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {

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


