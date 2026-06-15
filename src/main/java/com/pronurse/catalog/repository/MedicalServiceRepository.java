package com.pronurse.catalog.repository;

import com.pronurse.catalog.entity.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {
    List<MedicalService> findByParentServiceIsNullAndIsActiveTrue();
}