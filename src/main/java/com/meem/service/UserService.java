package com.meem.service;

import com.meem.model.dto.UserDto;
import com.meem.model.entity.User;
import com.meem.repository.UserRepository;
import com.meem.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.cdnBaseUrl}")
    private String cdnBaseUrl;

    private final S3Client s3Client;
    private final UserRepository repository;

    public UserService(S3Client s3Client, UserRepository repository) {
        this.s3Client = s3Client;
        this.repository = repository;
    }

    public UserDto save(MultipartFile file, String userName, String mobileNumber, String gender, String email, String folder) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String fileName = UUID.randomUUID() + "-" + sanitizedFileName;

        String key = folder + "/" + fileName;

        // Upload to Cloudflare R2
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        String url = cdnBaseUrl + "/" + key;

        // Check if user already exists
        User existingUser = repository.findByEmail(email).isPresent() ? repository.findByEmail(email).get() : null;

        if (existingUser != null) {
            // Update user
            existingUser.setUsername(userName);
            existingUser.setLogoUrl(url);
            existingUser.setMobileNumber(mobileNumber);
            existingUser.setGender(gender);
            existingUser.setUpdatedAt(LocalDateTime.now());

            repository.save(existingUser);
            UserDto userDto = new UserDto();
            userDto.setUsername(userName);
            userDto.setEmail(email);
            userDto.setLogoUrl(url);
            userDto.setMobileNumber(mobileNumber);
            userDto.setGender(gender);

            return userDto;
        }

        // New user
        User newUser = new User();
        newUser.setLogoFileName(sanitizedFileName);
        newUser.setLogoUrl(url);
        newUser.setMobileNumber(mobileNumber);
        newUser.setEmail(email);
        newUser.setGender(gender);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        repository.save(newUser);
        UserDto userDto = new UserDto();
        userDto.setUsername(userName);
        userDto.setEmail(email);
        userDto.setLogoUrl(url);
        userDto.setMobileNumber(mobileNumber);
        userDto.setGender(gender);

        return userDto;
    }
    private String sanitizeFileName(String originalFileName) {
        originalFileName = Paths.get(originalFileName).getFileName().toString();
        return originalFileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    public UserDto login(String email, String password) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        UserDto userDto = new UserDto();
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setLogoUrl(user.getLogoUrl());
        userDto.setGender(user.getGender());
        userDto.setMobileNumber(user.getMobileNumber());

        return userDto;
    }

}
