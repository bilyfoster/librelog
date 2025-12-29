package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for user management operations.
 */
public interface UserService {

	UserResponseDTO create(UserRequestDTO request);

	UserResponseDTO getById(UUID id);

	List<UserResponseDTO> getAll();

	UserResponseDTO update(UUID id, UserRequestDTO request);

	void delete(UUID id);

}

