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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
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

        String subject;
        String body;

        if ("FORGOT_PASSWORD".equalsIgnoreCase(otpDTO.getFlowType())) {
            subject = "Meem: Reset Your Password";
            body = "You're trying to reset your password. Use this OTP: " + otpCode;
        } else {
            subject = "Welcome to Meem!";
            body = "Thanks for registering with Meem. This is your OTP: " + otpCode;
        }

        mailService.sendEmail(otpDTO.getEmail(), subject, body);
        Map<String, String> response = new HashMap<>();
        response.put("otp", "OTP sent successfully");
        return response;
    }

    @Override
    public Map<String, String> verifyOtp(OtpDTO otpDTO) {
        Otp storedOtp = otpRepository.findByEmail(otpDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("OTP not found for the provided email"));

        if (!storedOtp.getOtp().equals(otpDTO.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (storedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        Optional<User> optionalUser = userRepository.findByEmail(storedOtp.getEmail());

        if ("REGISTER".equalsIgnoreCase(otpDTO.getFlowType())) {
            if (optionalUser.isPresent()) {
                throw new RuntimeException("User already exists. Please login or reset password.");
            }

            User newUser = new User();
            newUser.setEmail(storedOtp.getEmail());
            newUser.setPassword(storedOtp.getPassword());
            newUser.setUsername(storedOtp.getUsername());
            userRepository.save(newUser);

        } else if ("FORGOT_PASSWORD".equalsIgnoreCase(otpDTO.getFlowType())) {
            if (optionalUser.isEmpty()) {
                throw new RuntimeException("User not found. Please register.");
            }

            User existingUser = optionalUser.get();
            existingUser.setPassword(storedOtp.getPassword()); // Already encrypted
            userRepository.save(existingUser);
        } else {
            throw new RuntimeException("Invalid flow type.");
        }

        JwtDTO jwt = new JwtDTO();
        jwt.setToken("mock-jwt-token-for-" + otpDTO.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification successful");
        return response;
    }
}
