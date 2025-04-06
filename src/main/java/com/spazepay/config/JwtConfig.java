package com.spazepay.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Key;

@Configuration
public class JwtConfig {

    @Bean
    public Key jwtSecretKey() {
        // Generate a consistent key (HS512 requires 512 bits = 64 bytes)
        return Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);
    }
}