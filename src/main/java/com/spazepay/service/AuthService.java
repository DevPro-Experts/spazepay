package com.spazepay.service;

import com.spazepay.model.User;
import com.spazepay.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final long EXPIRATION_TIME = 3600000; // 1 hour in milliseconds

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

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
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS512)
                .compact();

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
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            logger.info("Token parsed, email: {}", email);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));
        } catch (io.jsonwebtoken.SignatureException e) {
            logger.warn("JWT signature validation failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        } catch (Exception e) {
            logger.warn("Unexpected error parsing JWT token: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid token");
        }
    }
}