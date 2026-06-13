package com.pronurse.nurse.service;

import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.util.LocalFileStorageServiceUtil;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NurseProfileServiceImpl implements NurseProfileService {

    private static final Logger logger = LoggerFactory.getLogger(NurseProfileServiceImpl.class);

    private final NurseProfileRepository nurseProfileRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageServiceUtil fileStorageUtil;

    public NurseProfileServiceImpl(NurseProfileRepository nurseProfileRepository,
                                   UserRepository userRepository,
                                   LocalFileStorageServiceUtil fileStorageUtil) {
        this.nurseProfileRepository = nurseProfileRepository;
        this.userRepository = userRepository;
        this.fileStorageUtil = fileStorageUtil;
    }

    @Override
    @Transactional
    public void updateProfile(String mobile, NurseProfileUpdateRequest request, MultipartFile profileImage) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found with mobile: " + mobile));

        // Update core User entity display name alongside domain profile record
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        NurseProfile profile = nurseProfileRepository.findByUserMobile(mobile)
                .orElseGet(() -> {
                    NurseProfile newProfile = new NurseProfile();
                    newProfile.setUser(user);
                    newProfile.setNurseId(request.getNurseId());
                    return newProfile;
                });

        // Map request payload data directly to entity fields
        profile.setGender(request.getGender());
        profile.setDob(request.getDob());
        profile.setAddress(request.getAddress());
        profile.setQualification(request.getQualification());
        profile.setExperience(request.getExperience());
        profile.setSpecialization(request.getSpecialization());
        profile.setLanguages(request.getLanguages());

        // Safe coordinates parsing
        try {
            if (request.getLatitude() != null && !request.getLatitude().isBlank()) {
                profile.setLatitude(Double.parseDouble(request.getLatitude()));
            }
            if (request.getLongitude() != null && !request.getLongitude().isBlank()) {
                profile.setLongitude(Double.parseDouble(request.getLongitude()));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid geographical coordinates payload provided for nurse registration mapping.");
        }

        // Process profile attachment file stream if present
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String savedFileName = fileStorageUtil.storeFile(profileImage, "profiles/nurses", user.getId());
                profile.setProfileImage(savedFileName);
            } catch (Exception e) {
                logger.error("Failed to persist uploaded nurse profile photo to storage array.", e);
                throw new ApplicationException("Profile photo storage upload crashed execution workflow.");
            }
        }

        profile.setUpdatedAt(LocalDateTime.now());
        nurseProfileRepository.save(profile);
        logger.info("Nurse profile records compiled and synchronized securely for user: {}", mobile);
    }

    @Override
    public NurseProfile getProfileByMobile(String mobile) {
        return nurseProfileRepository.findByUserMobile(mobile)
                .orElseThrow(() -> new ApplicationException("Nurse profile record could not be located."));
    }

    public Object getAdminProfileByMobile(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new RuntimeException("Admin account record missing"));

        // Map data to a clean response map or an AdminProfileResponse DTO
        return Map.of(
                "id", user.getId(),
                "name", user.getName() != null ? user.getName() : "System Administrator",
                "mobile", user.getMobile(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole(),
                "isActive", user.isActive(),
                "createdAt", user.getCreatedAt()
        );
    }
}