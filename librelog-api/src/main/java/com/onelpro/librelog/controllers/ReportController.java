package com.onelpro.librelog.controllers;

import com.onelpro.librelog.services.CampaignService;
import com.onelpro.librelog.services.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for reporting endpoints.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Traffic and scheduling reports")
public class ReportController {

	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

	private final CampaignService campaignService;
	private final SpotService spotService;

	public ReportController(CampaignService campaignService, SpotService spotService) {
		this.campaignService = campaignService;
		this.spotService = spotService;
	}

	@GetMapping("/dashboard")
	@Operation(
			summary = "Get dashboard report",
			description = "Returns summary statistics for the dashboard"
	)
	@ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully")
	public ResponseEntity<Map<String, Object>> getDashboardReport(
			@RequestParam(required = false) UUID stationId) {
		logger.debug("GET /api/reports/dashboard - Fetching dashboard report");

		Map<String, Object> report = new HashMap<>();
		
		// Add summary counts
		if (stationId != null) {
			report.put("activeCampaigns", campaignService.getActiveCampaigns(stationId).size());
			// You can add more station-specific metrics here
		}
		
		report.put("totalCampaigns", campaignService.getAll().size());
		
		return ResponseEntity.ok(report);
	}

	@GetMapping("/daily-traffic")
	@Operation(
			summary = "Get daily traffic report",
			description = "Returns spots scheduled for a specific date"
	)
	@ApiResponse(responseCode = "200", description = "Daily traffic report retrieved successfully")
	public ResponseEntity<Map<String, Object>> getDailyTrafficReport(
			@RequestParam UUID stationId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		logger.debug("GET /api/reports/daily-traffic - Station: {}, Date: {}", stationId, date);

		Map<String, Object> report = new HashMap<>();
		report.put("stationId", stationId);
		report.put("date", date);
		report.put("spots", spotService.getByStationAndDate(stationId, date));
		
		return ResponseEntity.ok(report);
	}

	@GetMapping("/campaign-performance")
	@Operation(
			summary = "Get campaign performance report",
			description = "Returns performance metrics for campaigns"
	)
	@ApiResponse(responseCode = "200", description = "Campaign performance report retrieved successfully")
	public ResponseEntity<Map<String, Object>> getCampaignPerformanceReport(
			@RequestParam(required = false) UUID campaignId) {
		logger.debug("GET /api/reports/campaign-performance - Campaign: {}", campaignId);

		Map<String, Object> report = new HashMap<>();
		
		if (campaignId != null) {
			report.put("campaign", campaignService.getById(campaignId));
			report.put("spots", spotService.getByCampaignId(campaignId));
		} else {
			report.put("campaigns", campaignService.getAll());
		}
		
		return ResponseEntity.ok(report);
	}

}
