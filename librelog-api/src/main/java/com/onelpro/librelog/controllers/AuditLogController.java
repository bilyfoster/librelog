package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AuditLogFilterDTO;
import com.onelpro.librelog.dto.AuditLogResponseDTO;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.services.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for audit log endpoints.
 */
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Audit log viewing and export endpoints")
public class AuditLogController {

	private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);

	private final AuditService auditService;

	public AuditLogController(AuditService auditService) {
		this.auditService = auditService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get audit logs with filtering",
			description = "Retrieves audit logs with optional filtering by user, action type, resource type, station, and date range. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Page<AuditLogResponseDTO>> getAuditLogs(
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) AuditActionType actionType,
			@RequestParam(required = false) String resourceType,
			@RequestParam(required = false) UUID stationId,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		logger.info("Get audit logs request with filters: userId={}, actionType={}, resourceType={}", 
				userId, actionType, resourceType);

		AuditLogFilterDTO filter = AuditLogFilterDTO.builder()
				.userId(userId)
				.actionType(actionType)
				.resourceType(resourceType)
				.stationId(stationId)
				.startDate(startDate != null ? java.time.LocalDateTime.parse(startDate) : null)
				.endDate(endDate != null ? java.time.LocalDateTime.parse(endDate) : null)
				.page(page)
				.size(size)
				.build();

		Page<AuditLogResponseDTO> auditLogs = auditService.getAuditLogs(filter);
		return ResponseEntity.ok(auditLogs);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get audit log by ID",
			description = "Retrieves a specific audit log entry by its ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Audit log found")
	@ApiResponse(responseCode = "404", description = "Audit log not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<AuditLogResponseDTO> getAuditLogById(@PathVariable UUID id) {
		logger.info("Get audit log request for ID: {}", id);
		// Note: This would require a getById method in AuditService
		// For now, return not implemented
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@GetMapping("/export")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Export audit logs",
			description = "Exports audit logs to CSV/Excel format with optional filtering. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Audit logs exported successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<byte[]> exportAuditLogs(
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) AuditActionType actionType,
			@RequestParam(required = false) String resourceType,
			@RequestParam(required = false) UUID stationId,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@RequestParam(defaultValue = "csv") String format) {
		logger.info("Export audit logs request: format={}", format);

		AuditLogFilterDTO filter = AuditLogFilterDTO.builder()
				.userId(userId)
				.actionType(actionType)
				.resourceType(resourceType)
				.stationId(stationId)
				.startDate(startDate != null ? java.time.LocalDateTime.parse(startDate) : null)
				.endDate(endDate != null ? java.time.LocalDateTime.parse(endDate) : null)
				.page(0)
				.size(Integer.MAX_VALUE)
				.build();

		byte[] exportData = auditService.exportAuditLogs(filter);

		HttpHeaders headers = new HttpHeaders();
		if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentDispositionFormData("attachment", "audit-logs.xlsx");
		} else {
			headers.setContentType(MediaType.TEXT_PLAIN);
			headers.setContentDispositionFormData("attachment", "audit-logs.csv");
		}

		return ResponseEntity.ok()
				.headers(headers)
				.body(exportData);
	}

}


