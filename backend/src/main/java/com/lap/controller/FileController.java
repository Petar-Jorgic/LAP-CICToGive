package com.lap.controller;

import com.lap.service.FileStorageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(
        FileController.class
    );

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
        @PathVariable String fileName
    ) {
        try {
            byte[] fileContent = fileStorageService.downloadFile(fileName);
            ByteArrayResource resource = new ByteArrayResource(fileContent);

            // Determine content type from file extension
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                contentType = MediaType.IMAGE_JPEG;
            } else if (lowerName.endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG;
            } else if (lowerName.endsWith(".gif")) {
                contentType = MediaType.IMAGE_GIF;
            } else if (lowerName.endsWith(".webp")) {
                contentType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + fileName + "\""
                )
                .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file {}: {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
        @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String fileName =
                "file_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

            String uploadedFileName = fileStorageService.uploadFile(file, fileName);
            String downloadUrl = fileStorageService.getDownloadUrl(uploadedFileName);

            response.put("success", true);
            response.put("fileName", uploadedFileName);
            response.put("downloadUrl", downloadUrl);
            response.put("fileSize", file.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
        @RequestParam("files") MultipartFile[] files
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (files == null || files.length == 0) {
                response.put("success", false);
                response.put("error", "No files provided");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> uploadResults = new HashMap<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName =
                        "file_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

                    String uploadedFileName = fileStorageService.uploadFile(file, fileName);
                    String downloadUrl = fileStorageService.getDownloadUrl(uploadedFileName);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", uploadedFileName);
                    fileInfo.put("downloadUrl", downloadUrl);
                    fileInfo.put("fileSize", file.getSize());

                    uploadResults.put(file.getOriginalFilename(), fileInfo);
                }
            }

            response.put("success", true);
            response.put("files", uploadResults);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading multiple files: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/images")
    public ResponseEntity<List<String>> uploadImages(
        @RequestParam("files") MultipartFile[] files
    ) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().build();
            }

            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName =
                        "image_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

                    String uploadedFileName = fileStorageService.uploadImage(file, fileName);
                    String downloadUrl = fileStorageService.getDownloadUrl(uploadedFileName);

                    uploadedUrls.add(downloadUrl);
                }
            }

            return ResponseEntity.ok(uploadedUrls);
        } catch (Exception e) {
            logger.error("Error uploading images: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/delete/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> deleteFile(
        @PathVariable String fileName
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!fileStorageService.fileExists(fileName)) {
                response.put("success", false);
                response.put("error", "File not found: " + fileName);
                return ResponseEntity.status(404).body(response);
            }

            fileStorageService.deleteFile(fileName);
            response.put("success", true);
            response.put("message", "File deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting file {}: {}", fileName, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/exists/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> fileExists(
        @PathVariable String fileName
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean exists = fileStorageService.fileExists(fileName);
            response.put("success", true);
            response.put("exists", exists);
            response.put("fileName", fileName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking file existence {}: {}", fileName, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles(
        @RequestParam(value = "maxCount", defaultValue = "100") int maxCount
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<String> files = fileStorageService.listFiles();

            if (files.size() > maxCount) {
                files = files.subList(0, maxCount);
            }

            response.put("success", true);
            response.put("files", files);
            response.put("count", files.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error listing files: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/info/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> getFileInfo(
        @PathVariable String fileName
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!fileStorageService.fileExists(fileName)) {
                response.put("success", false);
                response.put("error", "File not found: " + fileName);
                return ResponseEntity.status(404).body(response);
            }

            Map<String, String> metadata = fileStorageService.getFileMetadata(fileName);
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("metadata", metadata);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting file info {}: {}", fileName, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> healthStatus = fileStorageService.getHealthStatus();
            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            logger.error("Error getting health status: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("healthy", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
