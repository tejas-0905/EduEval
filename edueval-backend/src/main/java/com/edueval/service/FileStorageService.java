package com.edueval.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp");

    private final Cloudinary cloudinary;
    private final boolean cloudinaryEnabled;

    public FileStorageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinaryEnabled = cloudName != null && !cloudName.trim().isEmpty();
        if (this.cloudinaryEnabled) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret));
        } else {
            this.cloudinary = null;
        }
    }

    /**
     * Uploads a file to Cloudinary and returns the secure URL.
     * For local development without Cloudinary, returns a placeholder URL.
     */
    public String store(MultipartFile file, String subDirectory) {
        validateFile(file);
        if (!cloudinaryEnabled) {
            // Local development fallback: return a placeholder URL
            return "file://local/" + subDirectory + "/" + file.getOriginalFilename();
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "edueval/" + subDirectory,
                    "resource_type", "auto"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload file to Cloudinary", e);
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "auto"));
        } catch (IOException e) {
            System.err.println("Warning: could not delete from Cloudinary: " + publicId);
        }
    }

    /**
     * Returns a secure URL for the given public id or passes through absolute URLs.
     */
    public String resolve(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return cloudinary.url()
                .resourceType("raw")
                .secure(true)
                .generate(value);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType +
                            ". Allowed: PDF, JPEG, PNG, WEBP");
        }
    }
}