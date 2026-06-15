package com.pronurse.favorites.service;

import com.pronurse.favorites.dto.AddFavoriteRequest;
import com.pronurse.favorites.dto.FavoriteNurseResponse;

import java.util.List;

public interface FavoriteNurseService {
    
    /**
     * Add a nurse to patient's favorites
     */
    void addFavorite(String patientMobile, AddFavoriteRequest request);
    
    /**
     * Remove a nurse from patient's favorites
     */
    void removeFavorite(String patientMobile, Long nurseUserId);
    
    /**
     * Get all favorite nurses for a patient
     */
    List<FavoriteNurseResponse> getFavorites(String patientMobile);
    
    /**
     * Check if a nurse is in patient's favorites
     */
    boolean isFavorite(String patientMobile, Long nurseUserId);
    
    /**
     * Update notes for a favorite nurse
     */
    void updateNotes(String patientMobile, Long nurseUserId, String notes);
}

// Made with Bob
