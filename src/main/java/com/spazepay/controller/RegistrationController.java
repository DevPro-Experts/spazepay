package com.spazepay.controller;

import com.spazepay.dto.RegistrationRequest;
import com.spazepay.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok("Registration successful");
    }
}
