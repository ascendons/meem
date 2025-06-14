package com.meem.serviceImpl;

import com.meem.model.dto.JwtDTO;
import com.meem.model.dto.OtpDTO;
import com.meem.model.entity.Otp;
import com.meem.model.entity.User;
import com.meem.repository.OtpRepository;
import com.meem.repository.UserRepository;
import com.meem.service.MailService;
import com.meem.service.OtpService;
import com.meem.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;

    private final MailService mailService;

    private static final int EXPIRY_MINUTES = 5;

    public OtpServiceImpl(OtpRepository otpRepository, UserRepository userRepository, MailService mailService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Override
    public Map<String, String> generateOtp(OtpDTO otpDTO) {
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        Otp otp = otpRepository.findByEmail(otpDTO.getEmail()).orElse(new Otp());
        otp.setEmail(otpDTO.getEmail());
        otp.setPassword(PasswordUtil.encryptPassword(otpDTO.getPassword()));
        otp.setUsername(otpDTO.getFullName());
        otp.setOtp(otpCode);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        otpRepository.save(otp);

        try {
            String subject = switch (otpDTO.getFlowType().toUpperCase()) {
                case "FORGOT_PASSWORD" -> "Tonikra: Reset Your Password";
                case "REGISTER" -> "Welcome to Tonikra!";
                default -> "Tonikra: Your One-Time Password (OTP)";
            };
            String template = loadTemplateFromClasspath("templates/template.html");
            boolean isForgotPassword = "FORGOT_PASSWORD".equalsIgnoreCase(otpDTO.getFlowType());
            String htmlBody = getHtmlEmailBody(template, otpCode, otpDTO.getFullName(), isForgotPassword);
            mailService.sendHtmlEmail(otpDTO.getEmail(), subject, htmlBody);
            Map<String, String> response = new HashMap<>();
            response.put("otp", "OTP sent successfully");
            log.info("OTP sent to {}", otpDTO.getEmail());
            return response;
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            log.error("Failed to send OTP", e);
            response.put("otp", "OTP failed to send");
            return response;

        }

    }
    public String loadTemplateFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String getHtmlEmailBody(String template, String otpCode, String fullName, boolean isForgotPassword) {
        return template
                .replace("{{otpCode}}", otpCode)
                .replace("{{fullName}}", fullName)
                .replace("{{#if isForgotPassword}}", isForgotPassword ? "" : "<!--")
                .replace("{{/if}}", isForgotPassword ? "" : "-->")
                .replace("{{else}}", isForgotPassword ? "<!--" : "")
                ;
    }

    @Override
    public Map<String, String> verifyOtp(OtpDTO otpDTO) {
        Otp storedOtp = otpRepository.findByEmail(otpDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("OTP not found for the provided email"));

        if (!storedOtp.getOtp().equals(otpDTO.getOtp())) {
            log.error("Invalid OTP for email {}", otpDTO.getEmail());
            throw new RuntimeException("Invalid OTP");
        }

        if (storedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.error("OTP has expired for email {}", otpDTO.getEmail());
            throw new RuntimeException("OTP has expired");
        }

        Optional<User> optionalUser = userRepository.findByEmail(storedOtp.getEmail());

        if ("REGISTER".equalsIgnoreCase(otpDTO.getFlowType())) {
            if (optionalUser.isPresent()) {
                log.error("User already exists for email {}", otpDTO.getEmail());
                throw new RuntimeException("User already exists. Please login or reset password.");
            }

            User newUser = new User();
            newUser.setEmail(storedOtp.getEmail());
            newUser.setPassword(storedOtp.getPassword());
            newUser.setUsername(storedOtp.getUsername());
            userRepository.save(newUser);
            log.info("User registered successfully for email {}", otpDTO.getEmail());

        } else if ("FORGOT_PASSWORD".equalsIgnoreCase(otpDTO.getFlowType())) {
            if (optionalUser.isEmpty()) {
                log.error("User not found for email {}", otpDTO.getEmail());
                throw new RuntimeException("User not found. Please register.");
            }

            User existingUser = optionalUser.get();
            existingUser.setPassword(storedOtp.getPassword()); // Already encrypted
            userRepository.save(existingUser);
        } else {
            log.error("Invalid flow type {}", otpDTO.getFlowType());
            throw new RuntimeException("Invalid flow type.");
        }

        JwtDTO jwt = new JwtDTO();
        jwt.setToken("mock-jwt-token-for-" + otpDTO.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification successful");
        log.info("Verification successful for email {}", otpDTO.getEmail());
        return response;
    }
}
