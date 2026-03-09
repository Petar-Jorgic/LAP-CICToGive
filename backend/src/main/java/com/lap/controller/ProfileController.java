package com.lap.controller;

import com.lap.dto.ProfileDTO;
import com.lap.service.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(
        ProfileController.class
    );

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getProfile() {
        try {
            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
            logger.debug("Profile request - Authentication: " + authentication);
            logger.debug(
                "Profile request - Principal: " + authentication.getName()
            );

            String username = authentication.getName();

            ProfileDTO.ProfileResponse profile = profileService.getUserProfile(
                username
            );
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error getting profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(
                    "Fehler beim Laden des Profils: " + e.getMessage()
                )
            );
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
        @RequestParam("avatar") MultipartFile file
    ) {
        try {
            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
            logger.debug(
                "Avatar upload request - Authentication: " + authentication
            );
            logger.debug(
                "Avatar upload request - Principal: " + authentication.getName()
            );
            logger.debug(
                "Avatar upload request - File: " + file.getOriginalFilename()
            );

            String username = authentication.getName();

            ProfileDTO.AvatarUploadResponse response =
                profileService.uploadAvatar(username, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading avatar", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(e.getMessage())
            );
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<?> removeAvatar() {
        try {
            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            String message = profileService.removeAvatar(username);
            return ResponseEntity.ok(new SuccessResponse(message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(e.getMessage())
            );
        }
    }

    // Inner classes for responses
    public static class ErrorResponse {

        private String message;
        private String error;

        public ErrorResponse(String message) {
            this.message = message;
            this.error = "error";
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static class SuccessResponse {

        private String message;
        private String status;

        public SuccessResponse(String message) {
            this.message = message;
            this.status = "success";
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
