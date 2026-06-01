package com.pronurse.nurse.service;

import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.nurse.entity.NurseProfile;
import org.springframework.web.multipart.MultipartFile;

public interface NurseProfileService {
    void updateProfile(String mobile, NurseProfileUpdateRequest request, MultipartFile profileImage);
    NurseProfile getProfileByMobile(String mobile);

    Object getAdminProfileByMobile(String mobile);
}