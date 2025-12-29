package com.onelpro.librelog.services;

import com.onelpro.librelog.models.User;

/**
 * Service interface for authentication security operations.
 */
public interface AuthSecurityService {

    /**
     * Hashes a password using a secure hashing algorithm.
     *
     * @param rawPassword the raw password to hash
     * @return the hashed password
     */
    String hashPassword(String rawPassword);

    /**
     * Verifies a raw password against a hashed password.
     *
     * @param rawPassword the raw password to verify
     * @param hashedPassword the hashed password to compare against
     * @return true if passwords match, false otherwise
     */
    boolean verifyPassword(String rawPassword, String hashedPassword);

    /**
     * Updates the last login timestamp for a user.
     *
     * @param user the user to update
     */
    void updateLastLogin(User user);
}

