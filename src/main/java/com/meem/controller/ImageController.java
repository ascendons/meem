package com.meem.controller;

import com.meem.model.dto.ImageGroupDto;
import com.meem.model.dto.ImageMetadataDto;
import com.meem.service.ImageService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
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
    public ResponseEntity<ImageMetadataDto> upload(@RequestParam("file") MultipartFile file, @RequestParam("tag") String title) {
        try {
            return ResponseEntity.ok(imageService.upload(file, title,"uploads"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

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
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ImageMetadataDto> pageResult = imageService.getPaginated(page, size);

        Map<String, List<ImageGroupDto>> grouped = pageResult.getContent().stream()
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

        Map<String, Object> response = new HashMap<>();
        response.put("data", grouped);
        response.put("currentPage", pageResult.getNumber());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("totalItems", pageResult.getTotalElements());

        return ResponseEntity.ok(response);
    }
}