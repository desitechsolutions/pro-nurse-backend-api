package com.pronurse.patient.service;

import com.pronurse.patient.dto.PatientProfileResponse;
import com.pronurse.patient.dto.PatientProfileUpdateRequest;
import com.pronurse.patient.entity.PatientProfile;
import org.springframework.web.multipart.MultipartFile;

public interface PatientProfileService {
    void updateProfile(String mobile, PatientProfileUpdateRequest request, MultipartFile profileImage, MultipartFile medicalReport);
    PatientProfileResponse getProfileByMobile(String mobile);
}