package com.meem.exception;

public class OtpGenerationException extends RuntimeException {
    public OtpGenerationException(String message) {
        super(message);
    }

    public OtpGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}