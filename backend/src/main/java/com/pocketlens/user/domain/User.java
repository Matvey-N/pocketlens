package com.pocketlens.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Registered user account. Stores only what is needed for the service (data minimisation, GDPR Art. 5).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(name = "privacy_notice_version", nullable = false, length = 16)
    private String privacyNoticeVersion;

    @Column(name = "consent_given_at", nullable = false)
    private Instant consentGivenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String passwordHash, String displayName, Role role,
                String privacyNoticeVersion, Instant now) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.privacyNoticeVersion = privacyNoticeVersion;
        this.consentGivenAt = now;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public String getPrivacyNoticeVersion() {
        return privacyNoticeVersion;
    }

    public Instant getConsentGivenAt() {
        return consentGivenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
