package com.spazepay.exception;

public class SavingsException extends RuntimeException {
    private final String errorCode;

    public SavingsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SavingsException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}