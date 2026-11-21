package com.pocketlens.auth.api;

import com.pocketlens.common.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the HttpOnly refresh-token cookie. It is limited to the auth path and SameSite=Strict, so JavaScript
 * cannot read it and other sites cannot send it.
 */
@Component
public class RefreshCookie {

    public static final String NAME = "pl_refresh";
    private static final String PATH = "/api/v1/auth";

    private final boolean secure;

    public RefreshCookie(AppProperties properties) {
        this.secure = properties.jwt().secureCookie();
    }

    public ResponseCookie create(String token, Duration ttl) {
        return base(token).maxAge(ttl).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict").path(PATH);
    }
}
