package com.pronurse.catalog.service;

import com.pronurse.catalog.dto.ServiceCatalogResponse;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final MedicalServiceRepository serviceRepository;

    @Override
    public List<ServiceCatalogResponse> getActiveHierarchyTree() {
        List<MedicalService> categories = serviceRepository.findByParentServiceIsNullAndIsActiveTrue();
        return categories.stream().map(this::mapToTreeResponse).collect(Collectors.toList());
    }

    private ServiceCatalogResponse mapToTreeResponse(MedicalService entity) {
        return ServiceCatalogResponse.builder()
                .serviceId(entity.getId())
                .serviceName(entity.getName())
                .description(entity.getDescription())
                .basePrice(entity.getBasePrice())
                .subServices(entity.getSubServices().stream().map(this::mapToTreeResponse).collect(Collectors.toList()))
                .build();
    }
}