package com.pocketlens.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pocketlens.common.config.JwtConfig;
import com.pocketlens.support.TestData;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class TokenServiceTest {

    private final JwtConfig jwtConfig = new JwtConfig(TestData.properties());
    private final java.time.Clock clock = java.time.Clock.systemUTC();
    private final TokenService service = new TokenService(jwtConfig.jwtEncoder(), TestData.properties(), clock);

    @Test
    void accessTokenIsSignedAndCarriesSubjectRolesAndExpiry() {
        String token = service.createAccessToken(TestData.user());
        JwtDecoder decoder = jwtConfig.jwtDecoder();

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(TestData.USER_ID.toString());
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of("USER"));
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now().plusSeconds(14 * 60));
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("pocketlens");
    }

    @Test
    void refreshTokensAreRandomAndHashedDeterministically() {
        String a = service.createRefreshToken();
        String b = service.createRefreshToken();
        assertThat(a).isNotEqualTo(b).hasSizeGreaterThanOrEqualTo(43);
        assertThat(service.hash(a)).isEqualTo(service.hash(a)).hasSize(64).isNotEqualTo(a);
    }
}
