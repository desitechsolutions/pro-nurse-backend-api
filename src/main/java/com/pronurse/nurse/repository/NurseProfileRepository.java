package com.pronurse.nurse.repository;

import com.pronurse.nurse.entity.NurseProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NurseProfileRepository extends JpaRepository<NurseProfile, Long> {
    Optional<NurseProfile> findByUserMobile(String mobile);
    Optional<NurseProfile> findByNurseId(String nurseId);
}