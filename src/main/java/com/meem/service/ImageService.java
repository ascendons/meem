package com.meem.service;

import com.meem.model.dto.ImageMetadataDto;
import com.meem.model.entity.ImageMetadata;
import com.meem.model.enums.ImageType;
import com.meem.repository.ImageMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageService {

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.cdnBaseUrl}")
    private String cdnBaseUrl;

    private final S3Client s3Client;
    private final ImageMetadataRepository repository;

    public ImageService(S3Client s3Client, ImageMetadataRepository repository) {
        this.s3Client = s3Client;
        this.repository = repository;
    }

    public ImageMetadataDto upload(MultipartFile file, String type, String title) throws IOException {
        String originalFileName = file.getOriginalFilename();

        // 1. Sanitize the original file name
        String sanitizedFileName = sanitizeFileName(originalFileName);

        // 2. Prepend UUID to avoid collisions
        String fileName = UUID.randomUUID() + "-" + sanitizedFileName;

        // 3. Upload to S3
        String key = "uploads/" + fileName;
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        // 4. Direct URL – no encoding needed now
        String url = cdnBaseUrl + "/" + key;

        // 5. Save metadata
        ImageMetadata meta = new ImageMetadata();
        meta.setFileName(sanitizedFileName);
        meta.setImageType(type);
        meta.setImageTag(title);
        meta.setUrl(url);
        meta.setContentType(file.getContentType());
        meta.setSize(file.getSize());
        meta.setUploadedAt(LocalDateTime.now());

        repository.save(meta);

        return new ImageMetadataDto(
                fileName,
                type,
                title,
                url,
                file.getContentType(),
                file.getSize(),
                meta.getUploadedAt()
        );
    }
    private String sanitizeFileName(String originalFileName) {
        // Remove directory path if present
        originalFileName = Paths.get(originalFileName).getFileName().toString();

        // Replace all non-alphanumeric characters (except dot and hyphen) with underscores
        return originalFileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
    public List<ImageMetadataDto> getAll() {
        return repository.findAll().stream()
                .map(meta -> new ImageMetadataDto(
                        meta.getFileName(),
                        meta.getImageType(),
                        meta.getImageTag(),
                        meta.getUrl(),
                        meta.getContentType(),
                        meta.getSize(),
                        meta.getUploadedAt()
                ))
                .collect(Collectors.toList());
    }
}