package com.spazepay.controller;

import com.spazepay.dto.LoginRequest;
import com.spazepay.dto.UpdateProfileRequest;
import com.spazepay.model.User;
import com.spazepay.service.AuthService;
import com.spazepay.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login request for email: {}", loginRequest.getEmail());
        String token = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal User user) {
        logger.info("Profile request for user: {}", user.getEmail());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        logger.info("Update profile request for user: {}", user.getEmail());
        User updatedUser = userService.updateProfile(user, request);
        return ResponseEntity.ok(updatedUser);
    }
}