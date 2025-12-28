package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AutomationCommandRequestDTO;
import com.onelpro.librelog.dto.AutomationCommandResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for automation command operations.
 */
public interface AutomationCommandService {

	/**
	 * Create a new automation command.
	 *
	 * @param request the automation command creation request
	 * @return the created automation command response
	 */
	AutomationCommandResponseDTO create(AutomationCommandRequestDTO request);

	/**
	 * Get an automation command by ID.
	 *
	 * @param id the automation command ID
	 * @return the automation command response
	 */
	AutomationCommandResponseDTO getById(UUID id);

	/**
	 * Get all automation commands for a clock template.
	 *
	 * @param clockTemplateId the clock template ID
	 * @return list of automation command responses
	 */
	List<AutomationCommandResponseDTO> getByClockTemplateId(UUID clockTemplateId);

	/**
	 * Update an existing automation command.
	 *
	 * @param id      the automation command ID
	 * @param request the automation command update request
	 * @return the updated automation command response
	 */
	AutomationCommandResponseDTO update(UUID id, AutomationCommandRequestDTO request);

	/**
	 * Delete an automation command by ID.
	 *
	 * @param id the automation command ID
	 */
	void delete(UUID id);

}

