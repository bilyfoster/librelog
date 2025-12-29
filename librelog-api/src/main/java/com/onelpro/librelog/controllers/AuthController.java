package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

	public AuthController(AuthService authService) {
		this.authService = authService;
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

}

