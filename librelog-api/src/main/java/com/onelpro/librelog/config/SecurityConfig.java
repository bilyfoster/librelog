package com.onelpro.librelog.config;

import com.onelpro.librelog.config.JwtAuthenticationFilter;
import com.onelpro.librelog.security.LibreLogPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security configuration with permission-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final LibreLogPermissionEvaluator permissionEvaluator;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
	                     LibreLogPermissionEvaluator permissionEvaluator) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.permissionEvaluator = permissionEvaluator;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// Single security filter chain - static resources first, then API
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						// Static resources - MUST be first, evaluated in order
						// Explicitly permit dashboard.html and index.html
						.requestMatchers("/", "/index.html", "/dashboard.html").permitAll()
						.requestMatchers("/*.html", "/**/*.html").permitAll()
						.requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
						.requestMatchers("/favicon.ico").permitAll()
						// Public API endpoints
						.requestMatchers("/api/auth/**").permitAll()
						// Admin-only endpoints
						.requestMatchers("/api/users/**").hasRole("ADMIN")
						// All other API endpoints require authentication
						.requestMatchers("/api/**").authenticated()
						// Everything else is public (fallback)
						.anyRequest().permitAll()
				)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}


	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	/**
	 * Configures method security expression handler to use custom permission evaluator.
	 */
	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
		handler.setPermissionEvaluator(permissionEvaluator);
		return handler;
	}

}

