package com.meem.service;

import com.meem.model.dto.ImageMetadataDto;
import com.meem.model.entity.ImageMetadata;
import com.meem.repository.ImageMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public ImageMetadataDto upload(MultipartFile file, String title, String folder) throws IOException {
        String originalFileName = file.getOriginalFilename();

        // 1. Sanitize the original file name
        String sanitizedFileName = sanitizeFileName(originalFileName);

        // 2. Prepend UUID to avoid collisions
        String fileName = UUID.randomUUID() + "-" + sanitizedFileName;

        // 3. Upload to S3
        String key = folder +"/" + fileName;
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
        meta.setImageType("Self");
        meta.setImageTag(title);
        meta.setUrl(url);
        meta.setContentType(file.getContentType());
        meta.setSize(file.getSize());
        meta.setUploadedAt(LocalDateTime.now());

        repository.save(meta);

        return new ImageMetadataDto(
                fileName,
                "Self",
                title,
                url,
                file.getContentType(),
                file.getSize(),
                meta.getUploadedAt()
        );
    }
    public List<ImageMetadataDto> uploadImagesInternally(List<MultipartFile> files, String title, String folder, String type) throws IOException {
        List<ImageMetadataDto> metadataList = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFileName = file.getOriginalFilename();
            String sanitizedFileName = sanitizeFileName(originalFileName);
            String fileName = UUID.randomUUID() + "-" + sanitizedFileName;
            String key = folder + "/" + fileName;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            String url = cdnBaseUrl + "/" + key;

            ImageMetadata meta = new ImageMetadata();
            meta.setFileName(sanitizedFileName);
            meta.setImageType(type);
            meta.setImageTag(title);
            meta.setUrl(url);
            meta.setContentType(file.getContentType());
            meta.setSize(file.getSize());
            meta.setUploadedAt(LocalDateTime.now());

            repository.save(meta);

            metadataList.add(new ImageMetadataDto(
                    fileName,
                    type,
                    title,
                    url,
                    file.getContentType(),
                    file.getSize(),
                    meta.getUploadedAt()
            ));
        }

        return metadataList;
    }
    private String sanitizeFileName(String originalFileName) {
        // Remove directory path if present
        originalFileName = Paths.get(originalFileName).getFileName().toString();

        // Replace all non-alphanumeric characters (except dot and hyphen) with underscores
        return originalFileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
    public Page<ImageMetadataDto> getPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<ImageMetadata> pageResult = repository.findAll(pageable);

        return pageResult.map(meta -> new ImageMetadataDto(
                meta.getFileName(),
                meta.getImageType(),
                meta.getImageTag(),
                meta.getUrl(),
                meta.getContentType(),
                meta.getSize(),
                meta.getUploadedAt()
        ));
    }
}