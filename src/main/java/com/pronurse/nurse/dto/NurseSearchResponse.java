package com.pronurse.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response wrapper for nurse search queries")
public class NurseSearchResponse {
    @Schema(description = "Success status flag", example = "true")
    private boolean status;

    @Schema(description = "Response message", example = "Nurses found successfully")
    private String message;

    @Schema(description = "Total number of nurses matching search criteria", example = "10")
    private int total;

    @Schema(description = "Current page number", example = "1")
    private int page;

    @Schema(description = "Limit count per page", example = "10")
    private int limit;

    @Schema(description = "List of nurse search items matching the query")
    private List<NurseSearchItem> data;
}
