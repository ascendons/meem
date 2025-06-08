package com.meem.model.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Nullable
public class OtpDTO {
    private String email;
    private String password;
    private String fullName;
    private String otp;
    private String flowType;
}