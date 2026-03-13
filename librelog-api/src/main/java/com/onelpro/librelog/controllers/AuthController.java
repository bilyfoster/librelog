package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.ProfileUpdateRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.services.AuthService;
import com.onelpro.librelog.services.UserService;
import com.onelpro.librelog.utils.SecurityContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	private final AuthService authService;
	private final UserService userService;

	public AuthController(AuthService authService, UserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	@PostMapping("/register")
	@Operation(
			summary = "Register a new user",
			description = "Creates a new user account and returns an authentication token"
	)
	@ApiResponse(responseCode = "201", description = "User registered successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or email already exists")
	public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
		logger.info("Registration request received for email: {}", request.getEmail());
		AuthResponseDTO response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	@Operation(
			summary = "Login user",
			description = "Authenticates a user and returns an authentication token"
	)
	@ApiResponse(responseCode = "200", description = "Login successful")
	@ApiResponse(responseCode = "401", description = "Invalid credentials")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
		logger.info("Login request received for email: {}", request.getEmail());
		AuthResponseDTO response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/me")
	@Operation(
			summary = "Get current user",
			description = "Returns the currently authenticated user's information"
	)
	@ApiResponse(responseCode = "200", description = "User information retrieved successfully")
	@ApiResponse(responseCode = "401", description = "Not authenticated")
	@ApiResponse(responseCode = "404", description = "User not found")
	public ResponseEntity<UserResponseDTO> getCurrentUser() {
		java.util.UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Get current user request received but user is not authenticated");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		logger.debug("Get current user request received for ID: {}", userId);
		UserResponseDTO response = userService.getById(userId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/profile")
	@Operation(
			summary = "Update current user profile",
			description = "Updates the currently authenticated user's profile information"
	)
	@ApiResponse(responseCode = "200", description = "Profile updated successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "401", description = "Not authenticated")
	public ResponseEntity<UserResponseDTO> updateProfile(@Valid @RequestBody ProfileUpdateRequestDTO request) {
		java.util.UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Update profile request received but user is not authenticated");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		logger.info("Update profile request received for user ID: {}", userId);
		UserResponseDTO response = userService.updateProfile(userId, request);
		return ResponseEntity.ok(response);
	}

}

