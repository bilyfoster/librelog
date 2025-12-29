package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.LibreTimeExportDTO;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface for LibreTime integration and synchronization.
 */
public interface LibreTimeSyncService {

	/**
	 * Exports a clock template to LibreTime format.
	 * 
	 * @param clockTemplateId The ID of the clock template to export
	 * @return The clock template in LibreTime export format
	 */
	LibreTimeExportDTO exportClock(UUID clockTemplateId);

	/**
	 * Pushes a clock template export to LibreTime API.
	 * 
	 * @param clockTemplateId The ID of the clock template to export and push
	 * @return The response from LibreTime API
	 */
	String pushClockToLibreTime(UUID clockTemplateId);

	/**
	 * Generates a daily log from a clock template for a specific date.
	 * 
	 * @param clockTemplateId The ID of the clock template
	 * @param date The date for which to generate the log
	 * @return The log in LibreTime format
	 */
	LibreTimeExportDTO generateLogFromClock(UUID clockTemplateId, LocalDate date);

	/**
	 * Pushes a generated log to LibreTime API.
	 * 
	 * @param clockTemplateId The ID of the clock template
	 * @param date The date for which to generate and push the log
	 * @return The response from LibreTime API
	 */
	String pushLogToLibreTime(UUID clockTemplateId, LocalDate date);

}

