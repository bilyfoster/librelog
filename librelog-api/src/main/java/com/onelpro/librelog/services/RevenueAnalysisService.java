package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.RevenueAnalysisDTO;

import java.util.UUID;

/**
 * Service interface for revenue analysis of clock templates.
 */
public interface RevenueAnalysisService {

	/**
	 * Calculate revenue impact for a clock template based on its breaks and spot rates.
	 * @param clockTemplateId The ID of the clock template to analyze
	 * @return Revenue analysis DTO containing total revenue, breakdowns, and warnings
	 */
	RevenueAnalysisDTO calculateRevenueImpact(UUID clockTemplateId);

}

