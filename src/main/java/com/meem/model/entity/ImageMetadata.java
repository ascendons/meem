package com.meem.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "images")
public class ImageMetadata {
    @Id
    private String id;
    private String fileName;
    private String imageType;
    private String imageTag;
    private String url;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt;
}