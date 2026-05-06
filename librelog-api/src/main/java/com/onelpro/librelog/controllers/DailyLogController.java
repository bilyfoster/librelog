package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.LibreTimeExportDTO;
import com.onelpro.librelog.services.LibreTimeSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for daily log generation and publishing endpoints.
 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "Daily Logs", description = "Daily log generation and LibreTime publishing")
public class DailyLogController {

	private static final Logger logger = LoggerFactory.getLogger(DailyLogController.class);

	private final LibreTimeSyncService libreTimeSyncService;

	public DailyLogController(LibreTimeSyncService libreTimeSyncService) {
		this.libreTimeSyncService = libreTimeSyncService;
	}

	@GetMapping("/generate")
	@Operation(
			summary = "Generate daily log preview",
			description = "Generates a daily log from a clock template for preview (not published)"
	)
	@ApiResponse(responseCode = "200", description = "Log generated successfully")
	public ResponseEntity<Map<String, Object>> generateLog(
			@RequestParam UUID clockTemplateId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		logger.info("GET /api/logs/generate - Generating log for clock {} on date {}", clockTemplateId, date);

		LibreTimeExportDTO export = libreTimeSyncService.generateLogFromClock(clockTemplateId, date);
		
		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("date", date);
		response.put("export", export);
		response.put("generated", true);
		
		return ResponseEntity.ok(response);
	}

	@GetMapping("/generate-range")
	@Operation(
			summary = "Generate log preview for date range",
			description = "Generates logs from a clock template for a date range (inclusive) for preview"
	)
	@ApiResponse(responseCode = "200", description = "Range log generated successfully")
	public ResponseEntity<Map<String, Object>> generateLogRange(
			@RequestParam UUID clockTemplateId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		logger.info("GET /api/logs/generate-range - Generating logs for clock {} from {} to {}",
				clockTemplateId, startDate, endDate);

		LibreTimeExportDTO export = libreTimeSyncService.generateLogFromClock(clockTemplateId, startDate, endDate);

		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("startDate", startDate);
		response.put("endDate", endDate);
		response.put("showInstanceCount", export.getShowInstances() != null ? export.getShowInstances().size() : 0);
		response.put("export", export);
		response.put("generated", true);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/exceptions")
	@Operation(
			summary = "Generate scheduling exceptions report",
			description = "Builds logs for a date range and returns unfilled/conflicted scheduling slots"
	)
	@ApiResponse(responseCode = "200", description = "Scheduling exception report generated successfully")
	public ResponseEntity<Map<String, Object>> generateSchedulingExceptions(
			@RequestParam UUID clockTemplateId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		logger.info("GET /api/logs/exceptions - Generating scheduling exceptions for clock {} from {} to {}",
				clockTemplateId, startDate, endDate);

		var report = libreTimeSyncService.generateSchedulingExceptionReport(clockTemplateId, startDate, endDate);

		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("startDate", startDate);
		response.put("endDate", endDate);
		response.put("report", report);
		response.put("generated", true);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/exceptions/critical")
	@Operation(
			summary = "Generate critical scheduling exceptions report",
			description = "Returns only critical scheduling blockers (unfilled spot/interview slots)"
	)
	@ApiResponse(responseCode = "200", description = "Critical scheduling exception report generated successfully")
	public ResponseEntity<Map<String, Object>> generateCriticalSchedulingExceptions(
			@RequestParam UUID clockTemplateId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		logger.info("GET /api/logs/exceptions/critical - Generating critical scheduling exceptions for clock {} from {} to {}",
				clockTemplateId, startDate, endDate);

		var report = libreTimeSyncService.generateSchedulingExceptionReport(clockTemplateId, startDate, endDate);
		List<com.onelpro.librelog.dto.SchedulingExceptionReportDTO.SchedulingExceptionItemDTO> critical = new ArrayList<>();
		if (report.getExceptions() != null) {
			for (var item : report.getExceptions()) {
				if (item == null || item.getCategory() == null) {
					continue;
				}
				if ("SPOT".equalsIgnoreCase(item.getCategory()) || "INTERVIEW".equalsIgnoreCase(item.getCategory())) {
					critical.add(item);
				}
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("startDate", startDate);
		response.put("endDate", endDate);
		response.put("totalHoursGenerated", report.getTotalHoursGenerated());
		response.put("totalCriticalExceptions", critical.size());
		response.put("criticalExceptions", critical);
		response.put("generated", true);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/generate-range-with-critical")
	@Operation(
			summary = "Generate logs with critical exceptions",
			description = "Generates range logs and returns critical scheduling blockers in one response"
	)
	@ApiResponse(responseCode = "200", description = "Range logs and critical exceptions generated successfully")
	public ResponseEntity<Map<String, Object>> generateRangeWithCriticalExceptions(
			@RequestParam UUID clockTemplateId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		logger.info("GET /api/logs/generate-range-with-critical - Generating logs and critical exceptions for clock {} from {} to {}",
				clockTemplateId, startDate, endDate);

		LibreTimeExportDTO export = libreTimeSyncService.generateLogFromClock(clockTemplateId, startDate, endDate);
		var report = libreTimeSyncService.generateSchedulingExceptionReport(clockTemplateId, startDate, endDate);
		List<com.onelpro.librelog.dto.SchedulingExceptionReportDTO.SchedulingExceptionItemDTO> critical = new ArrayList<>();
		if (report.getExceptions() != null) {
			for (var item : report.getExceptions()) {
				if (item == null || item.getCategory() == null) {
					continue;
				}
				if ("SPOT".equalsIgnoreCase(item.getCategory()) || "INTERVIEW".equalsIgnoreCase(item.getCategory())) {
					critical.add(item);
				}
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("startDate", startDate);
		response.put("endDate", endDate);
		response.put("showInstanceCount", export.getShowInstances() != null ? export.getShowInstances().size() : 0);
		response.put("export", export);
		response.put("totalCriticalExceptions", critical.size());
		response.put("criticalExceptions", critical);
		response.put("generated", true);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/{clockTemplateId}/publish")
	@Operation(
			summary = "Publish log to LibreTime",
			description = "Generates and publishes a daily log to LibreTime for a specific date"
	)
	@ApiResponse(responseCode = "200", description = "Log published successfully")
	@ApiResponse(responseCode = "500", description = "Failed to publish log")
	public ResponseEntity<Map<String, Object>> publishLog(
			@PathVariable UUID clockTemplateId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		logger.info("POST /api/logs/{}/publish - Publishing log to LibreTime for date {}", clockTemplateId, date);

		String result = libreTimeSyncService.pushLogToLibreTime(clockTemplateId, date);
		
		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("date", date);
		response.put("status", "published");
		response.put("result", result);
		
		return ResponseEntity.ok(response);
	}

	@GetMapping("/validate")
	@Operation(
			summary = "Validate clock template for export",
			description = "Validates a clock template before exporting to LibreTime"
	)
	@ApiResponse(responseCode = "200", description = "Validation completed")
	public ResponseEntity<Map<String, Object>> validateClock(
			@RequestParam UUID clockTemplateId) {
		logger.info("GET /api/logs/validate - Validating clock template {}", clockTemplateId);

		var validation = libreTimeSyncService.validateClockTemplate(clockTemplateId);
		
		Map<String, Object> response = new HashMap<>();
		response.put("clockTemplateId", clockTemplateId);
		response.put("valid", validation.getErrors().isEmpty());
		response.put("errors", validation.getErrors());
		response.put("warnings", validation.getWarnings());
		
		return ResponseEntity.ok(response);
	}

}
