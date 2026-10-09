package com.pronurse.common.controller;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.patient.repository.PatientProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Tag(name = "21. Secure File & Document Management", description = "Secure authenticated streaming and inline viewing of prescriptions, profile avatars, medical reports, and uploaded documents.")
@SecurityRequirement(name = "Bearer Authentication")
public class FileDownloadController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;

    @Value("${spring.file.upload.dir:uploads}")
    private String uploadDir;

    @Operation(
            summary = "Download/View Prescription File",
            description = """
                Downloads or inline-previews prescription file for a specific booking.
                
                **Access Control:**
                - Patient who created the booking
                - Nurse assigned to the booking
                - Admin users
                
                **Returns:** PDF or image file (inline stream)
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prescription file retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid booking number or no prescription attached"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Forbidden access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

        String filename = booking.getPrescriptionFilePath();
        Path filePath = Paths.get(uploadDir, "prescriptions", filename).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            filePath = Paths.get(uploadDir, filename).toAbsolutePath().normalize();
        }

        return streamResource(filePath, filename);
    }

    @Operation(
            summary = "Download/View Profile Image",
            description = """
                Streams profile avatar image for a user.
                
                **Access Control:** Any authenticated user can view the public avatar of an active platform user.
                
                **Returns:** Image file (JPEG, PNG, etc.)
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image retrieved successfully",
                    content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid user ID or no profile image found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Forbidden access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/profile-image/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadProfileImage(
            @Parameter(description = "User ID") @PathVariable Long userId,
            Authentication authentication) {

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("Target user not found"));

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

        Path filePath;
        if (targetUser.getRole().name().equals("NURSE")) {
            filePath = Paths.get(uploadDir, "profiles/nurses", imagePath).toAbsolutePath().normalize();
        } else {
            filePath = Paths.get(uploadDir, "profiles/patients", imagePath).toAbsolutePath().normalize();
        }

        if (!Files.exists(filePath)) {
            filePath = Paths.get(uploadDir, imagePath).toAbsolutePath().normalize();
        }

        return streamResource(filePath, imagePath);
    }

    @Operation(
            summary = "Download/View Patient Medical Report",
            description = """
                Streams and displays medical history report / prescription file for a patient.
                
                **Access Control:**
                - Patient themselves
                - Assigned nurse
                - Admin users
                
                **Returns:** PDF or image file (inline stream)
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medical report file retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid user ID or no report found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Forbidden access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/medical-report/{patientUserId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'NURSE', 'ADMIN')")
    public ResponseEntity<Resource> downloadMedicalReport(
            @Parameter(description = "Patient User ID") @PathVariable Long patientUserId,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("User not found"));

        User patientUser = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ApplicationException("Patient user not found"));

        var patientProfile = patientProfileRepository.findByUserMobile(patientUser.getMobile())
                .orElseThrow(() -> new ApplicationException("Patient profile not found"));

        if (patientProfile.getMedicalReportPath() == null) {
            throw new ApplicationException("No medical report file attached for this patient");
        }

        boolean isSelf = currentUser.getId().equals(patientUserId);
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isNurse = currentUser.getRole().name().equals("NURSE");

        if (!isSelf && !isAdmin && !isNurse) {
            throw new ApplicationException("Unauthorized access to patient medical report");
        }

        String filename = patientProfile.getMedicalReportPath();
        Path filePath = Paths.get(uploadDir, "prescriptions/patients", filename).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            filePath = Paths.get(uploadDir, "prescriptions", filename).toAbsolutePath().normalize();
            if (!Files.exists(filePath)) {
                filePath = Paths.get(uploadDir, filename).toAbsolutePath().normalize();
            }
        }

        return streamResource(filePath, filename);
    }

    @Operation(
            summary = "Download Generic File",
            description = """
                Downloads any file by filename.
                
                **Access Control:** Admin only
                
                **Security:** Validates file path to prevent directory traversal attacks
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid filename or file not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Forbidden access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Filename to download") @PathVariable String filename) {
        if (filename.contains("..")) {
            throw new ApplicationException("Invalid file path");
        }
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        return streamResource(filePath, filename);
    }

    private ResponseEntity<Resource> streamResource(Path filePath, String originalFilename) {
        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ApplicationException("File not found or not readable: " + originalFilename);
            }

            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("Streaming file: {} with content type: {}", originalFilename, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error streaming file: {}", originalFilename, e);
            throw new ApplicationException("Error retrieving file: " + e.getMessage());
        }
    }
}
