package com.pronurse.nurse.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.dto.NurseSearchRequest;
import com.pronurse.nurse.dto.PublicNurseProfileResponse;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Nurse Discovery", description = "Public nurse search and discovery for patients")
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
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Nurses found successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Found 5 nurses matching criteria",
                                                      "data": {
                                                        "content": [
                                                          {
                                                            "nurseId": "NUR-001",
                                                            "name": "Jane Smith",
                                                            "gender": "Female",
                                                            "qualification": "GNM",
                                                            "experience": "5 Years",
                                                            "specialization": "Post Surgery Care",
                                                            "languages": "Hindi, English",
                                                            "profileImage": "/api/files/profile-image/1",
                                                            "averageRating": 4.5,
                                                            "totalReviews": 25,
                                                            "isOnDuty": true,
                                                            "distanceKm": 2.5,
                                                            "verificationStatus": "Approved"
                                                          }
                                                        ],
                                                        "totalElements": 5,
                                                        "totalPages": 1,
                                                        "size": 20,
                                                        "number": 0
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PublicNurseProfileResponse>>> searchNurses(
            @Parameter(description = "Specialization filter") @RequestParam(required = false) String specialization,
            @Parameter(description = "Minimum rating") @RequestParam(required = false) Double minRating,
            @Parameter(description = "Patient latitude") @RequestParam(required = false) String latitude,
            @Parameter(description = "Patient longitude") @RequestParam(required = false) String longitude,
            @Parameter(description = "Search radius in km") @RequestParam(required = false, defaultValue = "50") Integer radiusKm,
            @Parameter(description = "Language filter") @RequestParam(required = false) String language,
            @Parameter(description = "Gender filter") @RequestParam(required = false) String gender,
            @Parameter(description = "Only on-duty nurses") @RequestParam(required = false, defaultValue = "false") Boolean onDutyOnly,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Searching nurses with filters - specialization: {}, minRating: {}, location: {},{}, radius: {}km",
                specialization, minRating, latitude, longitude, radiusKm);

        // Fetch all verified nurses
        List<NurseProfile> allNurses = nurseProfileRepository.findAll().stream()
                .filter(np -> np.isVerified() && "Approved".equals(np.getVerificationStatus()))
                .collect(Collectors.toList());

        // Apply filters
        List<PublicNurseProfileResponse> filteredNurses = allNurses.stream()
                .filter(np -> specialization == null || 
                        (np.getSpecialization() != null && np.getSpecialization().toLowerCase().contains(specialization.toLowerCase())))
                .filter(np -> minRating == null || np.getAverageRating() >= minRating)
                .filter(np -> language == null || 
                        (np.getLanguages() != null && np.getLanguages().toLowerCase().contains(language.toLowerCase())))
                .filter(np -> gender == null || 
                        (np.getGender() != null && np.getGender().equalsIgnoreCase(gender)))
                .filter(np -> !onDutyOnly || np.isOnDuty())
                .map(np -> {
                    Double distance = null;
                    if (latitude != null && longitude != null && np.getLatitude() != null && np.getLongitude() != null) {
                        try {
                            double lat1 = Double.parseDouble(latitude);
                            double lon1 = Double.parseDouble(longitude);
                            distance = calculateDistance(lat1, lon1, np.getLatitude(), np.getLongitude());
                        } catch (NumberFormatException e) {
                            log.warn("Invalid coordinates provided");
                        }
                    }
                    
                    return PublicNurseProfileResponse.builder()
                            .nurseId(np.getNurseId())
                            .name(np.getUser().getName())
                            .gender(np.getGender())
                            .qualification(np.getQualification())
                            .experience(np.getExperience())
                            .specialization(np.getSpecialization())
                            .languages(np.getLanguages())
                            .profileImage(np.getProfileImage() != null ? "/api/files/profile-image/" + np.getUser().getId() : null)
                            .averageRating(np.getAverageRating())
                            .totalReviews(np.getTotalReviewsCount())
                            .isOnDuty(np.isOnDuty())
                            .distanceKm(distance)
                            .verificationStatus(np.getVerificationStatus())
                            .build();
                })
                .filter(np -> latitude == null || longitude == null || np.getDistanceKm() == null || np.getDistanceKm() <= radiusKm)
                .sorted((a, b) -> {
                    // Sort by distance if available, then by rating
                    if (a.getDistanceKm() != null && b.getDistanceKm() != null) {
                        return Double.compare(a.getDistanceKm(), b.getDistanceKm());
                    }
                    return Double.compare(b.getAverageRating(), a.getAverageRating());
                })
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredNurses.size());
        List<PublicNurseProfileResponse> pageContent = filteredNurses.subList(start, end);
        Page<PublicNurseProfileResponse> page = new PageImpl<>(pageContent, pageable, filteredNurses.size());

        log.info("Found {} nurses matching criteria", filteredNurses.size());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Found " + filteredNurses.size() + " nurses matching criteria",
                page
        ));
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


