package com.pronurse.nurse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveLocationPayload {
    private String bookingNo;
    private Double latitude;
    private Double longitude;
    private Double heading; // Optional: Direction the car is facing to rotate the map marker
}