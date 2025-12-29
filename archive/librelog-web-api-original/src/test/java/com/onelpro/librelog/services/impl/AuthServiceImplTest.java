package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.exceptions.ConflictException;
import com.onelpro.librelog.exceptions.UnauthorizedException;
import com.onelpro.librelog.exceptions.ValidationException;
import com.onelpro.librelog.models.FailedLoginAttempt;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.FailedLoginAttemptRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuthSecurityService;
import com.onelpro.librelog.services.JwtTokenService;
import com.onelpro.librelog.services.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FailedLoginAttemptRepository failedLoginAttemptRepository;

    @Mock
    private AuthSecurityService authSecurityService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthServiceImpl authService;
    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                failedLoginAttemptRepository,
                authSecurityService,
                jwtTokenService,
                tokenBlacklistService
        );
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .passwordHash("$2a$10$hashed")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void login_When_ValidCredentials_Expect_Success() {
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .username("testuser")
                .password("password123!")
                .build();

        when(failedLoginAttemptRepository.countByUsernameAndAttemptedAtAfter(anyString(), any()))
                .thenReturn(0L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authSecurityService.verifyPassword("password123!", "$2a$10$hashed")).thenReturn(true);
        when(failedLoginAttemptRepository.findByUsername("testuser")).thenReturn(List.of());
        when(jwtTokenService.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponseDTO response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(testUserId, response.getUserId());
        assertEquals("testuser", response.getUsername());
        verify(authSecurityService).updateLastLogin(testUser);
    }

    @Test
    void login_When_InvalidUsername_Expect_UnauthorizedException() {
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .username("invalid")
                .password("password123!")
                .build();

        when(failedLoginAttemptRepository.countByUsernameAndAttemptedAtAfter(anyString(), any()))
                .thenReturn(0L);
        when(userRepository.findByUsername("invalid")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(failedLoginAttemptRepository).save(any(FailedLoginAttempt.class));
    }

    @Test
    void login_When_InvalidPassword_Expect_UnauthorizedException() {
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(failedLoginAttemptRepository.countByUsernameAndAttemptedAtAfter(anyString(), any()))
                .thenReturn(0L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authSecurityService.verifyPassword("wrongpassword", "$2a$10$hashed")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(failedLoginAttemptRepository).save(any(FailedLoginAttempt.class));
    }

    @Test
    void login_When_TooManyFailedAttempts_Expect_UnauthorizedException() {
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .username("testuser")
                .password("password123!")
                .build();

        when(failedLoginAttemptRepository.countByUsernameAndAttemptedAtAfter(anyString(), any()))
                .thenReturn(5L);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void register_When_ValidRequest_Expect_Success() {
        RegisterRequestDTO registerRequest = RegisterRequestDTO.builder()
                .username("newuser")
                .password("ValidPass123!")
                .role(UserRole.PRODUCER)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(authSecurityService.hashPassword("ValidPass123!")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenService.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponseDTO response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_When_UsernameExists_Expect_ConflictException() {
        RegisterRequestDTO registerRequest = RegisterRequestDTO.builder()
                .username("existinguser")
                .password("ValidPass123!")
                .role(UserRole.PRODUCER)
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_When_InvalidPassword_Expect_ValidationException() {
        RegisterRequestDTO registerRequest = RegisterRequestDTO.builder()
                .username("newuser")
                .password("short")
                .role(UserRole.PRODUCER)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        assertThrows(ValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshToken_When_ValidRefreshToken_Expect_Success() {
        String refreshToken = "valid-refresh-token";

        when(tokenBlacklistService.isTokenBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenService.getUserIdFromToken(refreshToken)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtTokenService.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("new-access-token");
        when(jwtTokenService.generateRefreshToken(any())).thenReturn("new-refresh-token");

        AuthResponseDTO response = authService.refreshToken(refreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        verify(tokenBlacklistService).blacklistToken(refreshToken);
    }

    @Test
    void refreshToken_When_BlacklistedToken_Expect_UnauthorizedException() {
        String refreshToken = "blacklisted-token";

        when(tokenBlacklistService.isTokenBlacklisted(refreshToken)).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void refreshToken_When_InvalidToken_Expect_UnauthorizedException() {
        String refreshToken = "invalid-token";

        when(tokenBlacklistService.isTokenBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenService.isRefreshToken(refreshToken)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void logout_When_ValidTokens_Expect_TokensBlacklisted() {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        authService.logout(accessToken, refreshToken);

        verify(tokenBlacklistService).blacklistToken(accessToken);
        verify(tokenBlacklistService).blacklistToken(refreshToken);
    }

    @Test
    void getCurrentUser_When_ValidUserId_Expect_UserResponse() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserResponseDTO response = authService.getCurrentUser(testUserId);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals(UserRole.ADMIN, response.getRole());
    }

    @Test
    void getCurrentUser_When_InvalidUserId_Expect_NotFoundException() {
        UUID invalidId = UUID.randomUUID();
        when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

        assertThrows(com.onelpro.librelog.exceptions.NotFoundException.class,
                () -> authService.getCurrentUser(invalidId));
    }
}

