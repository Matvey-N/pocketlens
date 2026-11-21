package com.pocketlens.auth.service;

import com.pocketlens.auth.domain.RefreshToken;
import com.pocketlens.auth.persistence.RefreshTokenRepository;
import com.pocketlens.common.config.AppProperties;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.user.domain.Role;
import com.pocketlens.user.domain.User;
import com.pocketlens.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login, refresh-token rotation and logout (US-6.1, US-6.2).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AppProperties.Jwt jwtSettings;
    private final Clock clock;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                       TokenService tokenService, AppProperties properties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtSettings = properties.jwt();
        this.clock = clock;
    }

    @Transactional
    public AuthTokens register(RegistrationCommand command) {
        if (!command.acceptPrivacyNotice()
                || !PrivacyNotice.CURRENT_VERSION.equals(command.privacyNoticeVersion())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "The current privacy notice (version " + PrivacyNotice.CURRENT_VERSION + ") must be accepted",
                    Map.of("privacyNoticeVersion", PrivacyNotice.CURRENT_VERSION));
        }
        String email = normalizeEmail(command.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this e-mail already exists");
        }
        User user = users.save(new User(email, passwordEncoder.encode(command.password()),
                command.displayName().strip(), Role.USER, PrivacyNotice.CURRENT_VERSION, clock.instant()));
        log.info("user_registered userId={}", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> {
                    log.info("login_failed");
                    return new ApiException(ErrorCode.INVALID_CREDENTIALS, "E-mail or password is incorrect");
                });
        log.info("login_succeeded userId={}", user.getId());
        return issueTokens(user);
    }

    /**
     * Exchanges a valid refresh token for a new access token and a new refresh token (rotation).
     * Re-use of an already revoked token revokes all sessions of the user.
     */
    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        Instant now = clock.instant();
        RefreshToken stored = refreshTokens.findByTokenHash(tokenService.hash(rawRefreshToken))
                .orElseThrow(AuthService::invalidRefreshToken);
        if (stored.getRevokedAt() != null) {
            log.warn("refresh_token_reuse_detected userId={}", stored.getUserId());
            refreshTokens.revokeAllForUser(stored.getUserId(), now);
            throw invalidRefreshToken();
        }
        if (!stored.isActive(now)) {
            throw invalidRefreshToken();
        }
        stored.revoke(now);
        User user = users.findById(stored.getUserId()).orElseThrow(AuthService::invalidRefreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(tokenService.hash(rawRefreshToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    /** Removes expired refresh tokens once a day. */
    @Scheduled(cron = "${pocketlens.jobs.token-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokens.deleteExpired(clock.instant());
        log.info("expired_refresh_tokens_deleted count={}", deleted);
    }

    private AuthTokens issueTokens(User user) {
        Instant now = clock.instant();
        String refreshToken = tokenService.createRefreshToken();
        refreshTokens.save(new RefreshToken(user.getId(), tokenService.hash(refreshToken),
                now.plus(jwtSettings.refreshTokenTtl()), now));
        return new AuthTokens(user, tokenService.createAccessToken(user), jwtSettings.accessTokenTtl(),
                refreshToken, jwtSettings.refreshTokenTtl());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "The session has expired. Please log in again.");
    }
}
