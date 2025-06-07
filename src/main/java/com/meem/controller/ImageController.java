package com.meem.controller;

import com.meem.model.dto.ImageMetadataDto;
import com.meem.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    private static final String DEFAULT_FOLDER = "uploads";

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSingleImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tag") String title) {
        logger.info("Uploading single image with tag: {}", title);
        return handleImageUpload(() -> imageService.upload(file, title, DEFAULT_FOLDER), "single image");
    }

    @PostMapping("/uploadImagesInternally")
    public ResponseEntity<?> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("tag") String title,
            @RequestParam("type") String type) {
        logger.info("Uploading multiple images with tag: {}, type: {}", title, type);
        return handleImageUpload(() -> imageService.uploadImagesInternally(files, title, DEFAULT_FOLDER, type), "multiple images");
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getPaginatedImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("Fetching paginated images: page={}, size={}", page, size);
        Page<ImageMetadataDto> pagedResult = imageService.getPaginated(page, size);

        Map<String, Object> response = Map.of(
                "images", pagedResult.getContent(),
                "currentPage", pagedResult.getNumber(),
                "totalItems", pagedResult.getTotalElements(),
                "totalPages", pagedResult.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/grouped-images")
    public ResponseEntity<Map<String, Object>> getGroupedImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("Fetching grouped images: page={}, size={}", page, size);
        return ResponseEntity.ok(imageService.getGroupedPaginated(page, size));
    }

    private <T> ResponseEntity<T> handleImageUpload(ImageUploadOperation<T> operation, String description) {
        try {
            T result = operation.execute();
            logger.info("Successfully uploaded {}", description);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Failed to upload {}: {}", description, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @FunctionalInterface
    private interface ImageUploadOperation<T> {
        T execute() throws IOException;
    }
}