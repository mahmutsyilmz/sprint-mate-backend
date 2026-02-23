package com.sprintmate.exception;

/**
 * Exception thrown when an OTP verification code is invalid or has expired.
 *
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}
