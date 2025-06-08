package com.meem.controller;

import com.meem.model.dto.OtpDTO;
import com.meem.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin("*")
public class OtpController {

    private final OtpService otpService;


    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping(path="/generate", produces = "application/json")
    public ResponseEntity<Map<String, String>> generateOtp(@RequestBody OtpDTO otpDTO) {
        return ResponseEntity.ok().body(otpService.generateOtp(otpDTO));

    }

    @PostMapping( path = "/verify", produces = "application/json")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody OtpDTO otpDTO) {
        return ResponseEntity.ok(otpService.verifyOtp(otpDTO));
    }
}
