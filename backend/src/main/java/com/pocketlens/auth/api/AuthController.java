package com.pocketlens.auth.api;

import com.pocketlens.auth.api.dto.AuthResponse;
import com.pocketlens.auth.api.dto.LoginRequest;
import com.pocketlens.auth.api.dto.RegisterRequest;
import com.pocketlens.auth.service.AuthService;
import com.pocketlens.auth.service.AuthTokens;
import com.pocketlens.auth.service.PrivacyNotice;
import com.pocketlens.auth.service.RegistrationCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints: register, login, refresh, logout, privacy notice.
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookie refreshCookie;

    public AuthController(AuthService authService, RefreshCookie refreshCookie) {
        this.authService = authService;
        this.refreshCookie = refreshCookie;
    }

    @GetMapping("/privacy-notice")
    public PrivacyNotice privacyNotice() {
        return PrivacyNotice.current();
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = authService.register(new RegistrationCommand(request.email(), request.password(),
                request.displayName(), request.acceptPrivacyNotice(), request.privacyNoticeVersion()));
        return withCookie(HttpStatus.CREATED, tokens);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookie(HttpStatus.OK, authService.login(request.email(), request.password()));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        return withCookie(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withCookie(HttpStatus status, AuthTokens tokens) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie.create(tokens.refreshToken(), tokens.refreshTokenTtl()).toString())
                .body(AuthResponse.from(tokens));
    }
}
