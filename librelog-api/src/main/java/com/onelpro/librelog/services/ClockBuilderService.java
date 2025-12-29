package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.*;

import java.util.UUID;

/**
 * Service interface for managing clock template structure including breaks,
 * fixed assets, and automation commands.
 */
public interface ClockBuilderService {

	/**
	 * Get the complete clock structure including template and all elements.
	 * @param clockTemplateId The ID of the clock template
	 * @return DTO containing clock template with all breaks, fixed assets, and automation commands
	 */
	ClockTemplateWithBreaksDTO getClockStructure(UUID clockTemplateId);

	/**
	 * Add a break to a clock template.
	 * @param clockTemplateId The ID of the clock template
	 * @param request Break structure request DTO
	 * @return Created break structure response DTO
	 */
	BreakStructureResponseDTO addBreak(UUID clockTemplateId, BreakStructureRequestDTO request);

	/**
	 * Update a break in a clock template.
	 * @param breakId The ID of the break to update
	 * @param request Break structure request DTO
	 * @return Updated break structure response DTO
	 */
	BreakStructureResponseDTO updateBreak(UUID breakId, BreakStructureRequestDTO request);

	/**
	 * Remove a break from a clock template.
	 * @param breakId The ID of the break to remove
	 */
	void removeBreak(UUID breakId);

	/**
	 * Add a fixed asset to a clock template.
	 * @param clockTemplateId The ID of the clock template
	 * @param request Fixed asset request DTO
	 * @return Created fixed asset response DTO
	 */
	FixedAssetResponseDTO addFixedAsset(UUID clockTemplateId, FixedAssetRequestDTO request);

	/**
	 * Update a fixed asset in a clock template.
	 * @param fixedAssetId The ID of the fixed asset to update
	 * @param request Fixed asset request DTO
	 * @return Updated fixed asset response DTO
	 */
	FixedAssetResponseDTO updateFixedAsset(UUID fixedAssetId, FixedAssetRequestDTO request);

	/**
	 * Remove a fixed asset from a clock template.
	 * @param fixedAssetId The ID of the fixed asset to remove
	 */
	void removeFixedAsset(UUID fixedAssetId);

	/**
	 * Add an automation command to a clock template.
	 * @param clockTemplateId The ID of the clock template
	 * @param request Automation command request DTO
	 * @return Created automation command response DTO
	 */
	AutomationCommandResponseDTO addAutomationCommand(UUID clockTemplateId, AutomationCommandRequestDTO request);

	/**
	 * Update an automation command in a clock template.
	 * @param commandId The ID of the automation command to update
	 * @param request Automation command request DTO
	 * @return Updated automation command response DTO
	 */
	AutomationCommandResponseDTO updateAutomationCommand(UUID commandId, AutomationCommandRequestDTO request);

	/**
	 * Remove an automation command from a clock template.
	 * @param commandId The ID of the automation command to remove
	 */
	void removeAutomationCommand(UUID commandId);

}

