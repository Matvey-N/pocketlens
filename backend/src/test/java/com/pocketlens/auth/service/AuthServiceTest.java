package com.pocketlens.auth.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.auth.domain.RefreshToken;
import com.pocketlens.auth.persistence.RefreshTokenRepository;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.support.TestData;
import com.pocketlens.user.domain.Role;
import com.pocketlens.user.domain.User;
import com.pocketlens.user.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TokenService tokenService = mock(TokenService.class);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, tokens, encoder, tokenService, TestData.properties(), TestData.clock());
        when(tokenService.createAccessToken(any())).thenReturn("access");
        when(tokenService.createRefreshToken()).thenReturn("refresh");
        when(tokenService.hash(anyString())).thenAnswer(i -> "hash-" + i.getArgument(0));
        when(encoder.encode("Secret12345")).thenReturn("bcrypt");
    }

    @Test
    void registersUserWithConsentAndIssuesTokens() {
        when(users.save(any())).thenAnswer(i -> TestData.withId(i.getArgument(0), USER_ID));

        AuthTokens result = service.register(new RegistrationCommand(" Anna@Example.com ", "Secret12345", " Anna ",
                true, PrivacyNotice.CURRENT_VERSION));

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(result.user().getEmail()).isEqualTo("anna@example.com");
        assertThat(result.user().getDisplayName()).isEqualTo("Anna");
        assertThat(result.user().getRole()).isEqualTo(Role.USER);
        assertThat(result.user().getPasswordHash()).isEqualTo("bcrypt");
        ArgumentCaptor<RefreshToken> stored = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).save(stored.capture());
        assertThat(stored.getValue().getTokenHash()).isEqualTo("hash-refresh");
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(TestData.NOW.plusSeconds(7 * 24 * 3600));
    }

    @Test
    void rejectsRegistrationWithoutConsentOrWithTakenEmail() {
        assertThatThrownBy(() -> service.register(new RegistrationCommand("a@b.c", "Secret12345", "A", false,
                PrivacyNotice.CURRENT_VERSION))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.register(new RegistrationCommand("a@b.c", "Secret12345", "A", true,
                "2020-01"))).isInstanceOf(ApiException.class);
        when(users.existsByEmailIgnoreCase("a@b.c")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegistrationCommand("a@b.c", "Secret12345", "A", true,
                PrivacyNotice.CURRENT_VERSION))).isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void logsInWithCorrectPasswordOnly() {
        User user = TestData.user();
        when(users.findByEmailIgnoreCase("anna@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("right", "hash")).thenReturn(true);

        assertThat(service.login("ANNA@example.com", "right").user()).isEqualTo(user);
        assertThatThrownBy(() -> service.login("anna@example.com", "wrong"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThatThrownBy(() -> service.login(null, "x")).isInstanceOf(ApiException.class);
    }

    @Test
    void rotatesRefreshTokens() {
        RefreshToken stored = new RefreshToken(USER_ID, "hash-old", TestData.NOW.plusSeconds(3600), TestData.NOW);
        when(tokens.findByTokenHash("hash-old")).thenReturn(Optional.of(stored));
        when(users.findById(USER_ID)).thenReturn(Optional.of(TestData.user()));

        AuthTokens result = service.refresh("old");

        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(stored.getRevokedAt()).isEqualTo(TestData.NOW);
    }

    @Test
    void reuseOfRevokedTokenRevokesAllSessions() {
        RefreshToken revoked = new RefreshToken(USER_ID, "hash-old", TestData.NOW.plusSeconds(3600), TestData.NOW);
        revoked.revoke(TestData.NOW);
        when(tokens.findByTokenHash("hash-old")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(tokens).revokeAllForUser(USER_ID, TestData.NOW);
    }

    @Test
    void rejectsMissingUnknownOrExpiredRefreshTokens() {
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.refresh(" ")).isInstanceOf(ApiException.class);
        when(tokens.findByTokenHash("hash-unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("unknown")).isInstanceOf(ApiException.class);
        RefreshToken expired = new RefreshToken(USER_ID, "hash-exp", TestData.NOW.minusSeconds(1), TestData.NOW);
        when(tokens.findByTokenHash("hash-exp")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.refresh("exp")).isInstanceOf(ApiException.class);
        RefreshToken orphan = new RefreshToken(USER_ID, "hash-orphan", TestData.NOW.plusSeconds(60), TestData.NOW);
        when(tokens.findByTokenHash("hash-orphan")).thenReturn(Optional.of(orphan));
        when(users.findById(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("orphan")).isInstanceOf(ApiException.class);
    }

    @Test
    void logoutRevokesTokenAndCleanupDeletesExpired() {
        RefreshToken stored = new RefreshToken(USER_ID, "hash-t", TestData.NOW.plusSeconds(3600), TestData.NOW);
        when(tokens.findByTokenHash("hash-t")).thenReturn(Optional.of(stored));
        service.logout("t");
        assertThat(stored.isActive(TestData.NOW)).isFalse();
        service.logout(null);
        service.logout("");

        service.deleteExpiredTokens();
        verify(tokens).deleteExpired(TestData.NOW);
    }

    @Test
    void privacyNoticeHasCurrentVersion() {
        assertThat(PrivacyNotice.current().version()).isEqualTo(PrivacyNotice.CURRENT_VERSION);
        assertThat(PrivacyNotice.current().text()).contains("GDPR");
    }
}
