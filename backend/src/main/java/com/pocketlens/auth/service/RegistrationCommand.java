package com.pocketlens.auth.service;

/**
 * Input of a registration.
 */
public record RegistrationCommand(String email, String password, String displayName,
                                  boolean acceptPrivacyNotice, String privacyNoticeVersion) {
}
