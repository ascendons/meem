package com.meem.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Otp {
    @Id
    private String id;
    private String email;
    private String password;
    private String username;
    private String otp;
    private LocalDateTime expiryTime;
}