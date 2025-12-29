package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuthService;
import com.onelpro.librelog.services.JwtService;
import com.onelpro.librelog.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service implementation for authentication operations.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Invalid username or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", loginRequest.getUsername());
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Update last login
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        // Generate tokens
        java.util.Set<String> roleNames = user.getRoles().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());
        
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getUsername(),
                roleNames);

        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());

        // Get user response DTO
        UserResponseDTO userResponse = userService.getUserById(user.getId());

        log.info("Login successful for user: {}", user.getUsername());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }
}

