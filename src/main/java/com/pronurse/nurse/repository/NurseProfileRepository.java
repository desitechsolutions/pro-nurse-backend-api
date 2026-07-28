package com.pronurse.nurse.repository;

import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.onboarding.enums.OnboardingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NurseProfileRepository extends JpaRepository<NurseProfile, Long> {
    Optional<NurseProfile> findByUserMobile(String mobile);
    Optional<NurseProfile> findByNurseId(String nurseId);

    @Query("SELECT np FROM NurseProfile np JOIN FETCH np.user u " +
           "WHERE np.isVerified = true AND np.verificationStatus = 'Approved' " +
           "AND (:specialization IS NULL OR LOWER(np.specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))) " +
           "AND (:minRating IS NULL OR np.averageRating >= :minRating) " +
           "AND (:language IS NULL OR LOWER(np.languages) LIKE LOWER(CONCAT('%', :language, '%'))) " +
           "AND (:gender IS NULL OR np.gender = :gender) " +
           "AND (:onDutyOnly = false OR np.isOnDuty = true)")
    List<NurseProfile> searchNursesRaw(
            @Param("specialization") String specialization,
            @Param("minRating") Double minRating,
            @Param("language") String language,
            @Param("gender") com.pronurse.enums.Gender gender,
            @Param("onDutyOnly") boolean onDutyOnly
    );

    @Query("SELECT np FROM NurseProfile np JOIN FETCH np.user u " +
            "WHERE np.isVerified = true AND np.verificationStatus = 'Approved' " +
            "AND (CAST(:city AS string) IS NULL OR LOWER(np.city) = LOWER(CAST(:city AS string))) " +
            "AND (:gender IS NULL OR np.gender = :gender) " +
            "AND (:rating IS NULL OR np.averageRating >= :rating) " +
            "AND (CAST(:keyword AS string) IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "    OR LOWER(np.specialization) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "    OR LOWER(np.qualification) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    List<NurseProfile> searchNursesMobile(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("gender") com.pronurse.enums.Gender gender,
            @Param("rating") Double rating
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "UPDATE NurseProfile np SET np.isOnDuty = :isOnDuty, np.updatedAt = CURRENT_TIMESTAMP WHERE np.user.id = :userId"
    )
    int updateDutyAvailability(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("isOnDuty") Boolean isOnDuty);

    org.springframework.data.domain.Page<com.pronurse.nurse.entity.NurseProfile> findByVerificationStatus(
            String verificationStatus,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Find nurse profiles by structured onboarding status (new enum-backed field).
     * Used by admin onboarding management panel.
     */
    Page<NurseProfile> findByOnboardingStatus(OnboardingStatus onboardingStatus, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "UPDATE NurseProfile np SET " +
                    "np.averageRating = ((np.averageRating * np.totalReviewsCount) + :newScore) / (np.totalReviewsCount + 1), " +
                    "np.totalReviewsCount = np.totalReviewsCount + 1, " +
                    "np.updatedAt = CURRENT_TIMESTAMP " +
                    "WHERE np.user.id = :nurseUserId"
    )
    int recalculateAndCacheNurseRating(@org.springframework.data.repository.query.Param("nurseUserId") Long nurseUserId, @org.springframework.data.repository.query.Param("newScore") double newScore);

    @Query(value = "SELECT np.*, " +
            "(6371 * acos(cos(radians(:patientLat)) * cos(radians(np.latitude)) * " +
            "cos(radians(np.longitude) - radians(:patientLon)) + " +
            "sin(radians(:patientLat)) * sin(radians(np.latitude)))) AS distance " +
            "FROM nurse_profiles np " +
            "WHERE np.is_on_duty = true " +
            "AND np.is_verified = true " +
            "AND np.verification_status = 'Approved' " +
            "HAVING distance <= :radiusKm " +
            "ORDER BY distance ASC",
            nativeQuery = true)
    List<NurseProfile> findNearbyAvailableNurses(
            @Param("patientLat") double patientLat,
            @Param("patientLon") double patientLon,
            @Param("radiusKm") double radiusKm,
            Pageable pageable
    );
}
