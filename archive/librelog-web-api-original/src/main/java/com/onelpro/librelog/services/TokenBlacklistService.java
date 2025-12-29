package com.onelpro.librelog.services;

import java.util.Set;

/**
 * Service interface for managing blacklisted JWT tokens.
 */
public interface TokenBlacklistService {

    /**
     * Adds a token to the blacklist.
     *
     * @param token the token to blacklist
     */
    void blacklistToken(String token);

    /**
     * Checks if a token is blacklisted.
     *
     * @param token the token to check
     * @return true if the token is blacklisted, false otherwise
     */
    boolean isTokenBlacklisted(String token);

    /**
     * Removes expired tokens from the blacklist.
     */
    void cleanupExpiredTokens();

    /**
     * Gets all blacklisted tokens (for testing/debugging purposes).
     *
     * @return set of blacklisted tokens
     */
    Set<String> getBlacklistedTokens();
}

