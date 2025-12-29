package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Creates a new user.
     *
     * @param userRequestDTO the user creation request
     * @return the created user response
     * @throws IllegalArgumentException if username already exists
     */
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID
     * @return the user response
     * @throws jakarta.persistence.EntityNotFoundException if user not found
     */
    UserResponseDTO getUserById(UUID id);

    /**
     * Retrieves a user by username.
     *
     * @param username the username
     * @return the user response
     * @throws jakarta.persistence.EntityNotFoundException if user not found
     */
    UserResponseDTO getUserByUsername(String username);

    /**
     * Retrieves all users.
     *
     * @return list of all user responses
     */
    List<UserResponseDTO> getAllUsers();

    /**
     * Updates an existing user.
     *
     * @param id the user ID
     * @param userRequestDTO the user update request
     * @return the updated user response
     * @throws jakarta.persistence.EntityNotFoundException if user not found
     * @throws IllegalArgumentException if username already exists (when changing username)
     */
    UserResponseDTO updateUser(UUID id, UserRequestDTO userRequestDTO);

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID
     * @throws jakarta.persistence.EntityNotFoundException if user not found
     */
    void deleteUser(UUID id);
}

