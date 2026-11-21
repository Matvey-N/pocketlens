package com.pocketlens.user.domain;

import com.pocketlens.common.security.Roles;

/**
 * Role of a user account.
 */
public enum Role {
    USER(Roles.USER),
    ADMIN(Roles.ADMIN);

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
