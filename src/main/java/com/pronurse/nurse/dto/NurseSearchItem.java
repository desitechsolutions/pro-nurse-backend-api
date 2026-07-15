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
public class NurseSearchItem {
    private Long id;
    private String name;
    private String profile_image;
    private String gender;
    private int experience;
    private String qualification;
    private String specialization;
    private List<String> languages;
    private String city;
    private double rating;
    private int total_reviews;
    private double consultation_fee;
    private boolean availability;
    private String next_available;
}
