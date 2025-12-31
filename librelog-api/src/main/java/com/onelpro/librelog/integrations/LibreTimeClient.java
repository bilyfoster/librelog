package com.onelpro.librelog.integrations;

import com.onelpro.librelog.config.LibreTimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Client for interacting with LibreTime API.
 * Handles authentication, clock export, and log push operations.
 * Uses LibreTimeHttpClient for HTTP operations.
 */
@Component
public class LibreTimeClient {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeClient.class);

	private final LibreTimeHttpClient httpClient;
	private final LibreTimeConfig config;

	public LibreTimeClient(LibreTimeConfig config, LibreTimeHttpClient httpClient) {
		this.config = config;
		this.httpClient = httpClient;
		
		// Configure HTTP client with config values
		if (config.getBaseUrl() != null) {
			this.httpClient.setBaseUrl(config.getBaseUrl());
		}
		if (config.getApiKey() != null) {
			this.httpClient.setJwtToken(config.getApiKey());
		}
		if (config.getTimeoutSeconds() != null) {
			this.httpClient.setTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
		}
		if (config.getMaxRetries() != null) {
			this.httpClient.setMaxRetries(config.getMaxRetries());
		}
	}

	/**
	 * Exports a clock template to LibreTime.
	 * 
	 * @param clockData The clock data in LibreTime format (JSON)
	 * @return Response from LibreTime API
	 */
	public reactor.core.publisher.Mono<String> exportClock(String clockData) {
		logger.info("Exporting clock to LibreTime: {}", config.getBaseUrl());
		return httpClient.post("/api/v2/show-instances", clockData)
				.doOnSuccess(response -> logger.info("Clock exported successfully to LibreTime"))
				.doOnError(error -> logger.error("Failed to export clock to LibreTime: {}", error.getMessage()));
	}

	/**
	 * Pushes a log (schedule) to LibreTime.
	 * 
	 * @param logData The log data in LibreTime format (JSON)
	 * @return Response from LibreTime API
	 */
	public reactor.core.publisher.Mono<String> pushLog(String logData) {
		logger.info("Pushing log to LibreTime: {}", config.getBaseUrl());
		return httpClient.post("/api/v2/show-instances", logData)
				.doOnSuccess(response -> logger.info("Log pushed successfully to LibreTime"))
				.doOnError(error -> logger.error("Failed to push log to LibreTime: {}", error.getMessage()));
	}

	/**
	 * Tests the connection to LibreTime API.
	 * 
	 * @return true if connection is successful, false otherwise
	 */
	public reactor.core.publisher.Mono<Boolean> testConnection() {
		logger.debug("Testing LibreTime connection: {}", config.getBaseUrl());
		return httpClient.testConnection("/api/v2/status");
	}

}

