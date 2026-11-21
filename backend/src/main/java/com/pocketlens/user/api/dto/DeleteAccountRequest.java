package com.pocketlens.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Confirmation of account deletion with the current password.
 */
public record DeleteAccountRequest(@NotBlank @Size(max = 72) String password) {
}
