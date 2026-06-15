package com.pronurse.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewModerationRequest {
    @NotNull(message = "Review target tracker identifier required")
    private Long reviewId;

    @NotBlank(message = "Target operational action parameter required (APPROVE/DELETE)")
    private String action; // 'APPROVE' or 'DELETE'
}