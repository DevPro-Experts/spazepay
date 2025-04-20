package com.spazepay.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {
    private String errorCode;
    private String message;
    private String timestamp;

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }
}