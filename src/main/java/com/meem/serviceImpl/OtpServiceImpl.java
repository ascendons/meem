package com.meem.serviceImpl;

import com.meem.exception.OtpException;
import com.meem.exception.OtpGenerationException;
import com.meem.model.dto.JwtDTO;
import com.meem.model.dto.OtpDTO;
import com.meem.model.entity.Otp;
import com.meem.model.entity.User;
import com.meem.repository.OtpRepository;
import com.meem.repository.UserRepository;
import com.meem.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpServiceImpl implements OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;


    @Autowired
    public OtpServiceImpl(OtpRepository otpRepository, UserRepository userRepository) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
    }
    private static final SecureRandom secureRandom = new SecureRandom(); // thread safe
    private static final int OTP_NUMBER_MAX = 999999;

    @Override
    public OtpDTO generateOtp(OtpDTO otpDto) {
        try {
            logger.info("Generating OTP for mobile: {}", otpDto.getEmail());
            String email = otpDto.getEmail();
            if (otpDto.getEmail() == null) {
                throw new OtpGenerationException("Invalid mobile number. Please enter a 10-digit mobile number.");
            }


            Optional<User> user = userRepository.findByEmail(otpDto.getEmail());

            if (user.isEmpty()) {
                logger.info("No user found with email: {}. Creating new user.", otpDto.getEmail());
                user = Optional.of(new User());
                user.get().setEmail(otpDto.getEmail());
                userRepository.save(user.get());
            }

            Otp otpEntity = otpRepository.getOtpByEmail(user.get().getEmail());

            // If the Otp entity exists and has not expired, return the existing OTP
            if (otpEntity != null && otpEntity.getExpiryTime().after(new Timestamp(System.currentTimeMillis()))) {
                otpDto.setOtp(otpEntity.getOtp());
                logger.info("Existing OTP returned for mobile: {}", otpDto.getEmail());
                return otpDto;
            }

            // If the Otp entity does not exist or has expired, delete the existing Otp entity (if any)
            if (otpEntity != null) {
                otpRepository.delete(otpEntity);
            }
            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            cal.add(Calendar.MINUTE, 3);


            // Generate a new OTP, save the new Otp entity to the database, and return the new OTP
            String otp = generateSixDigitOtp();

            otpEntity = new Otp();
            otpEntity.setOtp(otp);
            otpEntity.setEmail(email);
            otpEntity.setCreatedAt(now);
            otpEntity.setExpiryTime(cal.getTime());

            otpRepository.save(otpEntity);

            otpDto.setOtp(otp);
            logger.info("New OTP generated successfully for mobile: {}", otpDto.getEmail());
            return otpDto;
        } catch (OtpGenerationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate OTP for mobile: " + otpDto.getEmail(), e);
            throw new OtpGenerationException("Failed to generate OTP for mobile: " + otpDto.getEmail(), e);
        }
    }

    private String generateSixDigitOtp() {
        int randomNum = secureRandom.nextInt(OTP_NUMBER_MAX);
        return String.format("%06d", randomNum); // convert to 6 digit with leading zeros if necessary
    }

    @Override
    public JwtDTO verifyOtp(OtpDTO otpDto) throws Exception {
        Otp otp;
        Optional<User> user;
        try {
            user = userRepository.findByEmail(otpDto.getEmail());
            // Fetch the OTP associated with the user from the database
            otp = otpRepository.getOtpByEmail(user.get().getEmail());
        } catch (Exception e) {
            throw new OtpException("Failed to fetch OTP for mobile: " + otpDto.getEmail(), e);
        }

        // If the Otp entity does not exist, throw an exception
        if (otp == null) {
            throw new OtpException("No OTP found for mobile: " + otpDto.getEmail(), new Exception());
        }

        // Compare the fetched OTP with the OTP provided by the user
        boolean isValid = otpDto.getOtp().equals(otp.getOtp());

        JwtDTO jwtDto = new JwtDTO();
        try{
            if (!isValid) {
                throw new OtpException("Otp Verification failed.",new Exception());
            }
        } catch (OtpException e) {
            throw e;
        }
        clearOtp(otpDto);
        jwtDto.setMessage("OTP verified successfully.");
        return jwtDto;
    }

    @Override
    public void clearOtp(OtpDTO otpDto) throws Exception {
        Otp otpEntity;
        try {
            Optional<User> user = userRepository.findByEmail(otpDto.getEmail());
            otpEntity = otpRepository.getOtpByEmail(user.get().getEmail());
        } catch (Exception e) {
            throw new OtpException("Failed to fetch OTP for mobile: " + otpDto.getEmail(), e);
        }
        if (otpEntity == null) {
            throw new OtpException("No OTP found for mobile: " + otpDto.getEmail(), new Exception());        }

        try {
            otpRepository.delete(otpEntity);
        } catch (Exception e) {
            throw new OtpException("Failed to clear OTP for mobile: " + otpDto.getEmail(), e);
        }
    }
}
