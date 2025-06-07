package com.meem.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "otp")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Otp {

    @Id
    private String id;
    private String otp;
    private Date createdAt;
    private Date expiryTime;
    @DBRef
    private String email;
}