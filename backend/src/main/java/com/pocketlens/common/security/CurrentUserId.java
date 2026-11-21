package com.pocketlens.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id ({@link java.util.UUID}) of the authenticated user into a controller method parameter.
 * Every data query is scoped by this id (NFR-S2).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
