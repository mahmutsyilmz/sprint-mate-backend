package com.sprintmate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for sending an email verification OTP.
 *
 * @param email The email address to verify (must be a valid email format)
 */
public record SendVerificationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    String email
) {}
