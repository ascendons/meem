package com.meem.model.entity;

import jakarta.annotation.Nullable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "user")
@Nullable
public class User {
    @Id
    private String id;
    private String username;
    @Indexed(unique = true)
    private String email;
    private String mobileNumber;
    private String logoUrl;
    private String logoFileName;
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime UpdatedAt;
}
