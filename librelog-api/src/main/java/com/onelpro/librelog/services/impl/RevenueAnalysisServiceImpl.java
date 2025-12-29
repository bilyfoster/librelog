package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.BreakStructureResponseDTO;
import com.onelpro.librelog.dto.RevenueAnalysisDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.services.BreakStructureService;
import com.onelpro.librelog.services.RevenueAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of revenue analysis service for calculating revenue impact of clock templates.
 */
@Service
public class RevenueAnalysisServiceImpl implements RevenueAnalysisService {

	private static final Logger logger = LoggerFactory.getLogger(RevenueAnalysisServiceImpl.class);

	private static final BigDecimal DEFAULT_RATE_PER_MINUTE = new BigDecimal("50.00"); // Default $50 per minute
	private static final int SECONDS_PER_MINUTE = 60;

	private final BreakStructureService breakStructureService;
	private final ClockTemplateRepository clockTemplateRepository;

	@Value("${librelog.revenue.default-rate-per-minute:50.00}")
	private BigDecimal defaultRatePerMinute;

	public RevenueAnalysisServiceImpl(
			BreakStructureService breakStructureService,
			ClockTemplateRepository clockTemplateRepository) {
		this.breakStructureService = breakStructureService;
		this.clockTemplateRepository = clockTemplateRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public RevenueAnalysisDTO calculateRevenueImpact(UUID clockTemplateId) {
		logger.debug("Calculating revenue impact for clock template: {}", clockTemplateId);

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(clockTemplateId)
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", clockTemplateId);
					return new NotFoundException("Clock template not found with ID: " + clockTemplateId);
				});

		// Get all breaks for the clock template
		List<BreakStructureResponseDTO> breaks = breakStructureService.getByClockTemplateId(clockTemplateId);

		// Initialize result
		RevenueAnalysisDTO.RevenueAnalysisDTOBuilder builder = RevenueAnalysisDTO.builder()
				.breakCount(breaks.size())
				.revenueByBreak(new HashMap<>())
				.revenueByAvailType(new HashMap<>())
				.warnings(new java.util.ArrayList<>());

		// Calculate total inventory minutes
		double totalInventorySeconds = breaks.stream()
				.mapToDouble(breakStructure -> breakStructure.getDurationSeconds() != null ?
						breakStructure.getDurationSeconds().doubleValue() : 0.0)
				.sum();
		double totalInventoryMinutes = totalInventorySeconds / SECONDS_PER_MINUTE;
		builder.totalInventoryMinutes(totalInventoryMinutes);

		// Calculate revenue for each break
		BigDecimal totalRevenue = BigDecimal.ZERO;
		Map<UUID, BigDecimal> revenueByBreak = new HashMap<>();
		Map<String, BigDecimal> revenueByAvailType = new HashMap<>();

		for (BreakStructureResponseDTO breakStructure : breaks) {
			// Calculate break duration in minutes
			double breakMinutes = breakStructure.getDurationSeconds() != null ?
					breakStructure.getDurationSeconds().doubleValue() / SECONDS_PER_MINUTE : 0.0;

			// Get rate per minute (use default if no rate configured)
			// TODO: In the future, this could be pulled from a rate card or break-specific configuration
			BigDecimal ratePerMinute = getRatePerMinute(breakStructure);

			// Calculate revenue for this break
			BigDecimal breakRevenue = BigDecimal.valueOf(breakMinutes)
					.multiply(ratePerMinute)
					.setScale(2, RoundingMode.HALF_UP);

			revenueByBreak.put(breakStructure.getId(), breakRevenue);
			totalRevenue = totalRevenue.add(breakRevenue);

			// Aggregate by avail type if available
			if (breakStructure.getAvailTypeName() != null && !breakStructure.getAvailTypeName().isEmpty()) {
				String availTypeName = breakStructure.getAvailTypeName();
				revenueByAvailType.merge(availTypeName, breakRevenue, BigDecimal::add);
			}
		}

		builder.revenueByBreak(revenueByBreak);
		builder.revenueByAvailType(revenueByAvailType);
		builder.potentialRevenue(totalRevenue);

		// Calculate averages
		if (breaks.size() > 0) {
			BigDecimal averageRevenuePerMinute = totalInventoryMinutes > 0 ?
					totalRevenue.divide(BigDecimal.valueOf(totalInventoryMinutes), 2, RoundingMode.HALF_UP) :
					BigDecimal.ZERO;
			BigDecimal averageRevenuePerBreak = totalRevenue.divide(BigDecimal.valueOf(breaks.size()), 2, RoundingMode.HALF_UP);

			builder.averageRevenuePerMinute(averageRevenuePerMinute);
			builder.averageRevenuePerBreak(averageRevenuePerBreak);
		} else {
			builder.averageRevenuePerMinute(BigDecimal.ZERO);
			builder.averageRevenuePerBreak(BigDecimal.ZERO);
		}

		// Build the result first
		RevenueAnalysisDTO result = builder.build();
		List<String> warnings = new java.util.ArrayList<>();

		// Add warnings for low inventory or other issues
		if (breaks.size() == 0) {
			warnings.add("No commercial breaks found in clock template. No revenue potential.");
		} else {
			if (totalInventoryMinutes < 10.0) {
				warnings.add(String.format("Low commercial inventory: %.2f minutes. Industry standard is typically 12-18 minutes per hour.",
						totalInventoryMinutes));
			} else if (totalInventoryMinutes > 20.0) {
				warnings.add(String.format("High commercial inventory: %.2f minutes. This may impact listener experience.",
						totalInventoryMinutes));
			}

			// Add warning if using default rates
			if (revenueByBreak.values().stream().anyMatch(rev -> rev.compareTo(BigDecimal.ZERO) > 0)) {
				warnings.add("Revenue calculation uses default rates. Configure spot rates for accurate revenue projections.");
			}
		}

		// Rebuild with updated warnings
		result = RevenueAnalysisDTO.builder()
				.totalInventoryMinutes(result.getTotalInventoryMinutes())
				.potentialRevenue(result.getPotentialRevenue())
				.revenueByBreak(result.getRevenueByBreak())
				.revenueByAvailType(result.getRevenueByAvailType())
				.warnings(warnings)
				.breakCount(result.getBreakCount())
				.averageRevenuePerMinute(result.getAverageRevenuePerMinute())
				.averageRevenuePerBreak(result.getAverageRevenuePerBreak())
				.build();

		logger.debug("Revenue analysis complete. Total revenue: ${}, Total inventory: {} minutes",
				result.getPotentialRevenue(), result.getTotalInventoryMinutes());

		return result;
	}

	/**
	 * Get the rate per minute for a break structure.
	 * Currently uses default rate, but can be extended to use break-specific or avail-type-specific rates.
	 * @param breakStructure The break structure
	 * @return Rate per minute
	 */
	private BigDecimal getRatePerMinute(BreakStructureResponseDTO breakStructure) {
		// TODO: In the future, this could:
		// 1. Check for break-specific rate configuration
		// 2. Check for avail-type-specific rate
		// 3. Check for time-of-day rate multipliers
		// 4. Check for daypart-specific rates
		// For now, use the configured default rate
		return defaultRatePerMinute != null ? defaultRatePerMinute : DEFAULT_RATE_PER_MINUTE;
	}

}

