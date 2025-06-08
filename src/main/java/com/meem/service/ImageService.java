package com.meem.service;

import com.meem.model.dto.ImageGroupDto;
import com.meem.model.dto.ImageMetadataDto;
import com.meem.model.entity.ImageMetadata;
import com.meem.repository.ImageMetadataRepository;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
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
    private final MongoTemplate mongoTemplate;

    public ImageService(S3Client s3Client, ImageMetadataRepository repository, MongoTemplate mongoTemplate) {
        this.s3Client = s3Client;
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public ImageMetadataDto upload(MultipartFile file, String title, String folder) throws IOException {
        clearImageListCache();
        logger.info("Uploading single image with tag='{}' to folder='{}'", title, folder);
        return uploadSingleImage(file, title, folder, "Self");
    }

    public List<ImageMetadataDto> uploadImagesInternally(List<MultipartFile> files, String title, String folder, String type) throws IOException {
        clearImageListCache();
        clearGroupedImageCache();
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

    @Cacheable(value = "groupedImages", key = "'groupedImages-' + #page + '-' + #size")
    public Map<String, Object> getGroupedPaginatedImages(int page, int size) {
        logger.info("Fetching grouped paginated images via aggregation: page={}, size={}", page, size);

        // Add a projection to normalize null or blank imageType
        ProjectionOperation projectWithDefaultType = Aggregation.project("imageTag", "url", "imageType")
                .and(
                        ConditionalOperators.ifNull("imageType").then("Others")
                ).as("imageType");

        // Group by imageType and collect imageTag + url
        GroupOperation groupByImageType = Aggregation.group("imageType")
                .push(
                        new BasicDBObject("imageTag", "$imageTag")
                                .append("url", "$url")
                ).as("images");

        SortOperation sortByType = Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"));

        SkipOperation skip = Aggregation.skip((long) page * size);
        LimitOperation limit = Aggregation.limit(size);

        Aggregation aggregation = Aggregation.newAggregation(
                projectWithDefaultType,
                groupByImageType,
                sortByType,
                skip,
                limit
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "images", Document.class);
        List<Document> documents = results.getMappedResults();

        Map<String, List<ImageGroupDto>> groupedData = new LinkedHashMap<>();
        for (Document doc : documents) {
            String imageType = doc.getString("_id");
            List<Document> images = (List<Document>) doc.get("images");

            List<ImageGroupDto> imageGroup = images.stream()
                    .map(img -> new ImageGroupDto(img.getString("imageTag"), img.getString("url")))
                    .collect(Collectors.toList());

            groupedData.put(imageType, imageGroup);
        }

        // Efficient count aggregation
        Aggregation countAggregation = Aggregation.newAggregation(
                projectWithDefaultType,
                groupByImageType,
                Aggregation.count().as("totalGroups")
        );

        AggregationResults<Document> countResults = mongoTemplate.aggregate(countAggregation, "images", Document.class);
        countResults.getUniqueMappedResult();
        long totalGroups = countResults.getUniqueMappedResult().getInteger("totalGroups");

        int totalPages = (int) Math.ceil((double) totalGroups / size);

        return Map.of(
                "data", groupedData,
                "currentPage", page,
                "totalPages", totalPages,
                "totalItems", totalGroups
        );
    }

    @CacheEvict(value = "imageList", allEntries = true)
    public void clearImageListCache() {
        logger.info("Evicting all entries from 'imageList' cache");
    }
    @CacheEvict(value = "groupedImages", allEntries = true)
    public void clearGroupedImageCache() {
        logger.info("Evicting all entries from 'imageList' cache");
    }
}