package com.spazepay.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SavingsException.class)
    public ResponseEntity<Object> handleSavingsException(SavingsException ex) {
        logger.error("Handling SavingsException: {} - {}", ex.getErrorCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getErrorCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", Instant.now().toString());
        return new ResponseEntity<>(body, getHttpStatus(ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            logger.warn("Validation failed for field {}: {}", error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument error: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>("An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PrematureRolloverException.class)
    public ResponseEntity<Object> handlePrematureRolloverException(PrematureRolloverException ex) {
        logger.info("Handling PrematureRolloverException: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Invalid Request");
        body.put("message", ex.getMessage());
        body.put("timestamp", Instant.now().toString());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidJwtTokenException.class)
    public ResponseEntity<Object> handleInvalidJwtTokenException(InvalidJwtTokenException ex) {
        logger.warn("Invalid JWT token: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Authentication Failed");
        body.put("message", ex.getMessage());
        body.put("timestamp", Instant.now().toString());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    private HttpStatus getHttpStatus(String errorCode) {
        switch (errorCode) {
            case "MAX_PLANS_REACHED":
            case "INSUFFICIENT_PLAN_BALANCE":
            case "INSUFFICIENT_BALANCE":
            case "ALREADY_IN_GROUP":
            case "GROUP_FULL":
            case "INACTIVE_MEMBER":
            case "GROUP_TOO_SMALL":
            case "INCOMPLETE_CONTRIBUTIONS":
            case "PAYOUTS_STARTED":
            case "PLAN_NOT_FOUND":
                return HttpStatus.BAD_REQUEST;
            case "NOT_AUTHORIZED":
                return HttpStatus.FORBIDDEN;
            default:
                logger.warn("Unknown error code: {}. Defaulting to BAD_REQUEST", errorCode);
                return HttpStatus.BAD_REQUEST;
        }
    }
}