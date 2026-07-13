package com.pronurse.catalog.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceCatalogResponse {
    private Long serviceId;
    private String serviceName;
    private String description;
    private BigDecimal basePrice;
    private List<ServiceCatalogResponse> subServices;
}