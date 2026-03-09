package com.lap.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(
        LocalFileStorageService.class
    );

    @Value("${file.storage.path:./uploads}")
    private String storagePath;

    /**
     * Upload a file to local storage
     */
    public String uploadFile(MultipartFile file, String fileName) {
        try {
            // Create storage directory if it doesn't exist
            Path uploadDir = Paths.get(storagePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename if not provided
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = generateFileName(file.getOriginalFilename());
            }

            // Save file to local storage (create subdirectories if needed)
            Path filePath = uploadDir.resolve(fileName);
            Files.createDirectories(filePath.getParent());
            Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
            );

            logger.info("File uploaded successfully: {}", fileName);
            return fileName;
        } catch (IOException e) {
            logger.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * Upload file with metadata
     */
    public String uploadFileWithMetadata(
        MultipartFile file,
        String fileName,
        Map<String, String> metadata
    ) {
        // For local storage, we just upload the file
        // Metadata could be stored in a separate JSON file if needed
        return uploadFile(file, fileName);
    }

    /**
     * Download file by name
     */
    public byte[] downloadFileByName(String fileName) {
        try {
            Path filePath = Paths.get(storagePath).resolve(fileName);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + fileName);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            logger.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("File download failed", e);
        }
    }

    /**
     * Delete file by name
     */
    public void deleteFileByName(String fileName) {
        try {
            Path filePath = Paths.get(storagePath).resolve(fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("File deleted successfully: {}", fileName);
            } else {
                logger.warn("File not found for deletion: {}", fileName);
            }
        } catch (IOException e) {
            logger.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("File deletion failed", e);
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileName) {
        Path filePath = Paths.get(storagePath).resolve(fileName);
        return Files.exists(filePath);
    }

    /**
     * List files in storage
     */
    public List<String> listFiles() {
        try {
            Path uploadDir = Paths.get(storagePath);
            if (!Files.exists(uploadDir)) {
                return new ArrayList<>();
            }

            return Files.list(uploadDir)
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .toList();
        } catch (IOException e) {
            logger.error("Error listing files: {}", e.getMessage());
            throw new RuntimeException("File listing failed", e);
        }
    }

    /**
     * Get file size
     */
    public long getFileSize(String fileName) {
        try {
            Path filePath = Paths.get(storagePath).resolve(fileName);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + fileName);
            }
            return Files.size(filePath);
        } catch (IOException e) {
            logger.error("Error getting file size: {}", e.getMessage());
            throw new RuntimeException("Failed to get file size", e);
        }
    }

    /**
     * Upload image with validation
     */
    public String uploadImage(MultipartFile file, String fileName) {
        if (!isImageFile(file)) {
            throw new IllegalArgumentException("File is not a valid image");
        }
        return uploadFile(file, fileName);
    }

    /**
     * Upload image with dimensions metadata
     */
    public String uploadImageWithDimensions(
        MultipartFile file,
        String fileName,
        Integer width,
        Integer height
    ) {
        if (!isImageFile(file)) {
            throw new IllegalArgumentException("File is not a valid image");
        }

        Map<String, String> metadata = new HashMap<>();
        if (width != null) metadata.put("width", width.toString());
        if (height != null) metadata.put("height", height.toString());

        return uploadFileWithMetadata(file, fileName, metadata);
    }

    /**
     * Update existing image
     */
    public String updateImage(
        String oldFileName,
        MultipartFile file,
        String newFileName
    ) {
        // Delete old image if it exists
        if (fileExists(oldFileName)) {
            deleteFileByName(oldFileName);
        }

        // Upload new image
        return uploadImage(file, newFileName);
    }

    /**
     * Get download URL for file
     */
    public String getDownloadUrl(String fileName) {
        // For local storage, return a relative URL
        return "/api/files/download/" + fileName;
    }

    /**
     * Get image metadata
     */
    public Map<String, String> getImageMetadata(String fileName) {
        Map<String, String> metadata = new HashMap<>();
        if (fileExists(fileName)) {
            try {
                Path filePath = Paths.get(storagePath).resolve(fileName);
                metadata.put("fileName", fileName);
                metadata.put("fileSize", String.valueOf(Files.size(filePath)));
                metadata.put("lastModified", Files.getLastModifiedTime(filePath).toString());

                // Try to determine content type
                String contentType = Files.probeContentType(filePath);
                if (contentType != null) {
                    metadata.put("contentType", contentType);
                }
            } catch (IOException e) {
                logger.error("Error getting file metadata: {}", e.getMessage());
            }
        }
        return metadata;
    }

    /**
     * Generate unique filename
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(
                originalFilename.lastIndexOf(".")
            );
        }
        return "file_" + UUID.randomUUID().toString() + extension;
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Get storage info
     */
    public String getStorageInfo() {
        try {
            Path uploadDir = Paths.get(storagePath);
            if (!Files.exists(uploadDir)) {
                return "Storage directory not created yet: " + storagePath;
            }

            long fileCount = Files.list(uploadDir)
                .filter(Files::isRegularFile)
                .count();

            return String.format(
                "Local Storage: %s (%d files)",
                storagePath,
                fileCount
            );
        } catch (IOException e) {
            logger.error("Error getting storage info: {}", e.getMessage());
            return "Error getting storage info: " + e.getMessage();
        }
    }
}
