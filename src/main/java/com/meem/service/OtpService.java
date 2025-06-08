package com.meem.service;


import com.meem.model.dto.OtpDTO;

import java.util.Map;

public interface OtpService {
    Map<String, String> generateOtp(OtpDTO email);
    Map<String, String> verifyOtp(OtpDTO otpDTO);
}