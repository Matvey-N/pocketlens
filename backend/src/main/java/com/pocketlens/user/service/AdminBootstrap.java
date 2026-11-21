package com.pocketlens.user.service;

import com.pocketlens.auth.service.PrivacyNotice;
import com.pocketlens.user.domain.Role;
import com.pocketlens.user.domain.User;
import com.pocketlens.user.persistence.UserRepository;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the administrator account from ADMIN_EMAIL / ADMIN_PASSWORD on first start (nothing is hardcoded).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder, Clock clock,
                          @Value("${pocketlens.admin.email:}") String email,
                          @Value("${pocketlens.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD is shorter than {} characters; administrator not created", MIN_PASSWORD_LENGTH);
            return;
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalized)) {
            return;
        }
        users.save(new User(normalized, passwordEncoder.encode(password), "Administrator", Role.ADMIN,
                PrivacyNotice.CURRENT_VERSION, clock.instant()));
        log.info("administrator_created");
    }
}
