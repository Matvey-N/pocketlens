package com.pocketlens.user.api.dto;

import com.pocketlens.user.domain.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of the current user.
 */
public record UserResponse(UUID id, String email, String displayName, String role,
                           String privacyNoticeVersion, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name(),
                user.getPrivacyNoticeVersion(), user.getCreatedAt());
    }
}
