package com.meem.controller;
import com.meem.model.dto.UserDto;
import com.meem.model.entity.User;
import com.meem.repository.UserRepository;
import com.meem.service.UserService;
import com.meem.service.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final MailService mailService;

    public UserController(UserService userService, UserRepository userRepository, MailService mailService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @PostMapping("/save")
    public ResponseEntity<UserDto> save(@RequestParam("file") MultipartFile file, @RequestParam("userName") String userName, @RequestParam("mobileNumber") String mobileNumber, @RequestParam("gender") String gender, @RequestParam("email") String email) {
        try {
            return ResponseEntity.ok(userService.save(file, userName, mobileNumber, gender,email, "logo"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/getByEmail")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sendWelcomeEmail")
    public ResponseEntity<String> sendWelcomeEmail(@RequestParam String email) {
        mailService.sendEmail(email, "Welcome to Meem!", "Thanks for registering with Meem.");
        return ResponseEntity.ok("Email sent!");
    }

    @PostMapping("/updateUser")
    public ResponseEntity<Map<String, User>> saveUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(Collections.singletonMap("user", savedUser));
    }
}
