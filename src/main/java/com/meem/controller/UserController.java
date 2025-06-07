package com.meem.controller;
import com.meem.model.dto.UserDto;
import com.meem.model.entity.User;
import com.meem.repository.UserRepository;
import com.meem.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
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

    @PostMapping("/updateUser")
    public User saveUser(@RequestBody User user) {
        return userRepository.save(user);
    }
}
