package com.pronurse.patient.service;

import com.pronurse.patient.dto.PatientProfileUpdateRequest;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.util.LocalFileStorageServiceUtil;
import com.pronurse.patient.entity.PatientProfile;
import com.pronurse.patient.repository.PatientProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class PatientProfileServiceImpl implements PatientProfileService {

    private static final Logger logger = LoggerFactory.getLogger(PatientProfileServiceImpl.class);

    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageServiceUtil fileStorageUtil;

    public PatientProfileServiceImpl(PatientProfileRepository patientProfileRepository,
                                     UserRepository userRepository,
                                     LocalFileStorageServiceUtil fileStorageUtil) {
        this.patientProfileRepository = patientProfileRepository;
        this.userRepository = userRepository;
        this.fileStorageUtil = fileStorageUtil;
    }

    @Override
    @Transactional
    public void updateProfile(String mobile, PatientProfileUpdateRequest request, MultipartFile profileImage) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User record connection dropped for identity context: " + mobile));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        PatientProfile profile = patientProfileRepository.findByUserMobile(mobile)
                .orElseGet(() -> {
                    PatientProfile newProfile = new PatientProfile();
                    newProfile.setUser(user);
                    newProfile.setPatientId(request.getPatientId());
                    return newProfile;
                });

        profile.setGender(request.getGender());
        profile.setDob(request.getDob());
        profile.setBloodGroup(request.getBloodGroup());
        profile.setAddress(request.getAddress());
        try {
            if (request.getLatitude() != null && !request.getLatitude().isBlank()) {
                profile.setLatitude(Double.parseDouble(request.getLatitude()));
            }
            if (request.getLongitude() != null && !request.getLongitude().isBlank()) {
                profile.setLongitude(Double.parseDouble(request.getLongitude()));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid geographical coordinates payload provided for patient profile mapping.");
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String savedFileName = fileStorageUtil.storeFile(profileImage, "profiles/patients", user.getId());
                profile.setProfileImage(savedFileName);
            } catch (Exception e) {
                logger.error("Failed to commit uploaded patient picture data to disk.", e);
                throw new ApplicationException("Patient image asset upload execution mapping error.");
            }
        }

        profile.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(profile);
        logger.info("Patient demographic details updated securely in database core for mobile: {}", mobile);
    }

    @Override
    public PatientProfile getProfileByMobile(String mobile) {
        return patientProfileRepository.findByUserMobile(mobile)
                .orElseThrow(() -> new ApplicationException("Patient clinical file parameters not initialized yet."));
    }
}