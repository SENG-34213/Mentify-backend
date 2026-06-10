package com.mentify.enums;

/**
 * Represents the lifecycle and state of a user's account.
 * This is crucial for managing access, security, and user lifecycle events.
 */
public enum AccountStatus {
    /**
     * The account has been invited through Keycloak and is waiting for setup.
     */
    INVITED,

    /**
     * The account is active and the user can log in.
     */
    ACTIVE,

    /**
     * The account is temporarily suspended by an admin. The user cannot log in,
     * but the account can be reactivated.
     */
    SUSPENDED,

    /**
     * The account is disabled and cannot be used.
     */
    DISABLED
}
