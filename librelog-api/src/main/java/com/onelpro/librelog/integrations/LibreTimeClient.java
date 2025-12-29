package com.onelpro.librelog.integrations;

import com.onelpro.librelog.config.LibreTimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Client for interacting with LibreTime API.
 * Handles authentication, clock export, and log push operations.
 */
@Component
public class LibreTimeClient {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeClient.class);

	private final WebClient webClient;
	private final LibreTimeConfig config;

	public LibreTimeClient(LibreTimeConfig config) {
		this.config = config;
		this.webClient = WebClient.builder()
				.baseUrl(config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:8080")
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	/**
	 * Exports a clock template to LibreTime.
	 * 
	 * @param clockData The clock data in LibreTime format (JSON)
	 * @return Response from LibreTime API
	 */
	public Mono<String> exportClock(String clockData) {
		logger.info("Exporting clock to LibreTime: {}", config.getBaseUrl());

		return webClient.post()
				.uri("/api/v2/show-instances")
				.header("Authorization", "Bearer " + config.getApiKey())
				.bodyValue(clockData)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
				.retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofSeconds(2))
						.filter(throwable -> {
							if (throwable instanceof WebClientResponseException) {
								WebClientResponseException ex = (WebClientResponseException) throwable;
								// Retry on server errors (5xx) and timeouts, but not on client errors (4xx)
								return ex.getStatusCode().is5xxServerError() || 
									   ex.getStatusCode() == HttpStatus.REQUEST_TIMEOUT ||
									   ex.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT;
							}
							return true; // Retry on other exceptions (network errors, etc.)
						})
						.doBeforeRetry(retrySignal -> 
							logger.warn("Retrying LibreTime export (attempt {}/{}): {}", 
									retrySignal.totalRetries() + 1, 
									config.getMaxRetries(), 
									retrySignal.failure().getMessage())))
				.doOnSuccess(response -> logger.info("Clock exported successfully to LibreTime"))
				.doOnError(error -> logger.error("Failed to export clock to LibreTime: {}", error.getMessage()));
	}

	/**
	 * Pushes a log (schedule) to LibreTime.
	 * 
	 * @param logData The log data in LibreTime format (JSON)
	 * @return Response from LibreTime API
	 */
	public Mono<String> pushLog(String logData) {
		logger.info("Pushing log to LibreTime: {}", config.getBaseUrl());

		return webClient.post()
				.uri("/api/v2/show-instances")
				.header("Authorization", "Bearer " + config.getApiKey())
				.bodyValue(logData)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
				.retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofSeconds(2))
						.filter(throwable -> {
							if (throwable instanceof WebClientResponseException) {
								WebClientResponseException ex = (WebClientResponseException) throwable;
								return ex.getStatusCode().is5xxServerError() || 
									   ex.getStatusCode() == HttpStatus.REQUEST_TIMEOUT ||
									   ex.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT;
							}
							return true;
						})
						.doBeforeRetry(retrySignal -> 
							logger.warn("Retrying LibreTime log push (attempt {}/{}): {}", 
									retrySignal.totalRetries() + 1, 
									config.getMaxRetries(), 
									retrySignal.failure().getMessage())))
				.doOnSuccess(response -> logger.info("Log pushed successfully to LibreTime"))
				.doOnError(error -> logger.error("Failed to push log to LibreTime: {}", error.getMessage()));
	}

	/**
	 * Tests the connection to LibreTime API.
	 * 
	 * @return true if connection is successful, false otherwise
	 */
	public Mono<Boolean> testConnection() {
		logger.debug("Testing LibreTime connection: {}", config.getBaseUrl());

		return webClient.get()
				.uri("/api/v2/status")
				.header("Authorization", "Bearer " + config.getApiKey())
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(5))
				.map(response -> {
					logger.info("LibreTime connection test successful");
					return true;
				})
				.onErrorReturn(false)
				.doOnError(error -> logger.warn("LibreTime connection test failed: {}", error.getMessage()));
	}

}

