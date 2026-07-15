package com.pronurse.nurse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NurseSearchResponse {
    private boolean status;
    private String message;
    private int total;
    private int page;
    private int limit;
    private List<NurseSearchItem> data;
}
