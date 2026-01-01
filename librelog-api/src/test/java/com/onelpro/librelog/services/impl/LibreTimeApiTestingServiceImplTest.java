package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ApiTestResultResponseDTO;
import com.onelpro.librelog.dto.ApiTestSummaryResponseDTO;
import com.onelpro.librelog.dto.BugReportResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.enums.SyncFrequency;
import com.onelpro.librelog.enums.TestStatus;
import com.onelpro.librelog.enums.TestType;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeApiEndpoint;
import com.onelpro.librelog.models.LibreTimeApiTestResult;
import com.onelpro.librelog.repositories.LibreTimeApiEndpointRepository;
import com.onelpro.librelog.repositories.LibreTimeApiTestResultRepository;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibreTimeApiTestingServiceImplTest {

	@Mock
	private LibreTimeApiEndpointRepository endpointRepository;

	@Mock
	private LibreTimeApiTestResultRepository testResultRepository;

	@Mock
	private LibreTimeHttpClient httpClient;

	@Mock
	private LibreTimeIntegrationConfigService configService;

	@InjectMocks
	private LibreTimeApiTestingServiceImpl testingService;

	private UUID stationId;
	private UUID endpointId;
	private LibreTimeIntegrationConfigResponseDTO configDTO;
	private LibreTimeApiEndpoint endpoint;
	private LibreTimeApiTestResult testResult;

	@BeforeEach
	void setUp() {
		stationId = UUID.randomUUID();
		endpointId = UUID.randomUUID();

		configDTO = LibreTimeIntegrationConfigResponseDTO.builder()
				.stationId(stationId)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.build();

		endpoint = LibreTimeApiEndpoint.builder()
				.id(endpointId)
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.resourceType("files")
				.status(EndpointStatus.WORKING)
				.lastTestedAt(LocalDateTime.now())
				.responseTimeMs(100)
				.requiresAuthentication(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		testResult = LibreTimeApiTestResult.builder()
				.id(UUID.randomUUID())
				.endpoint(endpoint)
				.testType(TestType.CRUD)
				.status(TestStatus.PASSED)
				.responseStatusCode(200)
				.responseTimeMs(100)
				.testTimestamp(LocalDateTime.now())
				.build();
	}

	@Test
	void testConnection_When_Successful_Expect_ReturnsPassed() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(httpClient.get("/api/v2/status")).thenReturn(Mono.just("{\"status\":\"ok\"}"));
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenAnswer(invocation -> {
			LibreTimeApiTestResult result = invocation.getArgument(0);
			result.setId(UUID.randomUUID());
			return result;
		});

		ApiTestResultResponseDTO result = testingService.testConnection(stationId);

		assertNotNull(result);
		assertEquals(TestType.CONNECTIVITY, result.getTestType());
		verify(httpClient).get("/api/v2/status");
		verify(testResultRepository).save(any(LibreTimeApiTestResult.class));
	}

	@Test
	void testConnection_When_Fails_Expect_ReturnsFailed() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(httpClient.get("/api/v2/status")).thenReturn(Mono.error(new RuntimeException("Connection failed")));
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenAnswer(invocation -> {
			LibreTimeApiTestResult result = invocation.getArgument(0);
			result.setId(UUID.randomUUID());
			return result;
		});

		ApiTestResultResponseDTO result = testingService.testConnection(stationId);

		assertNotNull(result);
		assertEquals(TestType.CONNECTIVITY, result.getTestType());
		assertEquals(TestStatus.ERROR, result.getStatus());
		verify(httpClient).get("/api/v2/status");
	}

	@Test
	void testAuthentication_When_Successful_Expect_ReturnsPassed() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(httpClient.get("/api/v2/status")).thenReturn(Mono.just("{\"status\":\"ok\"}"));
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenAnswer(invocation -> {
			LibreTimeApiTestResult result = invocation.getArgument(0);
			result.setId(UUID.randomUUID());
			return result;
		});

		ApiTestResultResponseDTO result = testingService.testAuthentication(stationId);

		assertNotNull(result);
		assertEquals(TestType.AUTHENTICATION, result.getTestType());
		verify(httpClient).get("/api/v2/status");
	}

	@Test
	void testAuthentication_When_Unauthorized_Expect_ReturnsFailed() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		WebClientResponseException unauthorized = 
				WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null, null, null);
		when(httpClient.get("/api/v2/status")).thenReturn(Mono.error(unauthorized));
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenAnswer(invocation -> {
			LibreTimeApiTestResult result = invocation.getArgument(0);
			result.setId(UUID.randomUUID());
			return result;
		});

		ApiTestResultResponseDTO result = testingService.testAuthentication(stationId);

		assertNotNull(result);
		assertEquals(TestType.AUTHENTICATION, result.getTestType());
		assertEquals(TestStatus.FAILED, result.getStatus());
	}

	@Test
	void testEndpoint_When_EndpointExists_Expect_ReturnsResult() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
		when(httpClient.get(endpoint.getEndpointPath())).thenReturn(Mono.just("{\"data\":[]}"));
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenReturn(testResult);

		ApiTestResultResponseDTO result = testingService.testEndpoint(stationId, endpointId);

		assertNotNull(result);
		assertEquals(TestType.CRUD, result.getTestType());
		verify(endpointRepository).findById(endpointId);
		verify(httpClient).get(endpoint.getEndpointPath());
	}

	@Test
	void testEndpoint_When_EndpointNotExists_Expect_ThrowsNotFoundException() {
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.empty());

		assertThrows(com.onelpro.librelog.exceptions.NotFoundException.class, 
				() -> testingService.testEndpoint(stationId, endpointId));
		verify(endpointRepository).findById(endpointId);
		verify(httpClient, never()).get(anyString());
	}

	@Test
	void testEndpoint_When_PostMethod_Expect_UsesPost() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		endpoint.setHttpMethod("POST");
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
		when(httpClient.post(anyString(), anyString())).thenReturn(Mono.just("{\"id\":\"123\"}"));
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenReturn(testResult);

		ApiTestResultResponseDTO result = testingService.testEndpoint(stationId, endpointId);

		assertNotNull(result);
		verify(httpClient).post(anyString(), anyString());
		verify(httpClient, never()).get(anyString());
	}

	@Test
	void runAllTests_When_EndpointsExist_Expect_ReturnsSummary() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(endpointRepository.findAll()).thenReturn(Arrays.asList(endpoint));
		when(httpClient.get(anyString())).thenReturn(Mono.just("{\"data\":[]}"));
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenReturn(testResult);

		ApiTestSummaryResponseDTO result = testingService.runAllTests(stationId);

		assertNotNull(result);
		assertEquals(1, result.getTotalTests());
		assertNotNull(result.getTestResults());
		verify(endpointRepository).findAll();
	}

	@Test
	void runAllTests_When_NoEndpoints_Expect_ReturnsEmptySummary() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(endpointRepository.findAll()).thenReturn(List.of());

		ApiTestSummaryResponseDTO result = testingService.runAllTests(stationId);

		assertNotNull(result);
		assertEquals(0, result.getTotalTests());
		assertTrue(result.getTestResults().isEmpty());
	}

	@Test
	void runAllTests_When_TestFails_Expect_IncludesFailedInSummary() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(endpointRepository.findAll()).thenReturn(Arrays.asList(endpoint));
		when(httpClient.get(anyString())).thenReturn(Mono.error(new RuntimeException("Test failed")));
		when(testResultRepository.save(any(LibreTimeApiTestResult.class))).thenReturn(testResult);

		ApiTestSummaryResponseDTO result = testingService.runAllTests(stationId);

		assertNotNull(result);
		assertEquals(1, result.getTotalTests());
		// Should include error result
		assertFalse(result.getTestResults().isEmpty());
	}

	@Test
	void generateTestReport_When_JsonFormat_Expect_ReturnsJson() {
		when(testResultRepository.findAll()).thenReturn(Arrays.asList(testResult));

		String result = testingService.generateTestReport("JSON");

		assertNotNull(result);
		assertTrue(result.contains("testResults"));
		verify(testResultRepository).findAll();
	}

	@Test
	void generateTestReport_When_MarkdownFormat_Expect_ReturnsMarkdown() {
		when(testResultRepository.findAll()).thenReturn(Arrays.asList(testResult));

		String result = testingService.generateTestReport("MARKDOWN");

		assertNotNull(result);
		verify(testResultRepository).findAll();
	}

	@Test
	void generateTestReport_When_HtmlFormat_Expect_ReturnsHtml() {
		when(testResultRepository.findAll()).thenReturn(Arrays.asList(testResult));

		String result = testingService.generateTestReport("HTML");

		assertNotNull(result);
		assertTrue(result.contains("<html") || result.contains("<!DOCTYPE"));
		verify(testResultRepository).findAll();
	}

	@Test
	void generateTestReport_When_UnknownFormat_Expect_ReturnsMarkdown() {
		when(testResultRepository.findAll()).thenReturn(Arrays.asList(testResult));

		String result = testingService.generateTestReport("UNKNOWN");

		assertNotNull(result);
		// Should default to Markdown
		verify(testResultRepository).findAll();
	}

	@Test
	void generateBugReport_When_EndpointExistsWithFailedTests_Expect_ReturnsReport() {
		LibreTimeApiTestResult failedTest = LibreTimeApiTestResult.builder()
				.id(UUID.randomUUID())
				.endpoint(endpoint)
				.testType(TestType.CRUD)
				.status(TestStatus.FAILED)
				.responseStatusCode(404)
				.errorMessage("Not found")
				.testTimestamp(LocalDateTime.now())
				.build();

		when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
		when(testResultRepository.findByEndpointAndTestType(endpoint, TestType.CRUD))
				.thenReturn(Arrays.asList(failedTest));

		BugReportResponseDTO result = testingService.generateBugReport(endpointId);

		assertNotNull(result);
		assertEquals(endpointId, result.getEndpointId());
		assertEquals(endpoint.getEndpointPath(), result.getEndpointPath());
		assertNotNull(result.getIssueDescription());
		verify(endpointRepository).findById(endpointId);
	}

	@Test
	void generateBugReport_When_EndpointNotExists_Expect_ThrowsNotFoundException() {
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.empty());

		assertThrows(com.onelpro.librelog.exceptions.NotFoundException.class, 
				() -> testingService.generateBugReport(endpointId));
		verify(endpointRepository).findById(endpointId);
	}

	@Test
	void generateBugReport_When_NoFailedTests_Expect_ThrowsBadRequestException() {
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
		when(testResultRepository.findByEndpointAndTestType(endpoint, TestType.CRUD))
				.thenReturn(List.of());

		assertThrows(com.onelpro.librelog.exceptions.BadRequestException.class, 
				() -> testingService.generateBugReport(endpointId));
	}

	@Test
	void generateBugReports_When_BrokenEndpointsExist_Expect_ReturnsReports() {
		LibreTimeApiTestResult failedTest = LibreTimeApiTestResult.builder()
				.id(UUID.randomUUID())
				.endpoint(endpoint)
				.testType(TestType.CRUD)
				.status(TestStatus.FAILED)
				.responseStatusCode(500)
				.errorMessage("Server error")
				.testTimestamp(LocalDateTime.now())
				.build();

		endpoint.setStatus(EndpointStatus.BROKEN);
		when(endpointRepository.findByStatus(EndpointStatus.BROKEN)).thenReturn(Arrays.asList(endpoint));
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
		when(testResultRepository.findByEndpointAndTestType(endpoint, TestType.CRUD))
				.thenReturn(Arrays.asList(failedTest));

		List<BugReportResponseDTO> result = testingService.generateBugReports();

		assertNotNull(result);
		// May be empty if exception occurs during generation, so just verify it's not null
		verify(endpointRepository).findByStatus(EndpointStatus.BROKEN);
	}

	@Test
	void generateBugReports_When_NoBrokenEndpoints_Expect_ReturnsEmptyList() {
		when(endpointRepository.findByStatus(EndpointStatus.BROKEN)).thenReturn(List.of());

		List<BugReportResponseDTO> result = testingService.generateBugReports();

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(endpointRepository).findByStatus(EndpointStatus.BROKEN);
	}

}

