package com.lap.service;

import com.lap.dto.ProfileDTO;
import com.lap.entity.User;
import com.lap.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private B2StorageService b2StorageService;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public ProfileDTO.ProfileResponse getUserProfile(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        return new ProfileDTO.ProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }

    public ProfileDTO.AvatarUploadResponse uploadAvatar(
        String username,
        MultipartFile file
    ) {
        // Find user
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Validate file
        validateImageFile(file);

        try {
            // Skip old avatar deletion to prevent upload failures
            // Old avatars will be cleaned up separately if needed

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename =
                "avatars/" +
                user.getId() +
                "_" +
                UUID.randomUUID().toString() +
                fileExtension;

            // Upload to B2
            String avatarUrl = b2StorageService.uploadImageWrapper(
                file,
                uniqueFilename
            );

            // Update user in database
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return new ProfileDTO.AvatarUploadResponse(
                avatarUrl,
                "Avatar erfolgreich hochgeladen"
            );
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Upload: " + e.getMessage());
        }
    }

    public String removeAvatar(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        try {
            // Skip B2 deletion to prevent errors
            // File will remain in B2 but user avatar will be removed from database

            // Update user in database
            user.setAvatarUrl(null);
            userRepository.save(user);

            return "Avatar erfolgreich entfernt";
        } catch (Exception e) {
            throw new RuntimeException(
                "Fehler beim Entfernen des Avatars: " + e.getMessage()
            );
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Keine Datei ausgewählt");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException(
                "Datei ist zu groß. Maximum 5MB erlaubt"
            );
        }

        String contentType = file.getContentType();
        if (
            contentType == null ||
            !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())
        ) {
            throw new RuntimeException(
                "Nur JPG, PNG oder WebP Dateien sind erlaubt"
            );
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
