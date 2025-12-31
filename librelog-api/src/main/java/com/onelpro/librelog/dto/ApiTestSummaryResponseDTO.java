package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for API test summary information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTestSummaryResponseDTO {

	private Integer totalTests;
	private Integer passedTests;
	private Integer failedTests;
	private Integer skippedTests;
	private LocalDateTime testRunTimestamp;
	private List<ApiTestResultResponseDTO> testResults;

}

