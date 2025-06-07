package com.meem.service;


import com.meem.exception.OtpException;
import com.meem.model.dto.JwtDTO;
import com.meem.model.dto.OtpDTO;

public interface OtpService {
    OtpDTO generateOtp(OtpDTO otpDto) throws OtpException;
    JwtDTO verifyOtp(OtpDTO otpDto) throws Exception;
    void clearOtp(OtpDTO otpDto) throws Exception;
}