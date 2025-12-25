package com.onelpro.librelog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter filter;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @Test
    void doFilterInternal_When_WithinRateLimit_Expect_RequestProceeded() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_When_ExceedsRateLimit_Expect_RateLimitError() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(response.getWriter()).thenReturn(printWriter);

        // Consume all tokens to exceed rate limit (100 tokens + 1 more)
        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        // This should exceed the limit
        filter.doFilterInternal(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        verify(response, atLeastOnce()).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("Rate limit exceeded"));
    }

    @Test
    void doFilterInternal_When_XForwardedForHeaderPresent_Expect_ClientIdFromHeader() throws ServletException, IOException {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(request).getHeader("X-Forwarded-For");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_When_NoXForwardedForHeader_Expect_ClientIdFromRemoteAddr() throws ServletException, IOException {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.3");

        filter.doFilterInternal(request, response, filterChain);

        verify(request).getRemoteAddr();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_When_EmptyXForwardedForHeader_Expect_ClientIdFromRemoteAddr() throws ServletException, IOException {
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("192.168.1.4");

        filter.doFilterInternal(request, response, filterChain);

        verify(request).getRemoteAddr();
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

