package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for bug report information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugReportResponseDTO {

	private UUID endpointId;
	private String endpointPath;
	private String httpMethod;
	private String issueDescription;
	private String expectedBehavior;
	private String actualBehavior;
	private String requestPayload;
	private String responseBody;
	private Integer responseStatusCode;
	private String errorMessage;
	private String reproductionSteps;
	private LocalDateTime reportedAt;

}

