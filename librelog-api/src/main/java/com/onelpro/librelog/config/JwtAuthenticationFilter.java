package com.onelpro.librelog.config;

import com.onelpro.librelog.services.JwtService;
import com.onelpro.librelog.services.PermissionService;
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
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter that processes JWT tokens in request headers.
 * Includes user's station assignments in the security context for permission checking.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final PermissionService permissionService;

	public JwtAuthenticationFilter(JwtService jwtService, PermissionService permissionService) {
		this.jwtService = jwtService;
		this.permissionService = permissionService;
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
			String userIdStr = jwtService.extractUserId(token);
			String email = jwtService.extractEmail(token);
			String role = jwtService.extractRole(token);

			if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				try {
					UUID userId = UUID.fromString(userIdStr);
					
					// Get user's station assignments for permission checking
					List<UUID> stationIds = permissionService.getUserStations(userId);
					
					// Create authentication token with user ID, role, and station assignments
					UsernamePasswordAuthenticationToken authToken = 
							new UsernamePasswordAuthenticationToken(
									userIdStr,
									null,
									Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
							);
					
					// Store station assignments in authentication details
					UserAuthenticationDetails details = new UserAuthenticationDetails(
							userId,
							email,
							role,
							stationIds,
							request
					);
					authToken.setDetails(details);
					
					SecurityContextHolder.getContext().setAuthentication(authToken);
					logger.debug("Authenticated user: {} with role: {} and {} station assignments", 
							email, role, stationIds.size());
				} catch (IllegalArgumentException e) {
					logger.warn("Invalid user ID format in JWT token: {}", userIdStr);
				}
			}
		}

		filterChain.doFilter(request, response);
	}

}

