package com.meem.service;

import com.meem.model.dto.ImageGroupDto;
import com.meem.model.dto.ImageMetadataDto;
import com.meem.model.entity.ImageMetadata;
import com.meem.repository.ImageMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

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
        clearImageListCache();
        logger.info("Uploading single image with tag='{}' to folder='{}'", title, folder);
        return uploadSingleImage(file, title, folder, "Self");
    }

    public List<ImageMetadataDto> uploadImagesInternally(List<MultipartFile> files, String title, String folder, String type) throws IOException {
        clearImageListCache();
        logger.info("Uploading {} images internally with tag='{}', type='{}'", files.size(), title, type);

        List<ImageMetadataDto> result = new ArrayList<>();
        for (MultipartFile file : files) {
            result.add(uploadSingleImage(file, title, folder, type));
        }
        return result;
    }

    private ImageMetadataDto uploadSingleImage(MultipartFile file, String title, String folder, String type) throws IOException {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String uniqueFileName = UUID.randomUUID() + "-" + sanitizedFileName;
        String key = folder + "/" + uniqueFileName;

        logger.debug("Uploading file '{}' as '{}' to S3 key='{}'", originalFileName, sanitizedFileName, key);
        uploadToS3(file, key);
        logger.info("File uploaded to S3 with key='{}'", key);

        String url = cdnBaseUrl + "/" + key;
        logger.debug("Generated CDN URL: {}", url);

        ImageMetadata metadata = saveImageMetadata(sanitizedFileName, type, title, url, file);
        logger.info("Saved image metadata for '{}'", sanitizedFileName);

        return toDto(metadata, uniqueFileName);
    }

    private void uploadToS3(MultipartFile file, String key) throws IOException {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            logger.error("Error uploading file to S3 (key='{}'): {}", key, e.getMessage(), e);
            throw e;
        }
    }

    private ImageMetadata saveImageMetadata(String fileName, String type, String tag, String url, MultipartFile file) {
        ImageMetadata meta = new ImageMetadata();
        meta.setFileName(fileName);
        meta.setImageType(type);
        meta.setImageTag(tag);
        meta.setUrl(url);
        meta.setContentType(file.getContentType());
        meta.setSize(file.getSize());
        meta.setUploadedAt(LocalDateTime.now());

        ImageMetadata saved = repository.save(meta);
        logger.debug("Image metadata saved with ID: {}", saved.getId());
        return saved;
    }

    private ImageMetadataDto toDto(ImageMetadata meta, String storedFileName) {
        return new ImageMetadataDto(
                storedFileName,
                meta.getImageType(),
                meta.getImageTag(),
                meta.getUrl(),
                meta.getContentType(),
                meta.getSize(),
                meta.getUploadedAt()
        );
    }

    private String sanitizeFileName(String originalFileName) {
        return Paths.get(originalFileName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    @Cacheable(value = "imageList", key = "#page + '-' + #size")
    public Page<ImageMetadataDto> getPaginated(int page, int size) {
        logger.info("Fetching paginated images: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        return repository.findAll(pageable).map(meta -> toDto(meta, meta.getFileName()));
    }

    @Cacheable(value = "groupedImages", key = "#page + '-' + #size")
    public Map<String, Object> getGroupedPaginated(int page, int size) {
        logger.info("Fetching grouped paginated images: page={}, size={}", page, size);
        Page<ImageMetadataDto> pageResult = getPaginated(page, size);

        Map<String, List<ImageGroupDto>> grouped = pageResult.getContent().stream()
                .collect(Collectors.groupingBy(
                        img -> Optional.ofNullable(img.getImageType()).filter(s -> !s.trim().isEmpty()).orElse("Others"),
                        Collectors.mapping(
                                img -> new ImageGroupDto(img.getImageTag(), img.getUrl()),
                                Collectors.toList()
                        )
                ));

        logger.debug("Grouped {} image types from page {}", grouped.size(), page);
        return Map.of(
                "data", grouped,
                "currentPage", pageResult.getNumber(),
                "totalPages", pageResult.getTotalPages(),
                "totalItems", pageResult.getTotalElements()
        );
    }

    @CacheEvict(value = "imageList", allEntries = true)
    public void clearImageListCache() {
        logger.info("Evicting all entries from 'imageList' cache");
    }
}