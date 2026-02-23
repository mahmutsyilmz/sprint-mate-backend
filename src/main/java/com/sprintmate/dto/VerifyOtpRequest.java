package com.sprintmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for verifying a 6-digit OTP code.
 *
 * @param otpCode The 6-digit numeric OTP sent to the user's email
 */
public record VerifyOtpRequest(
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    @Pattern(regexp = "\\d{6}", message = "OTP must contain only digits")
    String otpCode
) {}
