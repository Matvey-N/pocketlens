package com.pocketlens.auth.api.dto;

import com.pocketlens.auth.service.AuthTokens;
import com.pocketlens.user.api.dto.UserResponse;

/**
 * Access token returned after login, registration or refresh. The refresh token is set as an HttpOnly cookie.
 *
 * @param expiresIn lifetime of the access token in seconds
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse from(AuthTokens tokens) {
        return new AuthResponse(tokens.accessToken(), "Bearer", tokens.accessTokenTtl().toSeconds(),
                UserResponse.from(tokens.user()));
    }
}
