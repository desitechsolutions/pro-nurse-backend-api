package com.pronurse.booking.repository;

import com.pronurse.booking.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {
    
    List<EmergencyContact> findByUserIdOrderByIsPrimaryDescCreatedAtDesc(Long userId);
    
    Optional<EmergencyContact> findByUserIdAndIsPrimaryTrue(Long userId);
    
    boolean existsByUserIdAndContactMobile(Long userId, String contactMobile);
}

// Made with Bob
