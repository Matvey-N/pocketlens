package com.pocketlens.auth.service;

import com.pocketlens.common.config.AppProperties;
import com.pocketlens.common.security.Roles;
import com.pocketlens.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed access tokens and random refresh tokens, and hashes refresh tokens for storage.
 */
@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder encoder;
    private final AppProperties.Jwt settings;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, AppProperties properties, Clock clock) {
        this.encoder = encoder;
        this.settings = properties.jwt();
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(settings.accessTokenTtl()))
                .claim(Roles.CLAIM, List.of(user.getRole().authority()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String createRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
