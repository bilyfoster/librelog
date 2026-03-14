package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for order creation and update requests.
 * When advertiserId is provided, advertiser details are auto-populated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	/**
	 * Advertiser ID reference.
	 * When provided, advertiserName, agencyName, and salesRepName are auto-populated.
	 */
	private UUID advertiserId;

	/**
	 * Advertiser name (required if advertiserId not provided).
	 * Can be overridden even when advertiserId is provided.
	 */
	private String advertiserName;

	/**
	 * Agency name - auto-populated from advertiser's agency if advertiserId provided.
	 */
	private String agencyName;

	/**
	 * Sales rep name - auto-populated from advertiser's sales rep if advertiserId provided.
	 */
	private String salesRepName;

	@NotNull(message = "Start date is required")
	private LocalDate startDate;

	@NotNull(message = "End date is required")
	private LocalDate endDate;

	private Integer totalSpots;

	private BigDecimal totalAmount;

	private String notes;

}

