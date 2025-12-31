package com.onelpro.librelog.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * HTTP client service for making requests to LibreTime API.
 * Provides low-level HTTP operations with authentication, timeout handling, and retry logic.
 */
@Service
public class LibreTimeHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeHttpClient.class);

	private WebClient webClient;
	private String baseUrl;
	private String jwtToken;
	private Duration timeout;
	private int maxRetries;

	/**
	 * Default constructor. WebClient must be configured using setBaseUrl() and setJwtToken().
	 */
	public LibreTimeHttpClient() {
		this.timeout = Duration.ofSeconds(30);
		this.maxRetries = 3;
		buildWebClient();
	}

	/**
	 * Sets the base URL for the LibreTime API.
	 * 
	 * @param baseUrl The base URL (e.g., "https://studio-qa.gayphx.com/api/v2/")
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		buildWebClient();
	}

	/**
	 * Sets the JWT token for authentication.
	 * 
	 * @param jwtToken The JWT authentication token
	 */
	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
		buildWebClient();
	}

	/**
	 * Sets the request timeout.
	 * 
	 * @param timeout The timeout duration
	 */
	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
		buildWebClient();
	}

	/**
	 * Sets the maximum number of retries for failed requests.
	 * 
	 * @param maxRetries Maximum number of retries
	 */
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	/**
	 * Builds or rebuilds the WebClient with current configuration.
	 */
	private void buildWebClient() {
		WebClient.Builder builder = WebClient.builder()
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

		if (baseUrl != null && !baseUrl.isEmpty()) {
			builder.baseUrl(baseUrl);
		}

		if (jwtToken != null && !jwtToken.isEmpty()) {
			builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
		}

		this.webClient = builder.build();
	}

	/**
	 * Validates the API base URL format.
	 * 
	 * @param url The URL to validate
	 * @return true if URL format is valid, false otherwise
	 */
	public boolean validateBaseUrl(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}
		try {
			java.net.URI uri = java.net.URI.create(url);
			String scheme = uri.getScheme();
			return scheme != null && (scheme.equals("http") || scheme.equals("https"));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Makes a GET request to the specified URI.
	 * 
	 * @param uri The URI path (relative to base URL)
	 * @return Response body as String
	 */
	public reactor.core.publisher.Mono<String> get(String uri) {
		logger.debug("GET request to: {}", uri);
		long startTime = System.currentTimeMillis();

		return webClient.get()
				.uri(uri)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(timeout)
				.retryWhen(createRetrySpec())
				.doOnSuccess(response -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.info("GET {} completed successfully in {}ms", uri, duration);
					logger.debug("Response: {}", maskSensitiveData(response));
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.error("GET {} failed after {}ms: {}", uri, duration, maskSensitiveData(error.getMessage()));
				});
	}

	/**
	 * Makes a POST request to the specified URI with a JSON body.
	 * 
	 * @param uri The URI path (relative to base URL)
	 * @param body The request body (JSON string)
	 * @return Response body as String
	 */
	public reactor.core.publisher.Mono<String> post(String uri, String body) {
		logger.debug("POST request to: {}", uri);
		long startTime = System.currentTimeMillis();

		return webClient.post()
				.uri(uri)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(timeout)
				.retryWhen(createRetrySpec())
				.doOnSuccess(response -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.info("POST {} completed successfully in {}ms", uri, duration);
					logger.debug("Request body: {}", maskSensitiveData(body));
					logger.debug("Response: {}", maskSensitiveData(response));
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.error("POST {} failed after {}ms: {}", uri, duration, maskSensitiveData(error.getMessage()));
					logger.debug("Request body: {}", maskSensitiveData(body));
				});
	}

	/**
	 * Makes a PUT request to the specified URI with a JSON body.
	 * 
	 * @param uri The URI path (relative to base URL)
	 * @param body The request body (JSON string)
	 * @return Response body as String
	 */
	public reactor.core.publisher.Mono<String> put(String uri, String body) {
		logger.debug("PUT request to: {}", uri);
		long startTime = System.currentTimeMillis();

		return webClient.put()
				.uri(uri)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(timeout)
				.retryWhen(createRetrySpec())
				.doOnSuccess(response -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.info("PUT {} completed successfully in {}ms", uri, duration);
					logger.debug("Request body: {}", maskSensitiveData(body));
					logger.debug("Response: {}", maskSensitiveData(response));
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.error("PUT {} failed after {}ms: {}", uri, duration, maskSensitiveData(error.getMessage()));
					logger.debug("Request body: {}", maskSensitiveData(body));
				});
	}

	/**
	 * Makes a DELETE request to the specified URI.
	 * 
	 * @param uri The URI path (relative to base URL)
	 * @return Response body as String
	 */
	public reactor.core.publisher.Mono<String> delete(String uri) {
		logger.debug("DELETE request to: {}", uri);
		long startTime = System.currentTimeMillis();

		return webClient.delete()
				.uri(uri)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(timeout)
				.retryWhen(createRetrySpec())
				.doOnSuccess(response -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.info("DELETE {} completed successfully in {}ms", uri, duration);
					logger.debug("Response: {}", maskSensitiveData(response));
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.error("DELETE {} failed after {}ms: {}", uri, duration, maskSensitiveData(error.getMessage()));
				});
	}

	/**
	 * Makes a PATCH request to the specified URI with a JSON body.
	 * 
	 * @param uri The URI path (relative to base URL)
	 * @param body The request body (JSON string)
	 * @return Response body as String
	 */
	public reactor.core.publisher.Mono<String> patch(String uri, String body) {
		logger.debug("PATCH request to: {}", uri);
		long startTime = System.currentTimeMillis();

		return webClient.patch()
				.uri(uri)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(timeout)
				.retryWhen(createRetrySpec())
				.doOnSuccess(response -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.info("PATCH {} completed successfully in {}ms", uri, duration);
					logger.debug("Request body: {}", maskSensitiveData(body));
					logger.debug("Response: {}", maskSensitiveData(response));
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					logger.error("PATCH {} failed after {}ms: {}", uri, duration, maskSensitiveData(error.getMessage()));
					logger.debug("Request body: {}", maskSensitiveData(body));
				});
	}

	/**
	 * Creates a retry specification for handling transient failures.
	 * 
	 * @return Retry specification
	 */
	private reactor.util.retry.Retry createRetrySpec() {
		return reactor.util.retry.Retry.fixedDelay(maxRetries, Duration.ofSeconds(2))
				.filter(throwable -> {
					if (throwable instanceof WebClientResponseException) {
						WebClientResponseException ex = (WebClientResponseException) throwable;
						// Retry on server errors (5xx) and timeouts, but not on client errors (4xx)
						return ex.getStatusCode().is5xxServerError() ||
								ex.getStatusCode() == HttpStatus.REQUEST_TIMEOUT ||
								ex.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT;
					}
					// Retry on network errors
					return throwable instanceof WebClientRequestException;
				})
				.doBeforeRetry(retrySignal ->
						logger.warn("Retrying request (attempt {}/{}): {}",
								retrySignal.totalRetries() + 1,
								maxRetries,
								maskSensitiveData(retrySignal.failure().getMessage())));
	}

	/**
	 * Tests the connection to LibreTime API.
	 * Attempts to connect to a health check or status endpoint.
	 * 
	 * @param healthCheckUri The URI path for health check (default: "/api/v2/status" or "/health")
	 * @return true if connection is successful, false otherwise
	 */
	public reactor.core.publisher.Mono<Boolean> testConnection(String healthCheckUri) {
		if (healthCheckUri == null || healthCheckUri.isEmpty()) {
			healthCheckUri = "/api/v2/status";
		}
		logger.debug("Testing LibreTime connection: {}", baseUrl);

		return get(healthCheckUri)
				.timeout(Duration.ofSeconds(5))
				.map(response -> {
					logger.info("LibreTime connection test successful");
					return true;
				})
				.onErrorReturn(false)
				.doOnError(error -> logger.warn("LibreTime connection test failed: {}", maskSensitiveData(error.getMessage())));
	}

	/**
	 * Tests the connection to LibreTime API using default health check endpoint.
	 * 
	 * @return true if connection is successful, false otherwise
	 */
	public reactor.core.publisher.Mono<Boolean> testConnection() {
		return testConnection("/api/v2/status");
	}

	/**
	 * Masks sensitive information in log messages (JWT tokens, passwords, etc.).
	 * 
	 * @param data The data to mask
	 * @return Masked data
	 */
	private String maskSensitiveData(String data) {
		if (data == null) {
			return null;
		}
		// Mask JWT tokens (Bearer tokens)
		String masked = data.replaceAll("Bearer\\s+[A-Za-z0-9_-]+", "Bearer ***");
		// Mask potential passwords
		masked = masked.replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
		masked = masked.replaceAll("(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
		masked = masked.replaceAll("(\"secret\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
		return masked;
	}

}

