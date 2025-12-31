package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ApiTestResultResponseDTO;
import com.onelpro.librelog.dto.ApiTestSummaryResponseDTO;
import com.onelpro.librelog.dto.BugReportResponseDTO;
import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.enums.TestStatus;
import com.onelpro.librelog.enums.TestType;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeApiEndpoint;
import com.onelpro.librelog.models.LibreTimeApiTestResult;
import com.onelpro.librelog.repositories.LibreTimeApiEndpointRepository;
import com.onelpro.librelog.repositories.LibreTimeApiTestResultRepository;
import com.onelpro.librelog.services.LibreTimeApiTestingService;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the LibreTime API testing service.
 * Provides methods for testing endpoints, generating reports, and bug reports.
 */
@Service
public class LibreTimeApiTestingServiceImpl implements LibreTimeApiTestingService {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeApiTestingServiceImpl.class);

	private final LibreTimeApiEndpointRepository endpointRepository;
	private final LibreTimeApiTestResultRepository testResultRepository;
	private final LibreTimeHttpClient httpClient;
	private final LibreTimeIntegrationConfigService configService;

	public LibreTimeApiTestingServiceImpl(
			LibreTimeApiEndpointRepository endpointRepository,
			LibreTimeApiTestResultRepository testResultRepository,
			LibreTimeHttpClient httpClient,
			LibreTimeIntegrationConfigService configService) {
		this.endpointRepository = endpointRepository;
		this.testResultRepository = testResultRepository;
		this.httpClient = httpClient;
		this.configService = configService;
	}

	@Override
	@Transactional
	public ApiTestResultResponseDTO testConnection() {
		logger.info("Testing LibreTime API connection");
		return performTest(null, TestType.CONNECTIVITY, null, null);
	}

	@Override
	@Transactional
	public ApiTestResultResponseDTO testAuthentication() {
		logger.info("Testing LibreTime API authentication");
		return performTest(null, TestType.AUTHENTICATION, null, null);
	}

	@Override
	@Transactional
	public ApiTestResultResponseDTO testEndpoint(UUID endpointId) {
		logger.info("Testing endpoint: {}", endpointId);
		LibreTimeApiEndpoint endpoint = endpointRepository.findById(endpointId)
				.orElseThrow(() -> new com.onelpro.librelog.exceptions.NotFoundException("Endpoint not found: " + endpointId));
		return performTest(endpoint, TestType.CRUD, null, null);
	}

	@Override
	@Transactional
	public ApiTestSummaryResponseDTO runAllTests() {
		logger.info("Running all endpoint tests");
		List<LibreTimeApiEndpoint> endpoints = endpointRepository.findAll();
		List<ApiTestResultResponseDTO> results = new ArrayList<>();

		// Configure HTTP client
		configureHttpClient();

		for (LibreTimeApiEndpoint endpoint : endpoints) {
			try {
				ApiTestResultResponseDTO result = performTest(endpoint, TestType.CRUD, null, null);
				results.add(result);
			} catch (Exception e) {
				logger.error("Error testing endpoint {}: {}", endpoint.getId(), e.getMessage());
				ApiTestResultResponseDTO errorResult = ApiTestResultResponseDTO.builder()
						.endpointId(endpoint.getId())
						.endpointPath(endpoint.getEndpointPath())
						.httpMethod(endpoint.getHttpMethod())
						.testType(TestType.CRUD)
						.status(TestStatus.ERROR)
						.errorMessage(e.getMessage())
						.testTimestamp(LocalDateTime.now())
						.build();
				results.add(errorResult);
			}
		}

		int totalTests = results.size();
		long passedTests = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
		long failedTests = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
		long skippedTests = results.stream().filter(r -> r.getStatus() == TestStatus.SKIPPED).count();

		return ApiTestSummaryResponseDTO.builder()
				.totalTests(totalTests)
				.passedTests((int) passedTests)
				.failedTests((int) failedTests)
				.skippedTests((int) skippedTests)
				.testRunTimestamp(LocalDateTime.now())
				.testResults(results)
				.build();
	}

	@Override
	public String generateTestReport(String format) {
		logger.info("Generating test report in format: {}", format);
		List<LibreTimeApiTestResult> allResults = testResultRepository.findAll();

		if ("JSON".equalsIgnoreCase(format)) {
			return generateJsonReport(allResults);
		} else if ("MARKDOWN".equalsIgnoreCase(format)) {
			return generateMarkdownReport(allResults);
		} else if ("HTML".equalsIgnoreCase(format)) {
			return generateHtmlReport(allResults);
		} else {
			return generateMarkdownReport(allResults); // Default to Markdown
		}
	}

	@Override
	@Transactional
	public BugReportResponseDTO generateBugReport(UUID endpointId) {
		logger.info("Generating bug report for endpoint: {}", endpointId);
		LibreTimeApiEndpoint endpoint = endpointRepository.findById(endpointId)
				.orElseThrow(() -> new com.onelpro.librelog.exceptions.NotFoundException("Endpoint not found: " + endpointId));

		// Get latest failed test result
		List<LibreTimeApiTestResult> failedTests = testResultRepository
				.findByEndpointAndTestType(endpoint, TestType.CRUD)
				.stream()
				.filter(t -> t.getStatus() == TestStatus.FAILED || t.getStatus() == TestStatus.ERROR)
				.sorted((a, b) -> b.getTestTimestamp().compareTo(a.getTestTimestamp()))
				.limit(1)
				.collect(Collectors.toList());

		if (failedTests.isEmpty()) {
			throw new com.onelpro.librelog.exceptions.BadRequestException("No failed tests found for endpoint: " + endpointId);
		}

		LibreTimeApiTestResult latestFailure = failedTests.get(0);

		return BugReportResponseDTO.builder()
				.endpointId(endpoint.getId())
				.endpointPath(endpoint.getEndpointPath())
				.httpMethod(endpoint.getHttpMethod())
				.issueDescription("API endpoint test failed: " + latestFailure.getErrorMessage())
				.expectedBehavior("Endpoint should respond with 2xx status code")
				.actualBehavior("Endpoint returned status " + latestFailure.getResponseStatusCode() + ": " + latestFailure.getErrorMessage())
				.requestPayload(latestFailure.getRequestPayload())
				.responseBody(latestFailure.getResponseBody())
				.responseStatusCode(latestFailure.getResponseStatusCode())
				.errorMessage(latestFailure.getErrorMessage())
				.reproductionSteps(generateReproductionSteps(endpoint, latestFailure))
				.reportedAt(LocalDateTime.now())
				.build();
	}

	@Override
	@Transactional
	public List<BugReportResponseDTO> generateBugReports() {
		logger.info("Generating bug reports for all broken endpoints");
		List<LibreTimeApiEndpoint> brokenEndpoints = endpointRepository.findByStatus(EndpointStatus.BROKEN);
		List<BugReportResponseDTO> bugReports = new ArrayList<>();

		for (LibreTimeApiEndpoint endpoint : brokenEndpoints) {
			try {
				BugReportResponseDTO report = generateBugReport(endpoint.getId());
				bugReports.add(report);
			} catch (Exception e) {
				logger.error("Error generating bug report for endpoint {}: {}", endpoint.getId(), e.getMessage());
			}
		}

		return bugReports;
	}

	/**
	 * Performs a test on an endpoint.
	 * 
	 * @param endpoint The endpoint to test (null for connection/auth tests)
	 * @param testType The type of test
	 * @param requestPayload Optional request payload
	 * @param notes Optional notes
	 * @return Test result DTO
	 */
	private ApiTestResultResponseDTO performTest(
			LibreTimeApiEndpoint endpoint,
			TestType testType,
			String requestPayload,
			String notes) {
		configureHttpClient();

		long startTime = System.currentTimeMillis();
		TestStatus status = TestStatus.FAILED;
		Integer responseStatusCode = null;
		String responseBody = null;
		String errorMessage = null;
		String endpointPath = endpoint != null ? endpoint.getEndpointPath() : "/api/v2/status";

		try {
			Mono<String> response;
			if (endpoint != null && "POST".equalsIgnoreCase(endpoint.getHttpMethod())) {
				response = httpClient.post(endpoint.getEndpointPath(), requestPayload != null ? requestPayload : "{}");
			} else {
				response = httpClient.get(endpointPath);
			}

			String result = response.block(java.time.Duration.ofSeconds(10));
			long responseTime = System.currentTimeMillis() - startTime;

			if (result != null) {
				status = TestStatus.PASSED;
				responseStatusCode = 200;
				responseBody = result;
			}

			// Update endpoint status
			if (endpoint != null) {
				endpoint.setStatus(EndpointStatus.WORKING);
				endpoint.setLastTestedAt(LocalDateTime.now());
				endpoint.setResponseTimeMs((int) responseTime);
				endpointRepository.save(endpoint);
			}

		} catch (WebClientResponseException e) {
			long responseTime = System.currentTimeMillis() - startTime;
			responseStatusCode = e.getStatusCode().value();
			responseBody = e.getResponseBodyAsString();
			errorMessage = e.getMessage();

			if (e.getStatusCode().is4xxClientError()) {
				status = TestStatus.FAILED;
			} else if (e.getStatusCode().is5xxServerError()) {
				status = TestStatus.ERROR;
			}

			// Update endpoint status
			if (endpoint != null) {
				if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
					endpoint.setStatus(EndpointStatus.MISSING);
				} else {
					endpoint.setStatus(EndpointStatus.BROKEN);
				}
				endpoint.setLastTestedAt(LocalDateTime.now());
				endpoint.setResponseTimeMs((int) responseTime);
				endpointRepository.save(endpoint);
			}
		} catch (Exception e) {
			long responseTime = System.currentTimeMillis() - startTime;
			status = TestStatus.ERROR;
			errorMessage = e.getMessage();

			// Update endpoint status
			if (endpoint != null) {
				endpoint.setStatus(EndpointStatus.BROKEN);
				endpoint.setLastTestedAt(LocalDateTime.now());
				endpoint.setResponseTimeMs((int) responseTime);
				endpointRepository.save(endpoint);
			}
		}

		// Save test result
		LibreTimeApiTestResult testResult = LibreTimeApiTestResult.builder()
				.endpoint(endpoint)
				.testType(testType)
				.status(status)
				.requestPayload(requestPayload)
				.responseStatusCode(responseStatusCode)
				.responseBody(responseBody)
				.errorMessage(errorMessage)
				.responseTimeMs((int) (System.currentTimeMillis() - startTime))
				.testTimestamp(LocalDateTime.now())
				.notes(notes)
				.build();

		testResult = testResultRepository.save(testResult);

		return mapToDTO(testResult, endpoint);
	}

	/**
	 * Configures the HTTP client with current integration settings.
	 */
	private void configureHttpClient() {
		try {
			String jwtToken = configService.getDecryptedJwtToken();
			com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO config = configService.getConfig();
			if (config != null) {
				httpClient.setBaseUrl(config.getApiBaseUrl());
				httpClient.setJwtToken(jwtToken);
			}
		} catch (Exception e) {
			logger.warn("Failed to configure HTTP client: {}", e.getMessage());
		}
	}

	/**
	 * Maps test result entity to DTO.
	 */
	private ApiTestResultResponseDTO mapToDTO(LibreTimeApiTestResult testResult, LibreTimeApiEndpoint endpoint) {
		return ApiTestResultResponseDTO.builder()
				.id(testResult.getId())
				.endpointId(endpoint != null ? endpoint.getId() : null)
				.endpointPath(endpoint != null ? endpoint.getEndpointPath() : null)
				.httpMethod(endpoint != null ? endpoint.getHttpMethod() : null)
				.testType(testResult.getTestType())
				.status(testResult.getStatus())
				.requestPayload(testResult.getRequestPayload())
				.responseStatusCode(testResult.getResponseStatusCode())
				.responseBody(testResult.getResponseBody())
				.errorMessage(testResult.getErrorMessage())
				.responseTimeMs(testResult.getResponseTimeMs())
				.testTimestamp(testResult.getTestTimestamp())
				.notes(testResult.getNotes())
				.build();
	}

	/**
	 * Generates JSON format test report.
	 */
	private String generateJsonReport(List<LibreTimeApiTestResult> results) {
		// Simple JSON generation - could use Jackson ObjectMapper for more complex structure
		StringBuilder json = new StringBuilder("{\n  \"testResults\": [\n");
		for (int i = 0; i < results.size(); i++) {
			LibreTimeApiTestResult result = results.get(i);
			json.append("    {\n");
			json.append("      \"id\": \"").append(result.getId()).append("\",\n");
			json.append("      \"endpoint\": \"").append(result.getEndpoint().getEndpointPath()).append("\",\n");
			json.append("      \"status\": \"").append(result.getStatus()).append("\",\n");
			json.append("      \"testType\": \"").append(result.getTestType()).append("\",\n");
			json.append("      \"timestamp\": \"").append(result.getTestTimestamp()).append("\"\n");
			json.append("    }");
			if (i < results.size() - 1) {
				json.append(",");
			}
			json.append("\n");
		}
		json.append("  ]\n}");
		return json.toString();
	}

	/**
	 * Generates Markdown format test report.
	 */
	private String generateMarkdownReport(List<LibreTimeApiTestResult> results) {
		StringBuilder md = new StringBuilder("# LibreTime API Test Report\n\n");
		md.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
		md.append("## Summary\n\n");
		long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
		long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
		long errors = results.stream().filter(r -> r.getStatus() == TestStatus.ERROR).count();
		md.append("- Total Tests: ").append(results.size()).append("\n");
		md.append("- Passed: ").append(passed).append("\n");
		md.append("- Failed: ").append(failed).append("\n");
		md.append("- Errors: ").append(errors).append("\n\n");
		md.append("## Test Results\n\n");
		for (LibreTimeApiTestResult result : results) {
			md.append("### ").append(result.getEndpoint().getEndpointPath()).append("\n\n");
			md.append("- Status: ").append(result.getStatus()).append("\n");
			md.append("- Type: ").append(result.getTestType()).append("\n");
			md.append("- Timestamp: ").append(result.getTestTimestamp()).append("\n");
			if (result.getErrorMessage() != null) {
				md.append("- Error: ").append(result.getErrorMessage()).append("\n");
			}
			md.append("\n");
		}
		return md.toString();
	}

	/**
	 * Generates HTML format test report.
	 */
	private String generateHtmlReport(List<LibreTimeApiTestResult> results) {
		StringBuilder html = new StringBuilder("<html><head><title>LibreTime API Test Report</title></head><body>");
		html.append("<h1>LibreTime API Test Report</h1>");
		html.append("<p>Generated: ").append(LocalDateTime.now()).append("</p>");
		html.append("<h2>Summary</h2>");
		long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
		long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
		html.append("<p>Total: ").append(results.size()).append(" | Passed: ").append(passed).append(" | Failed: ").append(failed).append("</p>");
		html.append("<h2>Test Results</h2><table border='1'><tr><th>Endpoint</th><th>Status</th><th>Type</th><th>Timestamp</th></tr>");
		for (LibreTimeApiTestResult result : results) {
			html.append("<tr>");
			html.append("<td>").append(result.getEndpoint().getEndpointPath()).append("</td>");
			html.append("<td>").append(result.getStatus()).append("</td>");
			html.append("<td>").append(result.getTestType()).append("</td>");
			html.append("<td>").append(result.getTestTimestamp()).append("</td>");
			html.append("</tr>");
		}
		html.append("</table></body></html>");
		return html.toString();
	}

	/**
	 * Generates reproduction steps for a bug report.
	 */
	private String generateReproductionSteps(LibreTimeApiEndpoint endpoint, LibreTimeApiTestResult testResult) {
		StringBuilder steps = new StringBuilder();
		steps.append("1. Configure LibreTime integration with base URL and JWT token\n");
		steps.append("2. Make ").append(endpoint.getHttpMethod()).append(" request to: ").append(endpoint.getEndpointPath()).append("\n");
		if (testResult.getRequestPayload() != null) {
			steps.append("3. Include request payload: ").append(testResult.getRequestPayload()).append("\n");
		}
		steps.append("4. Observe the error response\n");
		return steps.toString();
	}

}

