package com.onelpro.librelog.config;

import com.onelpro.librelog.services.JwtTokenService;
import com.onelpro.librelog.services.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private UUID testUserId;

    @BeforeEach
    void setUp() throws Exception {
        filter = new JwtAuthenticationFilter(jwtTokenService, tokenBlacklistService);
        testUserId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_When_NoAuthorizationHeader_Expect_ChainProceeded() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_When_InvalidAuthorizationHeader_Expect_ChainProceeded() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_When_BlacklistedToken_Expect_ChainProceeded() throws Exception {
        String token = "blacklisted-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_When_ValidAccessToken_Expect_AuthenticationSet() throws Exception {
        String token = "valid-access-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenService.getUserIdFromToken(token)).thenReturn(testUserId);
        when(jwtTokenService.getUsernameFromToken(token)).thenReturn("testuser");
        when(jwtTokenService.getRoleFromToken(token)).thenReturn("ADMIN");
        when(jwtTokenService.isRefreshToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(testUserId.toString(), SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilterInternal_When_RefreshToken_Expect_NoAuthentication() throws Exception {
        String token = "refresh-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenService.getUserIdFromToken(token)).thenReturn(testUserId);
        when(jwtTokenService.getUsernameFromToken(token)).thenReturn("testuser");
        when(jwtTokenService.getRoleFromToken(token)).thenReturn("ADMIN");
        when(jwtTokenService.isRefreshToken(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_When_InvalidToken_Expect_NoAuthentication() throws Exception {
        String token = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenService.getUserIdFromToken(token)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_When_MissingUserId_Expect_NoAuthentication() throws Exception {
        String token = "token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtTokenService.getUserIdFromToken(token)).thenReturn(null);
        when(jwtTokenService.getUsernameFromToken(token)).thenReturn("testuser");
        when(jwtTokenService.getRoleFromToken(token)).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

