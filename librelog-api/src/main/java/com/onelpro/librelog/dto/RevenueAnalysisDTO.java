package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for revenue analysis results for a clock template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalysisDTO {

	/**
	 * Total commercial inventory in minutes (sum of all break durations).
	 */
	private Double totalInventoryMinutes;

	/**
	 * Total potential revenue for the clock template.
	 */
	private BigDecimal potentialRevenue;

	/**
	 * Revenue breakdown by break (break ID -> revenue).
	 */
	@Builder.Default
	private Map<UUID, BigDecimal> revenueByBreak = new HashMap<>();

	/**
	 * Revenue breakdown by avail type (avail type name -> revenue).
	 */
	@Builder.Default
	private Map<String, BigDecimal> revenueByAvailType = new HashMap<>();

	/**
	 * List of warnings or informational messages about the revenue analysis.
	 */
	@Builder.Default
	private List<String> warnings = new ArrayList<>();

	/**
	 * Number of commercial breaks in the clock template.
	 */
	private Integer breakCount;

	/**
	 * Average revenue per minute.
	 */
	private BigDecimal averageRevenuePerMinute;

	/**
	 * Average revenue per break.
	 */
	private BigDecimal averageRevenuePerBreak;

}

