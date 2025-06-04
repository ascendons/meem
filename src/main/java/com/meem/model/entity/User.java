package com.meem.model.entity;

import lombok.Data;
import org.springframework.cglib.core.Local;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "user")

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
