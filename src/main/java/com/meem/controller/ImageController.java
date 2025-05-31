package com.meem.controller;

import com.meem.model.dto.ImageGroupDto;
import com.meem.model.dto.ImageMetadataDto;
import com.meem.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageMetadataDto> upload(@RequestParam("file") MultipartFile file, @RequestParam("type") String type, @RequestParam("tag") String title) {
        try {
            return ResponseEntity.ok(imageService.upload(file, type, title));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<ImageMetadataDto>> list() {
        return ResponseEntity.ok(imageService.getAll());
    }

    @GetMapping("/grouped-images")
    public ResponseEntity<Map<String, List<ImageGroupDto>>> getGroupedImages() {
        List<ImageMetadataDto> allImages = imageService.getAll();

        Map<String, List<ImageGroupDto>> grouped = allImages.stream()
                .collect(Collectors.groupingBy(
                        img -> {
                            String type = img.getImageType();
                            return (type == null || type.trim().isEmpty()) ? "Others" : type;
                        },
                        Collectors.mapping(
                                img -> new ImageGroupDto(img.getImageTag(), img.getUrl()),
                                Collectors.toList()
                        )
                ));
        System.out.println("---------------------------------");
        System.out.println(grouped);

        return ResponseEntity.ok(grouped);
    }
}