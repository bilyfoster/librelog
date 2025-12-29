package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns tokens.
     *
     * @param loginRequest the login request containing username and password
     * @return authentication response with tokens and user information
     */
    AuthResponseDTO login(LoginRequestDTO loginRequest);

    /**
     * Registers a new user.
     *
     * @param registerRequest the registration request
     * @return authentication response with tokens and user information
     */
    AuthResponseDTO register(RegisterRequestDTO registerRequest);

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return new authentication response with new tokens
     */
    AuthResponseDTO refreshToken(String refreshToken);

    /**
     * Logs out a user by blacklisting their tokens.
     *
     * @param accessToken the access token to invalidate
     * @param refreshToken the refresh token to invalidate
     */
    void logout(String accessToken, String refreshToken);

    /**
     * Gets the current user information.
     *
     * @param userId the user ID
     * @return user response DTO
     */
    UserResponseDTO getCurrentUser(java.util.UUID userId);
}

