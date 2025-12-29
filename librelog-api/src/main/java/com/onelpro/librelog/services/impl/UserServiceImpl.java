package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.UserService;
import com.onelpro.librelog.utils.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of user management service.
 */
@Service
public class UserServiceImpl implements UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public UserResponseDTO create(UserRequestDTO request) {
		logger.info("Creating user with email: {}", request.getEmail());

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already registered");
		}

		if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
			throw new BadRequestException("Password is required");
		}

		if (!PasswordValidator.isValid(request.getPassword())) {
			throw new BadRequestException("Password must be between 8 and 128 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
		}

		User user = User.builder()
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.status(request.getStatus())
				.role(request.getRole())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		User savedUser = userRepository.save(user);
		logger.info("User created successfully with ID: {}", savedUser.getId());

		return mapToResponseDTO(savedUser);
	}

	@Override
	public UserResponseDTO getById(UUID id) {
		logger.info("Fetching user with ID: {}", id);
		User user = userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + id));
		return mapToResponseDTO(user);
	}

	@Override
	public List<UserResponseDTO> getAll() {
		logger.info("Fetching all users");
		return userRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public UserResponseDTO update(UUID id, UserRequestDTO request) {
		logger.info("Updating user with ID: {}", id);
		User user = userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + id));

		// Check if email is being changed and if it's already taken
		if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already registered");
		}

		user.setEmail(request.getEmail());
		user.setStatus(request.getStatus());
		user.setRole(request.getRole());
		user.setUpdatedAt(LocalDateTime.now());

		// Update password only if provided
		if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
			if (!PasswordValidator.isValid(request.getPassword())) {
				throw new BadRequestException("Password must be between 8 and 128 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
			}
			user.setPassword(passwordEncoder.encode(request.getPassword()));
		}

		User updatedUser = userRepository.save(user);
		logger.info("User updated successfully with ID: {}", updatedUser.getId());

		return mapToResponseDTO(updatedUser);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting user with ID: {}", id);
		if (!userRepository.existsById(id)) {
			throw new NotFoundException("User not found with id: " + id);
		}
		userRepository.deleteById(id);
		logger.info("User deleted successfully with ID: {}", id);
	}

	private UserResponseDTO mapToResponseDTO(User user) {
		return UserResponseDTO.builder()
				.id(user.getId())
				.email(user.getEmail())
				.status(user.getStatus())
				.role(user.getRole())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.build();
	}

}

