package com.meem.repository;

import com.meem.model.entity.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<Otp, Long> {
    Optional<Otp> findByEmail(String email);
}