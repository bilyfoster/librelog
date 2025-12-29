package com.onelpro.librelog.services;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private static final String TEST_SECRET = "test-secret-key-minimum-256-bits-for-hmac-sha-256-algorithm-required";
    private UUID testUserId;
    private String testUsername;
    private String testRole;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(TEST_SECRET, 60, 7);
        testUserId = UUID.randomUUID();
        testUsername = "testuser";
        testRole = "ADMIN";
    }

    @Test
    void generateAccessToken_When_ValidInput_Expect_NonEmptyToken() {
        String token = jwtTokenService.generateAccessToken(testUserId, testUsername, testRole);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateRefreshToken_When_ValidInput_Expect_NonEmptyToken() {
        String token = jwtTokenService.generateRefreshToken(testUserId);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_When_ValidAccessToken_Expect_ClaimsReturned() {
        String token = jwtTokenService.generateAccessToken(testUserId, testUsername, testRole);
        Claims claims = jwtTokenService.validateToken(token);
        assertNotNull(claims);
        assertEquals(testUserId.toString(), claims.getSubject());
        assertEquals(testUsername, claims.get("username", String.class));
        assertEquals(testRole, claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void validateToken_When_ValidRefreshToken_Expect_ClaimsReturned() {
        String token = jwtTokenService.generateRefreshToken(testUserId);
        Claims claims = jwtTokenService.validateToken(token);
        assertNotNull(claims);
        assertEquals(testUserId.toString(), claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void validateToken_When_InvalidToken_Expect_Null() {
        String invalidToken = "invalid.token.here";
        Claims claims = jwtTokenService.validateToken(invalidToken);
        assertNull(claims);
    }

    @Test
    void validateToken_When_EmptyToken_Expect_Null() {
        Claims claims = jwtTokenService.validateToken("");
        assertNull(claims);
    }

    @Test
    void validateToken_When_NullToken_Expect_Null() {
        Claims claims = jwtTokenService.validateToken(null);
        assertNull(claims);
    }

    @Test
    void getUserIdFromToken_When_ValidToken_Expect_UserId() {
        String token = jwtTokenService.generateAccessToken(testUserId, testUsername, testRole);
        UUID userId = jwtTokenService.getUserIdFromToken(token);
        assertEquals(testUserId, userId);
    }

    @Test
    void getUserIdFromToken_When_InvalidToken_Expect_Null() {
        UUID userId = jwtTokenService.getUserIdFromToken("invalid.token");
        assertNull(userId);
    }

    @Test
    void getUsernameFromToken_When_ValidToken_Expect_Username() {
        String token = jwtTokenService.generateAccessToken(testUserId, testUsername, testRole);
        String username = jwtTokenService.getUsernameFromToken(token);
        assertEquals(testUsername, username);
    }

    @Test
    void getUsernameFromToken_When_InvalidToken_Expect_Null() {
        String username = jwtTokenService.getUsernameFromToken("invalid.token");
        assertNull(username);
    }

    @Test
    void getRoleFromToken_When_ValidToken_Expect_Role() {
        String token = jwtTokenService.generateAccessToken(testUserId, testUsername, testRole);
        String role = jwtTokenService.getRoleFromToken(token);
        assertEquals(testRole, role);
    }

    @Test
    void getRoleFromToken_When_InvalidToken_Expect_Null() {
        String role = jwtTokenService.getRoleFromToken("invalid.token");
        assertNull(role);
    }

    @Test
    void isRefreshToken_When_RefreshToken_Expect_True() {
        String token = jwtTokenService.generateRefreshToken(testUserId);
        assertTrue(jwtTokenService.isRefreshToken(token));
    }

    @Test
    void isRefreshToken_When_AccessToken_Expect_False() {
        String token = jwtTokenService.generateAccessToken(testUserId, testUsername, testRole);
        assertFalse(jwtTokenService.isRefreshToken(token));
    }

    @Test
    void isRefreshToken_When_InvalidToken_Expect_False() {
        assertFalse(jwtTokenService.isRefreshToken("invalid.token"));
    }
}

