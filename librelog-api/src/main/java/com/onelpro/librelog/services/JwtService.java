package com.onelpro.librelog.services;

import java.util.UUID;

/**
 * Service interface for JWT token operations.
 */
public interface JwtService {

	String generateToken(UUID userId, String email, String role);

	String extractUserId(String token);

	String extractEmail(String token);

	String extractRole(String token);

	boolean isTokenValid(String token);

}

