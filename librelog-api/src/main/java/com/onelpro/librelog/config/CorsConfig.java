package com.onelpro.librelog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for cross-origin requests.
 */
@Configuration
public class CorsConfig {

	@Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
	private String allowedOrigins;

	@Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
	private String allowedMethods;

	@Value("${cors.allowed-headers:*}")
	private String allowedHeaders;

	@Value("${cors.allow-credentials:true}")
	private boolean allowCredentials;

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(allowCredentials);
		
		List<String> origins = Arrays.asList(allowedOrigins.split(","));
		config.setAllowedOrigins(origins);
		
		List<String> methods = Arrays.asList(allowedMethods.split(","));
		config.setAllowedMethods(methods);
		
		if ("*".equals(allowedHeaders)) {
			config.addAllowedHeader("*");
		} else {
			List<String> headers = Arrays.asList(allowedHeaders.split(","));
			config.setAllowedHeaders(headers);
		}
		
		config.setMaxAge(3600L);
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

}

