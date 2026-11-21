package com.pocketlens.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.support.TestData;
import com.pocketlens.user.domain.Role;
import com.pocketlens.user.domain.User;
import com.pocketlens.user.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void createsAdministratorOnce() {
        when(encoder.encode(any())).thenReturn("bcrypt");
        new AdminBootstrap(users, encoder, TestData.clock(), "Admin@Example.com", "a-long-admin-password").run(null);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void skipsWhenNotConfiguredTooWeakOrExisting() {
        new AdminBootstrap(users, encoder, TestData.clock(), "", "").run(null);
        new AdminBootstrap(users, encoder, TestData.clock(), "a@b.c", "short").run(null);
        when(users.existsByEmailIgnoreCase("a@b.c")).thenReturn(true);
        new AdminBootstrap(users, encoder, TestData.clock(), "a@b.c", "a-long-admin-password").run(null);
        verify(users, never()).save(any());
    }
}
