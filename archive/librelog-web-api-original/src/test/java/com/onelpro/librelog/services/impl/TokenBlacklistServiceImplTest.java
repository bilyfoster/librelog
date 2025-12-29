package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.services.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    @Mock
    private JwtTokenService jwtTokenService;

    private TokenBlacklistServiceImpl tokenBlacklistService;
    private static final String TEST_SECRET = "test-secret-key-minimum-256-bits-for-hmac-sha-256-algorithm-required";

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistServiceImpl(jwtTokenService);
    }

    @Test
    void blacklistToken_When_ValidToken_Expect_TokenBlacklisted() {
        String token = "test-token";
        tokenBlacklistService.blacklistToken(token);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void blacklistToken_When_NullToken_Expect_NoException() {
        assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(null));
    }

    @Test
    void blacklistToken_When_EmptyToken_Expect_NoException() {
        assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(""));
    }

    @Test
    void isTokenBlacklisted_When_TokenNotBlacklisted_Expect_False() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted("not-blacklisted-token"));
    }

    @Test
    void isTokenBlacklisted_When_TokenBlacklisted_Expect_True() {
        String token = "blacklisted-token";
        tokenBlacklistService.blacklistToken(token);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void isTokenBlacklisted_When_NullToken_Expect_False() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
    }

    @Test
    void isTokenBlacklisted_When_EmptyToken_Expect_False() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""));
    }

    @Test
    void cleanupExpiredTokens_When_ExpiredTokensExist_Expect_TokensRemoved() {
        JwtTokenService realJwtService = new JwtTokenService(TEST_SECRET, 60, 7);
        TokenBlacklistServiceImpl service = new TokenBlacklistServiceImpl(realJwtService);
        
        // Add a valid token
        String validToken = realJwtService.generateAccessToken(UUID.randomUUID(), "user", "ADMIN");
        service.blacklistToken(validToken);
        
        // Add an invalid/expired token
        service.blacklistToken("invalid-token");
        
        // Cleanup should remove invalid token
        service.cleanupExpiredTokens();
        
        // Valid token should still be blacklisted
        assertTrue(service.isTokenBlacklisted(validToken));
        // Invalid token should be removed
        assertFalse(service.isTokenBlacklisted("invalid-token"));
    }

    @Test
    void getBlacklistedTokens_When_TokensBlacklisted_Expect_AllTokensReturned() {
        String token1 = "token1";
        String token2 = "token2";
        
        tokenBlacklistService.blacklistToken(token1);
        tokenBlacklistService.blacklistToken(token2);
        
        Set<String> blacklisted = tokenBlacklistService.getBlacklistedTokens();
        assertEquals(2, blacklisted.size());
        assertTrue(blacklisted.contains(token1));
        assertTrue(blacklisted.contains(token2));
    }

    @Test
    void getBlacklistedTokens_When_NoTokensBlacklisted_Expect_EmptySet() {
        Set<String> blacklisted = tokenBlacklistService.getBlacklistedTokens();
        assertTrue(blacklisted.isEmpty());
    }

    @Test
    void getBlacklistedTokens_When_Called_Expect_ImmutableSet() {
        tokenBlacklistService.blacklistToken("token1");
        Set<String> blacklisted = tokenBlacklistService.getBlacklistedTokens();
        assertThrows(UnsupportedOperationException.class, () -> blacklisted.add("token2"));
    }
}

