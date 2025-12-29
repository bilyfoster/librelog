package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;

/**
 * Service for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns tokens.
     *
     * @param loginRequest the login request
     * @return the authentication response with tokens
     */
    AuthResponseDTO login(LoginRequestDTO loginRequest);
}

