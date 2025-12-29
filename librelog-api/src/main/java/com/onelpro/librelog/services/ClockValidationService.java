package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ClockValidationResultDTO;

import java.util.UUID;

/**
 * Service interface for clock template validation operations.
 */
public interface ClockValidationService {

	/**
	 * Validate a clock template for conflicts, overlaps, and timing issues.
	 *
	 * @param clockTemplateId the clock template ID to validate
	 * @return validation result with errors, warnings, and conflict details
	 */
	ClockValidationResultDTO validateClock(UUID clockTemplateId);

	/**
	 * Detect conflicts in a clock template.
	 *
	 * @param clockTemplateId the clock template ID
	 * @return validation result with conflict details
	 */
	ClockValidationResultDTO detectConflicts(UUID clockTemplateId);

	/**
	 * Check for overlapping elements in a clock template.
	 *
	 * @param clockTemplateId the clock template ID
	 * @return validation result with overlap details
	 */
	ClockValidationResultDTO checkOverlaps(UUID clockTemplateId);

	/**
	 * Validate timing constraints in a clock template.
	 *
	 * @param clockTemplateId the clock template ID
	 * @return validation result with timing validation details
	 */
	ClockValidationResultDTO validateTiming(UUID clockTemplateId);

}

