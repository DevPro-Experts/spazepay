package com.spazepay.service;

import com.spazepay.model.User;
import com.spazepay.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final Key secretKey;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final long EXPIRATION_TIME = 3600000; // 1 hour in milliseconds

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    public AuthService(Key jwtSecretKey) {
        this.secretKey = jwtSecretKey; // Constructor injection
    }

    public String login(String email, String password) {
        logger.info("Login attempt for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found for email: {}", email);
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Invalid password for email: {}", email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        // Send email with account number
        try {
            emailService.sendLoginEmail(user.getEmail(), user.getFullName());
            logger.info("Email sent to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Login successful, but email notification failed", e);
        }

        logger.info("Login successful for email: {}", email);
        return token;
    }


    public User getUserFromToken(String token) {
        try {
            String email = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            logger.info("Token parsed, email: {}", email);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        } catch (Exception e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        }
    }
}