package com.pronurse.common.controller;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.patient.repository.PatientProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "File upload and download operations")
@SecurityRequirement(name = "Bearer Authentication")
public class FileDownloadController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private static final String UPLOAD_DIR = "uploads/";

    @Operation(
            summary = "Download Prescription File",
            description = """
                Downloads prescription file for a specific booking.
                
                **Access Control:**
                - Patient who created the booking
                - Nurse assigned to the booking
                - Admin users
                
                **Returns:** PDF or image file
                """
    )
    @GetMapping("/prescription/{bookingNo}")
    @PreAuthorize("hasAnyRole('PATIENT', 'NURSE', 'ADMIN')")
    public ResponseEntity<Resource> downloadPrescription(
            @Parameter(description = "Booking number") @PathVariable String bookingNo,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Booking not found"));

        // Access control: only patient, assigned nurse, or admin can download
        boolean isPatient = booking.getPatientUser().getId().equals(user.getId());
        boolean isAssignedNurse = booking.getAssignedNurseUser() != null &&
                booking.getAssignedNurseUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isPatient && !isAssignedNurse && !isAdmin) {
            throw new ApplicationException("Unauthorized access to prescription file");
        }

        if (booking.getPrescriptionFilePath() == null) {
            throw new ApplicationException("No prescription file attached to this booking");
        }

        return downloadFile(booking.getPrescriptionFilePath());
    }

    @Operation(
            summary = "Download Profile Image",
            description = """
                Downloads profile image for a user.
                
                **Access Control:**
                - Own profile image (any authenticated user)
                - Admin can download any profile image
                
                **Returns:** Image file (JPEG, PNG, etc.)
                """
    )
    @GetMapping("/profile-image/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadProfileImage(
            @Parameter(description = "User ID") @PathVariable Long userId,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("Target user not found"));

        // Access control: own profile or admin
        boolean isOwnProfile = currentUser.getId().equals(userId);
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        if (!isOwnProfile && !isAdmin) {
            throw new ApplicationException("Unauthorized access to profile image");
        }

        String imagePath = null;
        if (targetUser.getRole().name().equals("NURSE") && targetUser.getNurseProfile() != null) {
            imagePath = targetUser.getNurseProfile().getProfileImage();
        } else if (targetUser.getRole().name().equals("PATIENT")) {
            var patientProfile = patientProfileRepository.findByUserMobile(targetUser.getMobile());
            if (patientProfile.isPresent()) {
                imagePath = patientProfile.get().getProfileImage();
            }
        }

        if (imagePath == null) {
            throw new ApplicationException("No profile image found");
        }

        return downloadFile(imagePath);
    }

    @Operation(
            summary = "Download Generic File",
            description = """
                Downloads any file by filename.
                
                **Access Control:** Admin only
                
                **Security:** Validates file path to prevent directory traversal attacks
                """
    )
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Filename to download") @PathVariable String filename) {
        try {
            // Prevent directory traversal attacks
            if (filename.contains("..")) {
                throw new ApplicationException("Invalid file path");
            }

            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ApplicationException("File not found or not readable: " + filename);
            }

            // Determine content type
            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("Downloading file: {} with content type: {}", filename, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading file: {}", filename, e);
            throw new ApplicationException("Error downloading file: " + e.getMessage());
        }
    }
}

// Made with Bob
