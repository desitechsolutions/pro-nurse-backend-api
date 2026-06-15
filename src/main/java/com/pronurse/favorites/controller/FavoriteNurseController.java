package com.pronurse.favorites.controller;

import com.pronurse.booking.dto.BookWithFavoriteRequest;
import com.pronurse.booking.service.BookingService;
import com.pronurse.common.payload.ApiResponse;
import com.pronurse.favorites.dto.AddFavoriteRequest;
import com.pronurse.favorites.dto.FavoriteNurseResponse;
import com.pronurse.favorites.service.FavoriteNurseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Favorite Nurses", description = "Manage favorite nurses for quick booking")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteNurseController {

    private final FavoriteNurseService favoriteService;
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Add nurse to favorites", 
               description = "Add a nurse to patient's favorite list for quick access")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @Valid @RequestBody AddFavoriteRequest request,
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        favoriteService.addFavorite(patientMobile, request);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Nurse added to favorites successfully")
                .build());
    }

    @DeleteMapping("/{nurseUserId}")
    @Operation(summary = "Remove nurse from favorites", 
               description = "Remove a nurse from patient's favorite list")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long nurseUserId,
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        favoriteService.removeFavorite(patientMobile, nurseUserId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Nurse removed from favorites successfully")
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all favorite nurses", 
               description = "Retrieve list of all favorite nurses with their details")
    public ResponseEntity<ApiResponse<List<FavoriteNurseResponse>>> getFavorites(
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        List<FavoriteNurseResponse> favorites = favoriteService.getFavorites(patientMobile);
        
        return ResponseEntity.ok(ApiResponse.<List<FavoriteNurseResponse>>builder()
                .success(true)
                .message("Favorites retrieved successfully")
                .data(favorites)
                .build());
    }

    @GetMapping("/check/{nurseUserId}")
    @Operation(summary = "Check if nurse is in favorites", 
               description = "Check whether a specific nurse is in patient's favorites")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(
            @PathVariable Long nurseUserId,
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        boolean isFav = favoriteService.isFavorite(patientMobile, nurseUserId);
        
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .data(isFav)
                .build());
    }

    @PutMapping("/{nurseUserId}/notes")
    @Operation(summary = "Update notes for favorite nurse", 
               description = "Update personal notes for a favorite nurse")
    public ResponseEntity<ApiResponse<Void>> updateNotes(
            @PathVariable Long nurseUserId,
            @RequestParam String notes,
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        favoriteService.updateNotes(patientMobile, nurseUserId, notes);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Notes updated successfully")
                .build());
    }

    @PostMapping("/book")
    @Operation(summary = "Quick book with favorite nurse",
               description = "Create a booking directly with a favorite nurse (no dispatch)")
    public ResponseEntity<ApiResponse<String>> bookWithFavorite(
            @Valid @RequestBody BookWithFavoriteRequest request,
            Authentication authentication) {
        
        String patientMobile = authentication.getName();
        String bookingNo = bookingService.bookWithFavoriteNurse(patientMobile, request);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Booking created successfully with your favorite nurse")
                .data(bookingNo)
                .build());
    }
}
