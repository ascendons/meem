package com.meem.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageMetadataDto {
    private String fileName;
    private String imageType;
    private String imageTag;
    private String url;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt;
}