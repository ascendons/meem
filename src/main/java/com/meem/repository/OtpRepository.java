package com.meem.repository;

import com.meem.model.entity.Otp;
import com.meem.model.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OtpRepository extends MongoRepository<Otp, Integer> {

    Otp getOtpByEmail(String email);
}