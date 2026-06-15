package com.pronurse.favorites.service;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.favorites.dto.AddFavoriteRequest;
import com.pronurse.favorites.dto.FavoriteNurseResponse;
import com.pronurse.favorites.entity.FavoriteNurse;
import com.pronurse.favorites.repository.FavoriteNurseRepository;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteNurseServiceImpl implements FavoriteNurseService {

    private final FavoriteNurseRepository favoriteRepository;
    private final UserRepository userRepository;
    private final NurseProfileRepository nurseProfileRepository;

    @Override
    @Transactional
    public void addFavorite(String patientMobile, AddFavoriteRequest request) {
        User patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        User nurse = userRepository.findById(request.getNurseUserId())
                .orElseThrow(() -> new ApplicationException("Nurse not found"));

        // Verify nurse has a profile
        nurseProfileRepository.findByUserMobile(nurse.getMobile())
                .orElseThrow(() -> new ApplicationException("Nurse profile not found"));

        if (favoriteRepository.existsByPatientIdAndNurseId(patient.getId(), nurse.getId())) {
            throw new ApplicationException("Nurse already in favorites");
        }

        FavoriteNurse favorite = new FavoriteNurse();
        favorite.setPatient(patient);
        favorite.setNurse(nurse);
        favorite.setNotes(request.getNotes());

        favoriteRepository.save(favorite);
        log.info("Patient {} added nurse {} to favorites", patientMobile, nurse.getMobile());
    }

    @Override
    @Transactional
    public void removeFavorite(String patientMobile, Long nurseUserId) {
        User patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        if (!favoriteRepository.existsByPatientIdAndNurseId(patient.getId(), nurseUserId)) {
            throw new ApplicationException("Nurse not in favorites");
        }

        favoriteRepository.deleteByPatientIdAndNurseId(patient.getId(), nurseUserId);
        log.info("Patient {} removed nurse {} from favorites", patientMobile, nurseUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteNurseResponse> getFavorites(String patientMobile) {
        User patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        List<FavoriteNurse> favorites = favoriteRepository
                .findByPatientIdOrderByCreatedAtDesc(patient.getId());

        return favorites.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(String patientMobile, Long nurseUserId) {
        User patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        return favoriteRepository.existsByPatientIdAndNurseId(patient.getId(), nurseUserId);
    }

    @Override
    @Transactional
    public void updateNotes(String patientMobile, Long nurseUserId, String notes) {
        User patient = userRepository.findByMobile(patientMobile)
                .orElseThrow(() -> new ApplicationException("Patient not found"));

        FavoriteNurse favorite = favoriteRepository.findByPatientIdAndNurseId(patient.getId(), nurseUserId)
                .orElseThrow(() -> new ApplicationException("Nurse not in favorites"));

        favorite.setNotes(notes);
        favoriteRepository.save(favorite);
        log.info("Patient {} updated notes for favorite nurse {}", patientMobile, nurseUserId);
    }

    private FavoriteNurseResponse mapToResponse(FavoriteNurse favorite) {
        User nurse = favorite.getNurse();
        NurseProfile profile = nurseProfileRepository.findByUserMobile(nurse.getMobile())
                .orElse(null);

        return FavoriteNurseResponse.builder()
                .id(favorite.getId())
                .nurseUserId(nurse.getId())
                .nurseName(nurse.getName())
                .nurseMobile(nurse.getMobile())
                .specialization(profile != null ? profile.getSpecialization() : null)
                .rating(profile != null ? profile.getAverageRating() : null)
                .totalReviews(profile != null ? profile.getTotalReviewsCount() : 0)
                .yearsOfExperience(profile != null && profile.getExperience() != null
                        ? parseExperienceYears(profile.getExperience()) : null)
                .profileImageUrl(profile != null && profile.getProfileImage() != null
                        ? "/api/files/download/" + profile.getProfileImage() : null)
                .notes(favorite.getNotes())
                .addedAt(favorite.getCreatedAt())
                .build();
    }

    private Integer parseExperienceYears(String experience) {
        if (experience == null || experience.trim().isEmpty()) {
            return null;
        }
        try {
            // Extract numeric value from strings like "5 Years", "3", "10 years"
            String numericPart = experience.replaceAll("[^0-9]", "");
            return numericPart.isEmpty() ? null : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

// Made with Bob
