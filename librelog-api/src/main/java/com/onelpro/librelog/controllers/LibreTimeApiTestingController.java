package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.ApiEndpointResponseDTO;
import com.onelpro.librelog.dto.ApiTestResultResponseDTO;
import com.onelpro.librelog.dto.ApiTestSummaryResponseDTO;
import com.onelpro.librelog.dto.BugReportResponseDTO;
import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.services.LibreTimeApiDiscoveryService;
import com.onelpro.librelog.services.LibreTimeApiTestingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for LibreTime API endpoint discovery and testing.
 */
@RestController
@RequestMapping("/api/libretime/testing")
@Tag(name = "LibreTime API Testing", description = "API endpoint discovery and testing endpoints")
public class LibreTimeApiTestingController {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeApiTestingController.class);

	private final LibreTimeApiDiscoveryService discoveryService;
	private final LibreTimeApiTestingService testingService;

	public LibreTimeApiTestingController(
			LibreTimeApiDiscoveryService discoveryService,
			LibreTimeApiTestingService testingService) {
		this.discoveryService = discoveryService;
		this.testingService = testingService;
	}

	@PostMapping("/discover")
	@Operation(
			summary = "Discover LibreTime API endpoints",
			description = "Discovers available LibreTime API endpoints by testing common REST patterns. Returns list of discovered endpoints."
	)
	@ApiResponse(responseCode = "200", description = "Endpoint discovery completed")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_TEST')")
	public ResponseEntity<List<ApiEndpointResponseDTO>> discoverEndpoints() {
		logger.info("POST /api/libretime/testing/discover - Discovering API endpoints");
		List<ApiEndpointResponseDTO> endpoints = discoveryService.discoverEndpoints();
		return ResponseEntity.ok(endpoints);
	}

	@GetMapping("/endpoints")
	@Operation(
			summary = "Get discovered endpoints",
			description = "Retrieves all previously discovered LibreTime API endpoints."
	)
	@ApiResponse(responseCode = "200", description = "Endpoints retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<List<ApiEndpointResponseDTO>> getDiscoveredEndpoints() {
		logger.info("GET /api/libretime/testing/endpoints - Getting discovered endpoints");
		List<ApiEndpointResponseDTO> endpoints = discoveryService.getDiscoveredEndpoints();
		return ResponseEntity.ok(endpoints);
	}

	@PutMapping("/endpoints/{endpointId}/status")
	@Operation(
			summary = "Update endpoint status",
			description = "Updates the status of a discovered endpoint."
	)
	@ApiResponse(responseCode = "200", description = "Endpoint status updated successfully")
	@ApiResponse(responseCode = "404", description = "Endpoint not found")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_TEST')")
	public ResponseEntity<ApiEndpointResponseDTO> updateEndpointStatus(
			@PathVariable UUID endpointId,
			@RequestParam String status) {
		logger.info("PUT /api/libretime/testing/endpoints/{}/status - Updating endpoint status to {}", endpointId, status);
		EndpointStatus endpointStatus = EndpointStatus.valueOf(status.toUpperCase());
		ApiEndpointResponseDTO endpoint = discoveryService.updateEndpointStatus(endpointId, endpointStatus);
		return ResponseEntity.ok(endpoint);
	}

	@PostMapping("/test-connection")
	@Operation(
			summary = "Test LibreTime API connection",
			description = "Tests basic connectivity to LibreTime API."
	)
	@ApiResponse(responseCode = "200", description = "Connection test completed")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_TEST')")
	public ResponseEntity<ApiTestResultResponseDTO> testConnection() {
		logger.info("POST /api/libretime/testing/test-connection - Testing API connection");
		ApiTestResultResponseDTO result = testingService.testConnection();
		return ResponseEntity.ok(result);
	}

	@PostMapping("/test-authentication")
	@Operation(
			summary = "Test LibreTime API authentication",
			description = "Tests authentication with LibreTime API using JWT token."
	)
	@ApiResponse(responseCode = "200", description = "Authentication test completed")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_TEST')")
	public ResponseEntity<ApiTestResultResponseDTO> testAuthentication() {
		logger.info("POST /api/libretime/testing/test-authentication - Testing API authentication");
		ApiTestResultResponseDTO result = testingService.testAuthentication();
		return ResponseEntity.ok(result);
	}

	@PostMapping("/test-endpoint/{endpointId}")
	@Operation(
			summary = "Test specific endpoint",
			description = "Tests a specific discovered endpoint."
	)
	@ApiResponse(responseCode = "200", description = "Endpoint test completed")
	@ApiResponse(responseCode = "404", description = "Endpoint not found")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_TEST')")
	public ResponseEntity<ApiTestResultResponseDTO> testEndpoint(@PathVariable UUID endpointId) {
		logger.info("POST /api/libretime/testing/test-endpoint/{} - Testing endpoint", endpointId);
		ApiTestResultResponseDTO result = testingService.testEndpoint(endpointId);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/run-all-tests")
	@Operation(
			summary = "Run all API tests",
			description = "Runs tests for all discovered endpoints. Returns summary of test results."
	)
	@ApiResponse(responseCode = "200", description = "All tests completed")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_TEST')")
	public ResponseEntity<ApiTestSummaryResponseDTO> runAllTests() {
		logger.info("POST /api/libretime/testing/run-all-tests - Running all API tests");
		ApiTestSummaryResponseDTO summary = testingService.runAllTests();
		return ResponseEntity.ok(summary);
	}

	@GetMapping("/test-results")
	@Operation(
			summary = "Get test results",
			description = "Retrieves test results. Can be filtered by endpoint, test type, or status."
	)
	@ApiResponse(responseCode = "200", description = "Test results retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<ApiTestSummaryResponseDTO> getTestResults(
			@RequestParam(required = false) UUID endpointId,
			@RequestParam(required = false) String status) {
		logger.info("GET /api/libretime/testing/test-results - Getting test results (endpointId: {}, status: {})", 
				endpointId, status);
		// For now, return all tests summary. In production, would filter by parameters
		ApiTestSummaryResponseDTO summary = testingService.runAllTests();
		return ResponseEntity.ok(summary);
	}

	@GetMapping("/test-summary")
	@Operation(
			summary = "Get test summary",
			description = "Retrieves summary statistics of all test results."
	)
	@ApiResponse(responseCode = "200", description = "Test summary retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<ApiTestSummaryResponseDTO> getTestSummary() {
		logger.info("GET /api/libretime/testing/test-summary - Getting test summary");
		ApiTestSummaryResponseDTO summary = testingService.runAllTests();
		return ResponseEntity.ok(summary);
	}

	@GetMapping("/bug-reports")
	@Operation(
			summary = "Get bug reports",
			description = "Retrieves bug reports for all broken endpoints."
	)
	@ApiResponse(responseCode = "200", description = "Bug reports retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_EXPORT_LOGS')")
	public ResponseEntity<List<BugReportResponseDTO>> getBugReports() {
		logger.info("GET /api/libretime/testing/bug-reports - Getting bug reports");
		List<BugReportResponseDTO> reports = testingService.generateBugReports();
		return ResponseEntity.ok(reports);
	}

	@GetMapping("/bug-reports/{endpointId}")
	@Operation(
			summary = "Get bug report for endpoint",
			description = "Retrieves a bug report for a specific broken endpoint."
	)
	@ApiResponse(responseCode = "200", description = "Bug report retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Endpoint not found or no failed tests")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_EXPORT_LOGS')")
	public ResponseEntity<BugReportResponseDTO> getBugReport(@PathVariable UUID endpointId) {
		logger.info("GET /api/libretime/testing/bug-reports/{} - Getting bug report", endpointId);
		BugReportResponseDTO report = testingService.generateBugReport(endpointId);
		return ResponseEntity.ok(report);
	}

	@GetMapping(value = "/export-documentation", produces = {MediaType.APPLICATION_JSON_VALUE, "text/markdown", "text/html"})
	@Operation(
			summary = "Export test documentation",
			description = "Exports test results and documentation in the requested format (JSON, Markdown, or HTML)."
	)
	@ApiResponse(responseCode = "200", description = "Documentation exported successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_EXPORT_LOGS')")
	public ResponseEntity<String> exportDocumentation(
			@RequestParam(defaultValue = "JSON") String format) {
		logger.info("GET /api/libretime/testing/export-documentation - Exporting documentation (format: {})", format);
		String documentation = testingService.generateTestReport(format);
		
		MediaType mediaType;
		if ("MARKDOWN".equalsIgnoreCase(format)) {
			mediaType = MediaType.parseMediaType("text/markdown");
		} else if ("HTML".equalsIgnoreCase(format)) {
			mediaType = MediaType.parseMediaType("text/html");
		} else {
			mediaType = MediaType.APPLICATION_JSON;
		}
		
		return ResponseEntity.ok()
				.contentType(mediaType)
				.body(documentation);
	}

}

