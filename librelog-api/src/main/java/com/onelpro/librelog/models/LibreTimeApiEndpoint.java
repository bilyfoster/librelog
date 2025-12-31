package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.EndpointStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a discovered LibreTime API endpoint.
 */
@Entity
@Table(name = "libretime_api_endpoints")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeApiEndpoint {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "endpoint_path", nullable = false, length = 500)
	private String endpointPath;

	@Column(name = "http_method", nullable = false, length = 10)
	private String httpMethod;

	@Column(name = "resource_type", length = 100)
	private String resourceType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private EndpointStatus status;

	@Column(name = "last_tested_at")
	private LocalDateTime lastTestedAt;

	@Column(name = "response_time_ms")
	private Integer responseTimeMs;

	@Column(name = "requires_authentication", nullable = false)
	private Boolean requiresAuthentication;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "documentation_url", length = 500)
	private String documentationUrl;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

