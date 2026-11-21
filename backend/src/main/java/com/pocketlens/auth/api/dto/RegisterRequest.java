package com.pocketlens.auth.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration form. The password must have 10–72 characters with at least one letter and one digit
 * (72 bytes is the BCrypt input limit).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain at least one letter and one digit")
        String password,
        @NotBlank @Size(max = 80) String displayName,
        @AssertTrue(message = "must be accepted") boolean acceptPrivacyNotice,
        @NotBlank String privacyNoticeVersion) {
}
