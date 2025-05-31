package com.meem.model.dto;

import lombok.Data;

@Data
public class ImageGroupDto {
    private String title;
    private String image;

    public ImageGroupDto(String title, String image) {
        this.title = title;
        this.image = image;
    }

    // Getters and setters
}