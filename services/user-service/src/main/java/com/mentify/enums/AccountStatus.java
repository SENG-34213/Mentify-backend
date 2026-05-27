package com.mentify.enums;

/**
 * Represents the lifecycle and state of a user's account.
 * This is crucial for managing access, security, and user lifecycle events.
 */
public enum AccountStatus {
    /**
     * The account is active and the user can log in.
     */
    ACTIVE,

    /**
     * The account has been created but is not yet active (e.g., pending email verification).
     * It can also be manually deactivated by an admin.
     */
    INACTIVE,

    /**
     * The account is temporarily suspended by an admin. The user cannot log in,
     * but the account can be reactivated.
     */
    SUSPENDED,

    /**
     * The account is permanently blocked, usually due to a violation of terms.
     * This is a more severe state than SUSPENDED.
     */
    BLOCKED,

    /**
     * The account is awaiting approval from an administrator before it can become active.
     * Useful for systems with manual vetting processes.
     */
    PENDING_APPROVAL
}
