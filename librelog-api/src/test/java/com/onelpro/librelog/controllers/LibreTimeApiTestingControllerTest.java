package com.onelpro.librelog.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.enums.TestStatus;
import com.onelpro.librelog.enums.TestType;
import com.onelpro.librelog.config.GlobalExceptionHandler;
import com.onelpro.librelog.services.LibreTimeApiDiscoveryService;
import com.onelpro.librelog.services.LibreTimeApiTestingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LibreTimeApiTestingControllerTest {

	@Mock
	private LibreTimeApiDiscoveryService discoveryService;

	@Mock
	private LibreTimeApiTestingService testingService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private UUID stationId;
	private UUID endpointId;

	@BeforeEach
	void setUp() {
		LibreTimeApiTestingController controller = new LibreTimeApiTestingController(discoveryService, testingService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		objectMapper = new ObjectMapper();
		stationId = UUID.randomUUID();
		endpointId = UUID.randomUUID();
	}

	@Test
	void discoverEndpoints_When_Requested_Expect_ReturnsEndpoints() throws Exception {
		ApiEndpointResponseDTO endpoint = ApiEndpointResponseDTO.builder()
				.id(endpointId)
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.status(EndpointStatus.WORKING)
				.build();

		when(discoveryService.discoverEndpoints(stationId)).thenReturn(Arrays.asList(endpoint));

		mockMvc.perform(post("/api/libretime/testing/discover")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].endpointPath").value("/api/v2/files"));
	}

	@Test
	void getDiscoveredEndpoints_When_Requested_Expect_ReturnsEndpoints() throws Exception {
		ApiEndpointResponseDTO endpoint = ApiEndpointResponseDTO.builder()
				.id(endpointId)
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.status(EndpointStatus.WORKING)
				.build();

		when(discoveryService.getDiscoveredEndpoints()).thenReturn(Arrays.asList(endpoint));

		mockMvc.perform(get("/api/libretime/testing/endpoints"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].endpointPath").value("/api/v2/files"));
	}

	@Test
	void updateEndpointStatus_When_ValidRequest_Expect_ReturnsUpdated() throws Exception {
		ApiEndpointResponseDTO endpoint = ApiEndpointResponseDTO.builder()
				.id(endpointId)
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.status(EndpointStatus.BROKEN)
				.build();

		when(discoveryService.updateEndpointStatus(endpointId, EndpointStatus.BROKEN))
				.thenReturn(endpoint);

		mockMvc.perform(put("/api/libretime/testing/endpoints/{endpointId}/status", endpointId)
						.param("status", "BROKEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("BROKEN"));
	}

	@Test
	void testConnection_When_Requested_Expect_ReturnsResult() throws Exception {
		ApiTestResultResponseDTO result = ApiTestResultResponseDTO.builder()
				.id(UUID.randomUUID())
				.testType(TestType.CONNECTIVITY)
				.status(TestStatus.PASSED)
				.responseStatusCode(200)
				.responseTimeMs(100)
				.testTimestamp(LocalDateTime.now())
				.build();

		when(testingService.testConnection(stationId)).thenReturn(result);

		mockMvc.perform(post("/api/libretime/testing/test-connection")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.testType").value("CONNECTIVITY"))
				.andExpect(jsonPath("$.status").value("PASSED"));
	}

	@Test
	void testAuthentication_When_Requested_Expect_ReturnsResult() throws Exception {
		ApiTestResultResponseDTO result = ApiTestResultResponseDTO.builder()
				.id(UUID.randomUUID())
				.testType(TestType.AUTHENTICATION)
				.status(TestStatus.PASSED)
				.responseStatusCode(200)
				.testTimestamp(LocalDateTime.now())
				.build();

		when(testingService.testAuthentication(stationId)).thenReturn(result);

		mockMvc.perform(post("/api/libretime/testing/test-authentication")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.testType").value("AUTHENTICATION"));
	}

	@Test
	void testEndpoint_When_EndpointExists_Expect_ReturnsResult() throws Exception {
		ApiTestResultResponseDTO result = ApiTestResultResponseDTO.builder()
				.id(UUID.randomUUID())
				.endpointId(endpointId)
				.testType(TestType.CRUD)
				.status(TestStatus.PASSED)
				.responseStatusCode(200)
				.testTimestamp(LocalDateTime.now())
				.build();

		when(testingService.testEndpoint(stationId, endpointId)).thenReturn(result);

		mockMvc.perform(post("/api/libretime/testing/test-endpoint/{endpointId}", endpointId)
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.testType").value("CRUD"))
				.andExpect(jsonPath("$.endpointId").value(endpointId.toString()));
	}

	@Test
	void runAllTests_When_Requested_Expect_ReturnsSummary() throws Exception {
		ApiTestResultResponseDTO testResult = ApiTestResultResponseDTO.builder()
				.id(UUID.randomUUID())
				.testType(TestType.CRUD)
				.status(TestStatus.PASSED)
				.testTimestamp(LocalDateTime.now())
				.build();

		ApiTestSummaryResponseDTO summary = ApiTestSummaryResponseDTO.builder()
				.totalTests(1)
				.passedTests(1)
				.failedTests(0)
				.skippedTests(0)
				.testRunTimestamp(LocalDateTime.now())
				.testResults(Arrays.asList(testResult))
				.build();

		when(testingService.runAllTests(stationId)).thenReturn(summary);

		mockMvc.perform(post("/api/libretime/testing/run-all-tests")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalTests").value(1))
				.andExpect(jsonPath("$.passedTests").value(1));
	}

	@Test
	void getTestResults_When_Requested_Expect_ReturnsSummary() throws Exception {
		ApiTestSummaryResponseDTO summary = ApiTestSummaryResponseDTO.builder()
				.totalTests(5)
				.passedTests(4)
				.failedTests(1)
				.skippedTests(0)
				.testRunTimestamp(LocalDateTime.now())
				.build();

		when(testingService.runAllTests(stationId)).thenReturn(summary);

		mockMvc.perform(get("/api/libretime/testing/test-results")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalTests").value(5))
				.andExpect(jsonPath("$.passedTests").value(4));
	}

	@Test
	void getTestSummary_When_Requested_Expect_ReturnsSummary() throws Exception {
		ApiTestSummaryResponseDTO summary = ApiTestSummaryResponseDTO.builder()
				.totalTests(10)
				.passedTests(8)
				.failedTests(2)
				.skippedTests(0)
				.testRunTimestamp(LocalDateTime.now())
				.build();

		when(testingService.runAllTests(stationId)).thenReturn(summary);

		mockMvc.perform(get("/api/libretime/testing/test-summary")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalTests").value(10));
	}

	@Test
	void getBugReports_When_Requested_Expect_ReturnsReports() throws Exception {
		BugReportResponseDTO bugReport = BugReportResponseDTO.builder()
				.endpointId(endpointId)
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.issueDescription("Endpoint returns 500 error")
				.expectedBehavior("Should return 200 with file list")
				.actualBehavior("Returns 500 Internal Server Error")
				.responseStatusCode(500)
				.reportedAt(LocalDateTime.now())
				.build();

		when(testingService.generateBugReports()).thenReturn(Arrays.asList(bugReport));

		mockMvc.perform(get("/api/libretime/testing/bug-reports"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].endpointPath").value("/api/v2/files"));
	}

	@Test
	void getBugReport_When_EndpointExists_Expect_ReturnsReport() throws Exception {
		BugReportResponseDTO bugReport = BugReportResponseDTO.builder()
				.endpointId(endpointId)
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.issueDescription("Endpoint returns 404")
				.reportedAt(LocalDateTime.now())
				.build();

		when(testingService.generateBugReport(endpointId)).thenReturn(bugReport);

		mockMvc.perform(get("/api/libretime/testing/bug-reports/{endpointId}", endpointId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.endpointId").value(endpointId.toString()))
				.andExpect(jsonPath("$.endpointPath").value("/api/v2/files"));
	}

	@Test
	void exportTestDocumentation_When_JsonFormat_Expect_ReturnsJson() throws Exception {
		String jsonReport = "{\"testResults\":[]}";
		when(testingService.generateTestReport("JSON")).thenReturn(jsonReport);

		mockMvc.perform(get("/api/libretime/testing/export-documentation")
						.param("format", "JSON"))
				.andExpect(status().isOk())
				.andExpect(content().string(jsonReport));
	}

	@Test
	void exportTestDocumentation_When_MarkdownFormat_Expect_ReturnsMarkdown() throws Exception {
		String markdownReport = "# Test Report\n\n## Summary";
		when(testingService.generateTestReport("MARKDOWN")).thenReturn(markdownReport);

		mockMvc.perform(get("/api/libretime/testing/export-documentation")
						.param("format", "MARKDOWN"))
				.andExpect(status().isOk())
				.andExpect(content().string(markdownReport));
	}

	@Test
	void exportTestDocumentation_When_HtmlFormat_Expect_ReturnsHtml() throws Exception {
		String htmlReport = "<html><body>Test Report</body></html>";
		when(testingService.generateTestReport("HTML")).thenReturn(htmlReport);

		mockMvc.perform(get("/api/libretime/testing/export-documentation")
						.param("format", "HTML"))
				.andExpect(status().isOk())
				.andExpect(content().string(htmlReport));
	}

}

