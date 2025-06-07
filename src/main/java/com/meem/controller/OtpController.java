package com.meem.controller;

import com.meem.exception.OtpException;
import com.meem.exception.OtpGenerationException;
import com.meem.model.dto.JwtDTO;
import com.meem.model.dto.OtpDTO;
import com.meem.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin("*")
public class OtpController {

    private final OtpService otpService;


    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }


    @PostMapping("/generate")
    public ResponseEntity<OtpDTO> generateOtp(@RequestBody OtpDTO otpDto) {
        try {
            OtpDTO generatedOtpDto = otpService.generateOtp(otpDto);
            generatedOtpDto.setMessage("OTP generated successfully.");
            return ResponseEntity.ok(generatedOtpDto);
        } catch (OtpException e) {
            OtpDTO errorOtpDto = new OtpDTO();
            errorOtpDto.setMessage("Error Generating OTP: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorOtpDto);
        } catch (OtpGenerationException e) {
            OtpDTO errorOtpDto = new OtpDTO();
            errorOtpDto.setMessage("Error in Request: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorOtpDto);
        } // Add this catch block to handle any other exceptions that may occur (e.g., NullPointerException, etc.
        catch (Exception e) {
            OtpDTO errorOtpDto = new OtpDTO();
            errorOtpDto.setMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(errorOtpDto);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<JwtDTO> verifyOtp(@RequestBody OtpDTO otpDto) {
        try {

            JwtDTO jwtDto;
            jwtDto=otpService.verifyOtp(otpDto);
            return ResponseEntity.ok(jwtDto);
        } catch (OtpException e) {
            JwtDTO errorJwtDto = new JwtDTO();
            errorJwtDto.setMessage("Error verifying OTP: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorJwtDto);
        } catch (Exception e) {
            JwtDTO errorJwtDto = new JwtDTO();
            errorJwtDto.setMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(errorJwtDto);
        }
    }
}
