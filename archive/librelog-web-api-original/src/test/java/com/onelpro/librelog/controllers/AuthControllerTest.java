package com.onelpro.librelog.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.exceptions.GlobalExceptionHandler;
import com.onelpro.librelog.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        testUserId = UUID.randomUUID();
    }

    @Test
    void login_When_ValidRequest_Expect_Success() throws Exception {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("testuser")
                .password("password123!")
                .build();

        AuthResponseDTO response = AuthResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId(testUserId)
                .username("testuser")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void register_When_ValidRequest_Expect_Created() throws Exception {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("newuser")
                .password("ValidPass123!")
                .role(UserRole.PRODUCER)
                .build();

        AuthResponseDTO response = AuthResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId(testUserId)
                .username("newuser")
                .role("PRODUCER")
                .build();

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void refreshToken_When_ValidRequest_Expect_Success() throws Exception {
        AuthController.RefreshTokenRequest request = new AuthController.RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        AuthResponseDTO response = AuthResponseDTO.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .userId(testUserId)
                .username("testuser")
                .role("ADMIN")
                .build();

        when(authService.refreshToken("refresh-token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_When_ValidRequest_Expect_Success() throws Exception {
        AuthController.LogoutRequest request = new AuthController.LogoutRequest();
        request.setAccessToken("access-token");
        request.setRefreshToken("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshToken_When_NullToken_Expect_Unauthorized() throws Exception {
        AuthController.RefreshTokenRequest request = new AuthController.RefreshTokenRequest();
        request.setRefreshToken(null);

        when(authService.refreshToken(null))
                .thenThrow(new com.onelpro.librelog.exceptions.UnauthorizedException("Refresh token is required"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_When_NullTokens_Expect_Success() throws Exception {
        AuthController.LogoutRequest request = new AuthController.LogoutRequest();
        request.setAccessToken(null);
        request.setRefreshToken(null);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenRequest_When_GetterAndSetterCalled_Expect_ValuesSet() {
        AuthController.RefreshTokenRequest request = new AuthController.RefreshTokenRequest();
        request.setRefreshToken("test-token");
        assertEquals("test-token", request.getRefreshToken());
    }

    @Test
    void logoutRequest_When_GetterAndSetterCalled_Expect_ValuesSet() {
        AuthController.LogoutRequest request = new AuthController.LogoutRequest();
        request.setAccessToken("access-token");
        request.setRefreshToken("refresh-token");
        assertEquals("access-token", request.getAccessToken());
        assertEquals("refresh-token", request.getRefreshToken());
    }
}

