package com.pocketlens.auth.service;

/**
 * Current privacy notice shown at registration (US-6.2). Users must accept exactly this version.
 */
public record PrivacyNotice(String version, String text) {

    public static final String CURRENT_VERSION = "2026-11";

    public static PrivacyNotice current() {
        return new PrivacyNotice(CURRENT_VERSION, """
                PocketLens stores the transactions you import, your categories, budgets and assistant conversations \
                to provide the service you request (GDPR Art. 6(1)(b)). Your data is never sold or shared for \
                advertising. When you use AI features, merchant descriptions and aggregated results are processed \
                by the LLM provider as a data processor; names, IBANs, card and account numbers are removed before \
                any request. You can export all your data or delete your account at any time in Settings.""");
    }
}
