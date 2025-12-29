package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.exceptions.ConflictException;
import com.onelpro.librelog.exceptions.UnauthorizedException;
import com.onelpro.librelog.models.FailedLoginAttempt;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.FailedLoginAttemptRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuthSecurityService;
import com.onelpro.librelog.services.AuthService;
import com.onelpro.librelog.services.JwtTokenService;
import com.onelpro.librelog.services.TokenBlacklistService;
import com.onelpro.librelog.utils.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of AuthService for authentication operations.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final FailedLoginAttemptRepository failedLoginAttemptRepository;
    private final AuthSecurityService authSecurityService;
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(
            UserRepository userRepository,
            FailedLoginAttemptRepository failedLoginAttemptRepository,
            AuthSecurityService authSecurityService,
            JwtTokenService jwtTokenService,
            TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.failedLoginAttemptRepository = failedLoginAttemptRepository;
        this.authSecurityService = authSecurityService;
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        // Check for too many failed login attempts
        checkFailedLoginAttempts(username);

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    recordFailedLoginAttempt(username, null, null);
                    return new UnauthorizedException("Invalid username or password");
                });

        // Verify password
        if (!authSecurityService.verifyPassword(password, user.getPasswordHash())) {
            recordFailedLoginAttempt(username, null, null);
            throw new UnauthorizedException("Invalid username or password");
        }

        // Clear failed login attempts on successful login
        clearFailedLoginAttempts(username);

        // Update last login
        authSecurityService.updateLastLogin(user);

        // Generate tokens
        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        logger.info("User logged in successfully: {}", username);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        UserRole role = registerRequest.getRole();

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }

        // Validate password
        List<String> passwordErrors = PasswordValidator.validate(password);
        if (!passwordErrors.isEmpty()) {
            throw new com.onelpro.librelog.exceptions.ValidationException(
                    "Password validation failed: " + String.join(", ", passwordErrors),
                    passwordErrors
            );
        }

        // Create new user
        User user = User.builder()
                .username(username)
                .passwordHash(authSecurityService.hashPassword(password))
                .role(role)
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);

        logger.info("New user registered: {}", username);

        // Generate tokens
        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new UnauthorizedException("Token has been revoked");
        }

        // Validate refresh token
        if (!jwtTokenService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId = jwtTokenService.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Generate new tokens
        String newAccessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        String newRefreshToken = jwtTokenService.generateRefreshToken(user.getId());

        // Blacklist old refresh token
        tokenBlacklistService.blacklistToken(refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            tokenBlacklistService.blacklistToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tokenBlacklistService.blacklistToken(refreshToken);
        }
        logger.debug("User logged out");
    }

    @Override
    public UserResponseDTO getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.onelpro.librelog.exceptions.NotFoundException("User not found"));

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    private void checkFailedLoginAttempts(String username) {
        Instant lockoutThreshold = Instant.now().minus(LOCKOUT_DURATION_MINUTES, java.time.temporal.ChronoUnit.MINUTES);
        long failedAttempts = failedLoginAttemptRepository.countByUsernameAndAttemptedAtAfter(username, lockoutThreshold);

        if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            throw new UnauthorizedException("Too many failed login attempts. Please try again later.");
        }
    }

    private void recordFailedLoginAttempt(String username, String ipAddress, String userAgent) {
        FailedLoginAttempt attempt = FailedLoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .attemptedAt(Instant.now())
                .build();
        failedLoginAttemptRepository.save(attempt);
        logger.warn("Failed login attempt recorded for username: {}", username);
    }

    private void clearFailedLoginAttempts(String username) {
        List<FailedLoginAttempt> attempts = failedLoginAttemptRepository.findByUsername(username);
        if (!attempts.isEmpty()) {
            failedLoginAttemptRepository.deleteAll(attempts);
            logger.debug("Cleared failed login attempts for username: {}", username);
        }
    }
}

