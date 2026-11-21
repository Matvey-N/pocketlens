package com.pocketlens.auth.service;

import com.pocketlens.user.domain.User;
import java.time.Duration;

/**
 * Result of a successful login, registration or refresh.
 *
 * @param accessToken     short-lived JWT for the Authorization header
 * @param accessTokenTtl  lifetime of the access token
 * @param refreshToken    opaque refresh token (sent to the browser only as an HttpOnly cookie)
 * @param refreshTokenTtl lifetime of the refresh token
 */
public record AuthTokens(User user, String accessToken, Duration accessTokenTtl,
                         String refreshToken, Duration refreshTokenTtl) {
}
