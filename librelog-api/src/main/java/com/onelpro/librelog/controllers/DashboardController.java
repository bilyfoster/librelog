package com.onelpro.librelog.controllers;

import com.onelpro.librelog.models.Invoice;
import com.onelpro.librelog.repositories.AdvertiserRepository;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.InvoiceRepository;
import com.onelpro.librelog.repositories.SpotRepository;
import com.onelpro.librelog.repositories.TrackRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for dashboard statistics and overview.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and overview")
public class DashboardController {

	private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

	private final TrackRepository trackRepository;
	private final SpotRepository spotRepository;
	private final AdvertiserRepository advertiserRepository;
	private final CampaignRepository campaignRepository;
	private final InvoiceRepository invoiceRepository;

	public DashboardController(TrackRepository trackRepository,
	                           SpotRepository spotRepository,
	                           AdvertiserRepository advertiserRepository,
	                           CampaignRepository campaignRepository,
	                           InvoiceRepository invoiceRepository) {
		this.trackRepository = trackRepository;
		this.spotRepository = spotRepository;
		this.advertiserRepository = advertiserRepository;
		this.campaignRepository = campaignRepository;
		this.invoiceRepository = invoiceRepository;
	}

	@GetMapping("/stats")
	@Operation(
			summary = "Get dashboard statistics",
			description = "Returns overview statistics for the dashboard including counts of tracks, spots, advertisers, campaigns, and billing info"
	)
	@ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
	public ResponseEntity<Map<String, Object>> getDashboardStats() {
		logger.debug("GET /api/dashboard/stats - Fetching dashboard statistics");

		Map<String, Object> stats = new HashMap<>();

		// Content counts
		stats.put("totalTracks", trackRepository.count());
		stats.put("totalSpots", spotRepository.count());
		stats.put("totalSpotsToday", spotRepository.findByScheduledDate(LocalDate.now()).size());

		// Business counts
		stats.put("totalAdvertisers", advertiserRepository.count());
		stats.put("activeAdvertisers", advertiserRepository.countByIsActive(true));
		stats.put("totalCampaigns", campaignRepository.count());
		stats.put("activeCampaigns", campaignRepository.countByStatus(com.onelpro.librelog.enums.CampaignStatus.ACTIVE));

		// Billing overview
		stats.put("totalInvoices", invoiceRepository.count());
		stats.put("overdueInvoices", invoiceRepository.countByStatus(Invoice.InvoiceStatus.OVERDUE));
		stats.put("pendingInvoices", invoiceRepository.countByStatus(Invoice.InvoiceStatus.SENT));
		
		// Revenue
		BigDecimal totalRevenue = invoiceRepository.getTotalRevenueByStatus(Invoice.InvoiceStatus.PAID);
		stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
		
		BigDecimal outstandingRevenue = invoiceRepository.getTotalRevenueByStatus(Invoice.InvoiceStatus.SENT);
		BigDecimal partialRevenue = invoiceRepository.getTotalRevenueByStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
		BigDecimal outstanding = (outstandingRevenue != null ? outstandingRevenue : BigDecimal.ZERO)
				.add(partialRevenue != null ? partialRevenue : BigDecimal.ZERO);
		stats.put("outstandingRevenue", outstanding);

		// Today's date
		stats.put("today", LocalDate.now().toString());

		return ResponseEntity.ok(stats);
	}

	@GetMapping("/recent-activity")
	@Operation(
			summary = "Get recent activity",
			description = "Returns recent activity for the dashboard"
	)
	@ApiResponse(responseCode = "200", description = "Activity retrieved successfully")
	public ResponseEntity<Map<String, Object>> getRecentActivity() {
		logger.debug("GET /api/dashboard/recent-activity - Fetching recent activity");

		Map<String, Object> activity = new HashMap<>();

		// Recent campaigns (last 5)
		activity.put("recentCampaigns", campaignRepository.findTop5ByOrderByCreatedAtDesc());

		// Recent advertisers (last 5)
		activity.put("recentAdvertisers", advertiserRepository.findTop5ByOrderByCreatedAtDesc());

		// Today's spots
		activity.put("spotsToday", spotRepository.findByScheduledDate(LocalDate.now()));

		// Overdue invoices
		activity.put("overdueInvoices", invoiceRepository.findByStatusAndDueDateBefore(
				Invoice.InvoiceStatus.SENT, LocalDate.now()));

		return ResponseEntity.ok(activity);
	}

}
