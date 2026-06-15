package com.pronurse.catalog.service;

import com.pronurse.catalog.dto.ServiceCatalogResponse;
import java.util.List;

public interface CatalogService {
    /**
     * Extracts all top-level medical categories along with their
     * underlying active sub-services and prices recursively.
     *
     * @return List of hierarchical ServiceCatalogResponse DTOs
     */
    List<ServiceCatalogResponse> getActiveHierarchyTree();
}