package com.edueval.controller;

import com.edueval.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * GET /api/files/view?publicId=edueval/submissions/...
     *
     * Resolves a Cloudinary public_id to a signed HTTPS URL and redirects.
     * The frontend/client follows the redirect to fetch the actual file from Cloudinary.
     * Requires authentication — handled by SecurityConfig.
     */
    @GetMapping("/view")
    public ResponseEntity<Void> viewFile(@RequestParam String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String cloudinaryUrl = fileStorageService.resolve(publicId);
        log.info("Redirecting publicId {} → {}", publicId, cloudinaryUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(cloudinaryUrl))
                .build();
    }
}