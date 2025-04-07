package com.spazepay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PrematureRolloverException extends RuntimeException {

    public PrematureRolloverException(String message) {
        super(message);
    }

    public PrematureRolloverException(String message, Throwable cause) {
        super(message, cause);
    }
}
