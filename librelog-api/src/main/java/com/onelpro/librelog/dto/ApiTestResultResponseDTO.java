package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.TestStatus;
import com.onelpro.librelog.enums.TestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for API test result information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTestResultResponseDTO {

	private UUID id;
	private UUID endpointId;
	private String endpointPath;
	private String httpMethod;
	private TestType testType;
	private TestStatus status;
	private String requestPayload;
	private Integer responseStatusCode;
	private String responseBody;
	private String errorMessage;
	private Integer responseTimeMs;
	private LocalDateTime testTimestamp;
	private String notes;

}

