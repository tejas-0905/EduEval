package com.edueval.controller;

import com.edueval.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * The frontend/client follows the redirect to fetch the actual file from
     * Cloudinary.
     */
    @GetMapping("/view")
    public ResponseEntity<Void> viewFile(@RequestParam String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String resolvedUrl = fileStorageService.resolve(publicId);
        log.info("Redirecting publicId {} → {}", publicId, resolvedUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(resolvedUrl))
                .build();
    }

    @GetMapping("/{path:.+}")
    public ResponseEntity<Void> viewFileByPath(@PathVariable String path) {
        String resolvedUrl = fileStorageService.resolve(path);
        log.info("Redirecting file path {} → {}", path, resolvedUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(resolvedUrl))
                .build();
    }
}