package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ApiTestResultResponseDTO;
import com.onelpro.librelog.dto.ApiTestSummaryResponseDTO;
import com.onelpro.librelog.dto.BugReportResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for testing LibreTime API endpoints.
 */
public interface LibreTimeApiTestingService {

	/**
	 * Tests basic connectivity to LibreTime API.
	 * 
	 * @param stationId The station ID
	 * @return Connection test result
	 */
	ApiTestResultResponseDTO testConnection(UUID stationId);

	/**
	 * Tests authentication with LibreTime API.
	 * 
	 * @param stationId The station ID
	 * @return Authentication test result
	 */
	ApiTestResultResponseDTO testAuthentication(UUID stationId);

	/**
	 * Tests a specific endpoint.
	 * 
	 * @param endpointId The endpoint ID to test
	 * @return Test result
	 */
	ApiTestResultResponseDTO testEndpoint(UUID endpointId);

	/**
	 * Runs all tests for all discovered endpoints.
	 * 
	 * @param stationId The station ID
	 * @return Summary of test results
	 */
	ApiTestSummaryResponseDTO runAllTests(UUID stationId);

	/**
	 * Generates a test report for all test results.
	 * 
	 * @param format Report format (JSON, Markdown, HTML)
	 * @return Test report as string
	 */
	String generateTestReport(String format);

	/**
	 * Generates a bug report for a broken endpoint.
	 * 
	 * @param endpointId The endpoint ID
	 * @return Bug report
	 */
	BugReportResponseDTO generateBugReport(UUID endpointId);

	/**
	 * Generates bug reports for all broken endpoints.
	 * 
	 * @return List of bug reports
	 */
	List<BugReportResponseDTO> generateBugReports();

}

