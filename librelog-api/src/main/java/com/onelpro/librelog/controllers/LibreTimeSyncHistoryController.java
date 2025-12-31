package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.SyncHistoryResponseDTO;
import com.onelpro.librelog.dto.SyncStatisticsResponseDTO;
import com.onelpro.librelog.enums.SyncType;
import com.onelpro.librelog.services.LibreTimeSyncHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for LibreTime sync history and statistics.
 */
@RestController
@RequestMapping("/api/libretime/sync-history")
@Tag(name = "LibreTime Sync History", description = "Sync history and statistics endpoints")
public class LibreTimeSyncHistoryController {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeSyncHistoryController.class);

	private final LibreTimeSyncHistoryService syncHistoryService;

	public LibreTimeSyncHistoryController(LibreTimeSyncHistoryService syncHistoryService) {
		this.syncHistoryService = syncHistoryService;
	}

	@GetMapping("/{historyId}")
	@Operation(
			summary = "Get sync history record",
			description = "Retrieves a specific sync history record by ID."
	)
	@ApiResponse(responseCode = "200", description = "Sync history record retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Sync history record not found")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<SyncHistoryResponseDTO> getSyncHistory(@PathVariable UUID historyId) {
		logger.info("GET /api/libretime/sync-history/{} - Getting sync history record", historyId);
		SyncHistoryResponseDTO history = syncHistoryService.getSyncHistory(historyId);
		return ResponseEntity.ok(history);
	}

	@GetMapping
	@Operation(
			summary = "Get sync history records",
			description = "Retrieves sync history records. Can be filtered by date range, sync type, or user."
	)
	@ApiResponse(responseCode = "200", description = "Sync history records retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<List<SyncHistoryResponseDTO>> getSyncHistory(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			@RequestParam(required = false) SyncType syncType,
			@RequestParam(required = false) UUID userId) {
		logger.info("GET /api/libretime/sync-history - Getting sync history (startDate: {}, endDate: {}, syncType: {}, userId: {})", 
				startDate, endDate, syncType, userId);
		
		List<SyncHistoryResponseDTO> history;
		if (syncType != null) {
			history = syncHistoryService.getSyncHistoryByType(syncType);
		} else if (userId != null) {
			history = syncHistoryService.getSyncHistoryByUser(userId);
		} else if (startDate != null && endDate != null) {
			history = syncHistoryService.getSyncHistoryByDateRange(startDate, endDate);
		} else {
			// Return all history (in production, might want to add pagination)
			history = syncHistoryService.getSyncHistoryByDateRange(
					LocalDateTime.now().minusMonths(1), LocalDateTime.now());
		}
		
		return ResponseEntity.ok(history);
	}

	@GetMapping("/statistics")
	@Operation(
			summary = "Get sync statistics",
			description = "Retrieves sync statistics including total files, synced count, failed count, pending count, and success rate. Can be filtered by date range."
	)
	@ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<SyncStatisticsResponseDTO> getSyncStatistics(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
		logger.info("GET /api/libretime/sync-history/statistics - Getting sync statistics (startDate: {}, endDate: {})", 
				startDate, endDate);
		SyncStatisticsResponseDTO statistics = syncHistoryService.getSyncStatistics(startDate, endDate);
		return ResponseEntity.ok(statistics);
	}

}

