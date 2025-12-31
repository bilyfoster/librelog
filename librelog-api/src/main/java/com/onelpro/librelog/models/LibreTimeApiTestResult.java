package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.TestStatus;
import com.onelpro.librelog.enums.TestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a detailed API test result for a LibreTime endpoint.
 */
@Entity
@Table(name = "libretime_api_test_results")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeApiTestResult {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "endpoint_id", nullable = false)
	private LibreTimeApiEndpoint endpoint;

	@Enumerated(EnumType.STRING)
	@Column(name = "test_type", nullable = false, length = 50)
	private TestType testType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private TestStatus status;

	@Column(name = "request_payload", columnDefinition = "TEXT")
	private String requestPayload;

	@Column(name = "response_status_code")
	private Integer responseStatusCode;

	@Column(name = "response_body", columnDefinition = "TEXT")
	private String responseBody;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "response_time_ms")
	private Integer responseTimeMs;

	@Column(name = "test_timestamp", nullable = false)
	private LocalDateTime testTimestamp;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

}

