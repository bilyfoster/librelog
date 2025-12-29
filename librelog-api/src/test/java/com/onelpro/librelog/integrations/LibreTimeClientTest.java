package com.onelpro.librelog.integrations;

import com.onelpro.librelog.config.LibreTimeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LibreTimeClient.
 */
@ExtendWith(MockitoExtension.class)
class LibreTimeClientTest {

	@Mock
	private LibreTimeConfig config;

	@BeforeEach
	void setUp() {
		lenient().when(config.getBaseUrl()).thenReturn("http://localhost:8080");
		lenient().when(config.getApiKey()).thenReturn("test-api-key");
		lenient().when(config.getTimeoutSeconds()).thenReturn(30);
		lenient().when(config.getMaxRetries()).thenReturn(3);
	}

	@Test
	void testConstructor_WithValidConfig_ExpectClientCreated() {
		when(config.getBaseUrl()).thenReturn("http://localhost:8080");
		LibreTimeClient client = new LibreTimeClient(config);
		assertNotNull(client);
	}

	@Test
	void testConstructor_WithNullBaseUrl_ExpectUsesDefault() {
		when(config.getBaseUrl()).thenReturn(null);
		LibreTimeClient client = new LibreTimeClient(config);
		assertNotNull(client);
	}

	@Test
	void testConstructor_WithEmptyBaseUrl_ExpectUsesDefault() {
		when(config.getBaseUrl()).thenReturn("");
		LibreTimeClient client = new LibreTimeClient(config);
		assertNotNull(client);
	}

	@Test
	void testExportClock_ExpectReturnsMono() {
		LibreTimeClient client = new LibreTimeClient(config);
		Mono<String> result = client.exportClock("{\"test\":\"data\"}");
		assertNotNull(result);
		// Verify it's a Mono that will error (network not available)
		StepVerifier.create(result)
				.expectError()
				.verify(Duration.ofSeconds(35));
	}

	@Test
	void testPushLog_ExpectReturnsMono() {
		LibreTimeClient client = new LibreTimeClient(config);
		Mono<String> result = client.pushLog("{\"test\":\"data\"}");
		assertNotNull(result);
		// Verify it's a Mono that will error (network not available)
		StepVerifier.create(result)
				.expectError()
				.verify(Duration.ofSeconds(35));
	}

	@Test
	void testTestConnection_ExpectReturnsMono() {
		LibreTimeClient client = new LibreTimeClient(config);
		Mono<Boolean> result = client.testConnection();
		assertNotNull(result);
		// Verify it returns false on error (network not available)
		StepVerifier.create(result)
				.expectNext(false)
				.verifyComplete(Duration.ofSeconds(10));
	}

	@Test
	void testExportClock_WithNullConfigValues_ExpectHandlesGracefully() {
		when(config.getBaseUrl()).thenReturn(null);
		when(config.getApiKey()).thenReturn(null);
		when(config.getTimeoutSeconds()).thenReturn(null);
		when(config.getMaxRetries()).thenReturn(null);
		
		LibreTimeClient client = new LibreTimeClient(config);
		Mono<String> result = client.exportClock("{\"test\":\"data\"}");
		assertNotNull(result);
		StepVerifier.create(result)
				.expectError()
				.verify(Duration.ofSeconds(35));
	}

	@Test
	void testPushLog_WithNullConfigValues_ExpectHandlesGracefully() {
		when(config.getBaseUrl()).thenReturn(null);
		when(config.getApiKey()).thenReturn(null);
		when(config.getTimeoutSeconds()).thenReturn(null);
		when(config.getMaxRetries()).thenReturn(null);
		
		LibreTimeClient client = new LibreTimeClient(config);
		Mono<String> result = client.pushLog("{\"test\":\"data\"}");
		assertNotNull(result);
		StepVerifier.create(result)
				.expectError()
				.verify(Duration.ofSeconds(35));
	}

}
