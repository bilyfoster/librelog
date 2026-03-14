package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.UnauthorizedException;
import com.onelpro.librelog.exceptions.ValidationException;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuthService;
import com.onelpro.librelog.services.JwtService;
import com.onelpro.librelog.utils.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of authentication service.
 */
@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;

	public AuthServiceImpl(
			UserRepository userRepository,
			JwtService jwtService,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.jwtService = jwtService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public AuthResponseDTO register(RegisterRequestDTO request) {
		logger.info("Registering new user with email: {}", request.getEmail());

		if (userRepository.existsByEmail(request.getEmail())) {
			logger.warn("Registration failed: email already exists - {}", request.getEmail());
			throw new BadRequestException("Email already registered");
		}

		if (!PasswordValidator.isValid(request.getPassword())) {
			logger.warn("Registration failed: password does not meet requirements");
			throw new ValidationException(PasswordValidator.getRequirements());
		}

		User user = User.builder()
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.status(UserStatus.ACTIVE)
				.role(UserRole.OPERATIONS)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		user = userRepository.save(user);
		logger.info("User registered successfully with ID: {}", user.getId());

		String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

		return AuthResponseDTO.builder()
				.token(token)
				.userId(user.getId())
				.email(user.getEmail())
				.role(user.getRole().name())
				.build();
	}

	@Override
	public AuthResponseDTO login(LoginRequestDTO request) {
		String effectiveEmail = request.getEffectiveEmail();
		
		if (effectiveEmail == null || effectiveEmail.trim().isEmpty()) {
			logger.warn("Login failed: email or username is required");
			throw new UnauthorizedException("Email or username is required");
		}
		
		logger.info("Login attempt for email: {}", effectiveEmail);

		User user = userRepository.findByEmail(effectiveEmail)
				.orElseThrow(() -> {
					logger.warn("Login failed: user not found - {}", effectiveEmail);
					return new UnauthorizedException("Invalid email or password");
				});

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			logger.warn("Login failed: invalid password for user - {}", effectiveEmail);
			throw new UnauthorizedException("Invalid email or password");
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			logger.warn("Login failed: user account is not active - {}", effectiveEmail);
			throw new UnauthorizedException("Account is not active");
		}

		logger.info("Login successful for user: {}", user.getEmail());

		String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

		return AuthResponseDTO.builder()
				.token(token)
				.userId(user.getId())
				.email(user.getEmail())
				.role(user.getRole().name())
				.build();
	}

}

