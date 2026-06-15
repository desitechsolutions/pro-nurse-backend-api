package com.pronurse.favorites.repository;

import com.pronurse.favorites.entity.FavoriteNurse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteNurseRepository extends JpaRepository<FavoriteNurse, Long> {
    
    List<FavoriteNurse> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    
    Optional<FavoriteNurse> findByPatientIdAndNurseId(Long patientId, Long nurseId);
    
    boolean existsByPatientIdAndNurseId(Long patientId, Long nurseId);
    
    void deleteByPatientIdAndNurseId(Long patientId, Long nurseId);
    
    @Query("SELECT COUNT(f) FROM FavoriteNurse f WHERE f.patient.id = :patientId")
    long countByPatientId(@Param("patientId") Long patientId);
}


