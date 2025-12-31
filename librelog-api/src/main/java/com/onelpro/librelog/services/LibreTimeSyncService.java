package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ClockExportResultDTO;
import com.onelpro.librelog.dto.ClockExportValidationResultDTO;
import com.onelpro.librelog.dto.LibreTimeExportDTO;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface for LibreTime integration and synchronization.
 */
public interface LibreTimeSyncService {

	/**
	 * Validates a clock template before export.
	 * Checks for missing files, invalid timing, metadata completeness, and file existence in LibreTime.
	 * 
	 * @param clockTemplateId The ID of the clock template to validate
	 * @return Validation result with errors and warnings
	 */
	ClockExportValidationResultDTO validateClockTemplate(UUID clockTemplateId);

	/**
	 * Exports a clock template to LibreTime format.
	 * Includes validation checks for missing files, invalid timing, and metadata completeness.
	 * 
	 * @param clockTemplateId The ID of the clock template to export
	 * @return The clock template in LibreTime export format
	 */
	LibreTimeExportDTO exportClock(UUID clockTemplateId);

	/**
	 * Pushes a clock template export to LibreTime API.
	 * Handles validation errors and returns detailed results (success, failures, warnings).
	 * 
	 * @param clockTemplateId The ID of the clock template to export and push
	 * @return Detailed export result with success status, failures, and warnings
	 */
	ClockExportResultDTO pushClockToLibreTime(UUID clockTemplateId);

	/**
	 * Generates a daily log from a clock template for a specific date.
	 * 
	 * @param clockTemplateId The ID of the clock template
	 * @param date The date for which to generate the log
	 * @return The log in LibreTime format
	 */
	LibreTimeExportDTO generateLogFromClock(UUID clockTemplateId, LocalDate date);

	/**
	 * Generates a log from a clock template for a date range.
	 * Supports recurring schedules and conflict detection.
	 * 
	 * @param clockTemplateId The ID of the clock template
	 * @param startDate The start date of the range
	 * @param endDate The end date of the range (inclusive)
	 * @return The log in LibreTime format with show instances for the date range
	 */
	LibreTimeExportDTO generateLogFromClock(UUID clockTemplateId, LocalDate startDate, LocalDate endDate);

	/**
	 * Pushes a generated log to LibreTime API.
	 * 
	 * @param clockTemplateId The ID of the clock template
	 * @param date The date for which to generate and push the log
	 * @return The response from LibreTime API
	 */
	String pushLogToLibreTime(UUID clockTemplateId, LocalDate date);

	/**
	 * Detects scheduling conflicts for show instances.
	 * Checks for overlapping show instances and conflicts with existing shows in LibreTime.
	 * 
	 * @param export The clock export to check for conflicts
	 * @return List of conflict descriptions
	 */
	java.util.List<String> detectSchedulingConflicts(LibreTimeExportDTO export);

}

