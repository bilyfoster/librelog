package com.onelpro.librelog.services;

import java.util.Date;
import java.util.Map;

/**
 * Service for JWT token operations.
 */
public interface JwtService {

    /**
     * Generates an access token for a user.
     *
     * @param userId the user ID
     * @param username the username
     * @param roles the user roles
     * @return the access token
     */
    String generateAccessToken(String userId, String username, java.util.Set<String> roles);

    /**
     * Generates a refresh token for a user.
     *
     * @param userId the user ID
     * @return the refresh token
     */
    String generateRefreshToken(String userId);

    /**
     * Validates a token and extracts claims.
     *
     * @param token the token to validate
     * @return the claims map
     */
    Map<String, Object> validateToken(String token);

    /**
     * Extracts the user ID from a token.
     *
     * @param token the token
     * @return the user ID
     */
    String getUserIdFromToken(String token);
}

