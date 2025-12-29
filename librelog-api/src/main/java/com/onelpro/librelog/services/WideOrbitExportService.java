package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.WideOrbitExportDTO;

import java.util.UUID;

/**
 * Service interface for exporting clock templates to WideOrbit format.
 */
public interface WideOrbitExportService {

	/**
	 * Exports a clock template to WideOrbit format.
	 *
	 * @param clockTemplateId The ID of the clock template to export
	 * @return WideOrbitExportDTO containing the clock template in WideOrbit format
	 */
	WideOrbitExportDTO exportClock(UUID clockTemplateId);

	/**
	 * Exports a clock template to WideOrbit XML format (if WideOrbit uses XML).
	 *
	 * @param clockTemplateId The ID of the clock template to export
	 * @return XML string representation of the clock template
	 */
	String exportClockToXml(UUID clockTemplateId);

	/**
	 * Exports a clock template to WideOrbit JSON format.
	 *
	 * @param clockTemplateId The ID of the clock template to export
	 * @return JSON string representation of the clock template
	 */
	String exportClockToJson(UUID clockTemplateId);
}

