package com.stary.backend.api.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/images")
public class ImageController {
    private final Path root;

    public ImageController(@Value("${file.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/products/{filename:.+}")
    public ResponseEntity<Resource> serveProductImage(@PathVariable String filename) {
        return serveFile("products", filename);
    }

    @GetMapping("/profiles/{filename:.+}")
    public ResponseEntity<Resource> serveProfileImage(@PathVariable String filename) {
        return serveFile("profiles", filename);
    }

    private ResponseEntity<Resource> serveFile(String subdir, String filename) {
        try {
            Path file = root.resolve(subdir).resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if(!resource.exists()) return ResponseEntity.notFound().build();

            String contentType = Files.probeContentType(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
