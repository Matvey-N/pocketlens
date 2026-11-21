package com.pocketlens.common.security;

/**
 * Role names used in tokens and access rules.
 */
public final class Roles {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    /** Name of the JWT claim that carries the user's roles. */
    public static final String CLAIM = "roles";

    private Roles() {
    }
}
