package com.lap.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final LocalFileStorageService localFileStorageService;

    @Autowired
    public FileStorageService(LocalFileStorageService localFileStorageService) {
        this.localFileStorageService = localFileStorageService;
    }

    public String uploadFile(MultipartFile file, String fileName) {
        return localFileStorageService.uploadFile(file, fileName);
    }

    public String uploadImage(MultipartFile file, String fileName) {
        return localFileStorageService.uploadImage(file, fileName);
    }

    public String getDownloadUrl(String fileName) {
        return "/api/files/download/" + fileName;
    }

    public String getDownloadUrl(String fileName, int expirySeconds) {
        return "/api/files/download/" + fileName;
    }

    public void deleteFile(String fileName) {
        localFileStorageService.deleteFileByName(fileName);
    }

    public boolean fileExists(String fileName) {
        return localFileStorageService.fileExists(fileName);
    }

    public byte[] downloadFile(String fileName) {
        return localFileStorageService.downloadFileByName(fileName);
    }

    public List<String> listFiles() {
        return localFileStorageService.listFiles();
    }

    public Map<String, String> getFileMetadata(String fileName) {
        return localFileStorageService.getImageMetadata(fileName);
    }

    public String getStorageInfo() {
        return "Using Local Storage: " + localFileStorageService.getStorageInfo();
    }

    public String getCurrentStorageType() {
        return "Local File Storage (PVC)";
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("currentStorage", getCurrentStorageType());
        try {
            status.put("storageInfo", getStorageInfo());
            status.put("healthy", true);
        } catch (Exception e) {
            status.put("healthy", false);
            status.put("error", e.getMessage());
        }
        return status;
    }
}
