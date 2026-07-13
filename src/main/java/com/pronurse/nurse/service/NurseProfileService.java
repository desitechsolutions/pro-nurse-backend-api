package com.pronurse.nurse.service;

import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.nurse.dto.NurseProfileResponse; // DTO Reference Added
import org.springframework.web.multipart.MultipartFile;

public interface NurseProfileService {
    void updateProfile(String mobile, NurseProfileUpdateRequest request, MultipartFile profileImage);
    NurseProfileResponse getProfileByMobile(String mobile); // Updated return context
    Object getAdminProfileByMobile(String mobile);
    void updateLiveCoordinates(String mobile, String latitude, String longitude);
}