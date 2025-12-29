package com.onelpro.librelog.config;

import com.onelpro.librelog.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter that processes JWT tokens in request headers.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// Completely skip JWT filter for static resources and public endpoints
		String path = request.getRequestURI();
		if (path == null) {
			return false;
		}
		return path.equals("/") ||
				path.endsWith(".html") ||
				path.startsWith("/api/auth/") ||
				path.startsWith("/swagger-ui/") ||
				path.startsWith("/api-docs/") ||
				path.startsWith("/actuator/") ||
				path.startsWith("/css/") ||
				path.startsWith("/js/") ||
				path.startsWith("/images/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String authHeader = request.getHeader(AUTHORIZATION_HEADER);

		if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(BEARER_PREFIX.length());

		if (jwtService.isTokenValid(token)) {
			String userId = jwtService.extractUserId(token);
			String email = jwtService.extractEmail(token);
			String role = jwtService.extractRole(token);

			if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						userId,
						null,
						Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
				);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
				logger.debug("Authenticated user: {} with role: {}", email, role);
			}
		}

		filterChain.doFilter(request, response);
	}

}

