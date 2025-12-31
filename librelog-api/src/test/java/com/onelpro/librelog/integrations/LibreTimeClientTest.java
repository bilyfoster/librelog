package com.onelpro.librelog.integrations;

import com.onelpro.librelog.config.LibreTimeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LibreTimeClient.
 */
@ExtendWith(MockitoExtension.class)
class LibreTimeClientTest {

	@Mock
	private LibreTimeConfig config;

	@Mock
	private LibreTimeHttpClient httpClient;

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
		LibreTimeClient client = new LibreTimeClient(config, httpClient);
		assertNotNull(client);
	}

	@Test
	void testConstructor_WithNullBaseUrl_ExpectUsesDefault() {
		when(config.getBaseUrl()).thenReturn(null);
		LibreTimeClient client = new LibreTimeClient(config, httpClient);
		assertNotNull(client);
	}

	@Test
	void testConstructor_WithEmptyBaseUrl_ExpectUsesDefault() {
		when(config.getBaseUrl()).thenReturn("");
		LibreTimeClient client = new LibreTimeClient(config, httpClient);
		assertNotNull(client);
	}

	@Test
	void testExportClock_ExpectReturnsMono() {
		when(httpClient.post(anyString(), anyString())).thenReturn(Mono.just("{\"success\":true}"));
		LibreTimeClient client = new LibreTimeClient(config, httpClient);
		Mono<String> result = client.exportClock("{\"test\":\"data\"}");
		assertNotNull(result);
		// Verify it returns a Mono
		StepVerifier.create(result)
				.expectNext("{\"success\":true}")
				.verifyComplete();
	}

	@Test
	void testPushLog_ExpectReturnsMono() {
		when(httpClient.post(anyString(), anyString())).thenReturn(Mono.just("{\"success\":true}"));
		LibreTimeClient client = new LibreTimeClient(config, httpClient);
		Mono<String> result = client.pushLog("{\"test\":\"data\"}");
		assertNotNull(result);
		// Verify it returns a Mono
		StepVerifier.create(result)
				.expectNext("{\"success\":true}")
				.verifyComplete();
	}

	@Test
	void testTestConnection_ExpectReturnsMono() {
		when(httpClient.testConnection(anyString())).thenReturn(Mono.just(true));
		LibreTimeClient client = new LibreTimeClient(config, httpClient);
		Mono<Boolean> result = client.testConnection();
		assertNotNull(result);
		// Verify it returns true
		StepVerifier.create(result)
				.expectNext(true)
				.verifyComplete();
	}


}
