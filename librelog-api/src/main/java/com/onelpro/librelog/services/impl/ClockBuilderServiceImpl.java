package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of clock builder service for managing clock template structure.
 */
@Service
public class ClockBuilderServiceImpl implements ClockBuilderService {

	private static final Logger logger = LoggerFactory.getLogger(ClockBuilderServiceImpl.class);

	private final ClockService clockService;
	private final BreakStructureService breakStructureService;
	private final FixedAssetService fixedAssetService;
	private final AutomationCommandService automationCommandService;
	private final ClockValidationService clockValidationService;
	private final ClockTemplateRepository clockTemplateRepository;

	public ClockBuilderServiceImpl(
			ClockService clockService,
			BreakStructureService breakStructureService,
			FixedAssetService fixedAssetService,
			AutomationCommandService automationCommandService,
			ClockValidationService clockValidationService,
			ClockTemplateRepository clockTemplateRepository) {
		this.clockService = clockService;
		this.breakStructureService = breakStructureService;
		this.fixedAssetService = fixedAssetService;
		this.automationCommandService = automationCommandService;
		this.clockValidationService = clockValidationService;
		this.clockTemplateRepository = clockTemplateRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ClockTemplateWithBreaksDTO getClockStructure(UUID clockTemplateId) {
		logger.debug("Getting clock structure for template: {}", clockTemplateId);

		// Get clock template
		ClockTemplateResponseDTO clockTemplate = clockService.getById(clockTemplateId);
		if (clockTemplate == null) {
			logger.warn("Clock template not found with ID: {}", clockTemplateId);
			throw new NotFoundException("Clock template not found with ID: " + clockTemplateId);
		}

		// Get all breaks
		var breaks = breakStructureService.getByClockTemplateId(clockTemplateId);

		// Get all fixed assets
		var fixedAssets = fixedAssetService.getByClockTemplateId(clockTemplateId);

		// Get all automation commands
		var automationCommands = automationCommandService.getByClockTemplateId(clockTemplateId);

		// Build response DTO
		return ClockTemplateWithBreaksDTO.builder()
				.id(clockTemplate.getId())
				.name(clockTemplate.getName())
				.description(clockTemplate.getDescription())
				.channelId(clockTemplate.getChannelId())
				.channelName(clockTemplate.getChannelName())
				.isActive(clockTemplate.getIsActive())
				.createdAt(clockTemplate.getCreatedAt())
				.updatedAt(clockTemplate.getUpdatedAt())
				.breaks(breaks)
				.fixedAssets(fixedAssets)
				.automationCommands(automationCommands)
				.build();
	}

	@Override
	@Transactional
	public BreakStructureResponseDTO addBreak(UUID clockTemplateId, BreakStructureRequestDTO request) {
		logger.info("Adding break to clock template: {}", clockTemplateId);

		// Validate clock template exists
		validateClockTemplateExists(clockTemplateId);

		// Ensure request has correct clock template ID
		request.setClockTemplateId(clockTemplateId);

		// Create break
		BreakStructureResponseDTO response = breakStructureService.create(request);

		// Validate clock after adding break
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(clockTemplateId);
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after adding break. Errors: {}", validationResult.getErrors());
			// Note: We don't fail here, but log warnings. The validation result can be checked by the caller.
		}

		return response;
	}

	@Override
	@Transactional
	public BreakStructureResponseDTO updateBreak(UUID breakId, BreakStructureRequestDTO request) {
		logger.info("Updating break: {}", breakId);

		// Update break
		BreakStructureResponseDTO response = breakStructureService.update(breakId, request);

		// Validate clock after updating break
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(request.getClockTemplateId());
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after updating break. Errors: {}", validationResult.getErrors());
		}

		return response;
	}

	@Override
	@Transactional
	public void removeBreak(UUID breakId) {
		logger.info("Removing break: {}", breakId);

		// Get break to find clock template ID before deletion
		BreakStructureResponseDTO breakStructure = breakStructureService.getById(breakId);
		UUID clockTemplateId = breakStructure.getClockTemplateId();

		// Delete break
		breakStructureService.delete(breakId);

		// Validate clock after removing break
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(clockTemplateId);
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after removing break. Errors: {}", validationResult.getErrors());
		}
	}

	@Override
	@Transactional
	public FixedAssetResponseDTO addFixedAsset(UUID clockTemplateId, FixedAssetRequestDTO request) {
		logger.info("Adding fixed asset to clock template: {}", clockTemplateId);

		// Validate clock template exists
		validateClockTemplateExists(clockTemplateId);

		// Ensure request has correct clock template ID
		request.setClockTemplateId(clockTemplateId);

		// Create fixed asset
		FixedAssetResponseDTO response = fixedAssetService.create(request);

		// Validate clock after adding fixed asset
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(clockTemplateId);
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after adding fixed asset. Errors: {}", validationResult.getErrors());
		}

		return response;
	}

	@Override
	@Transactional
	public FixedAssetResponseDTO updateFixedAsset(UUID fixedAssetId, FixedAssetRequestDTO request) {
		logger.info("Updating fixed asset: {}", fixedAssetId);

		// Update fixed asset
		FixedAssetResponseDTO response = fixedAssetService.update(fixedAssetId, request);

		// Validate clock after updating fixed asset
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(request.getClockTemplateId());
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after updating fixed asset. Errors: {}", validationResult.getErrors());
		}

		return response;
	}

	@Override
	@Transactional
	public void removeFixedAsset(UUID fixedAssetId) {
		logger.info("Removing fixed asset: {}", fixedAssetId);

		// Get fixed asset to find clock template ID before deletion
		FixedAssetResponseDTO fixedAsset = fixedAssetService.getById(fixedAssetId);
		UUID clockTemplateId = fixedAsset.getClockTemplateId();

		// Delete fixed asset
		fixedAssetService.delete(fixedAssetId);

		// Validate clock after removing fixed asset
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(clockTemplateId);
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after removing fixed asset. Errors: {}", validationResult.getErrors());
		}
	}

	@Override
	@Transactional
	public AutomationCommandResponseDTO addAutomationCommand(UUID clockTemplateId, AutomationCommandRequestDTO request) {
		logger.info("Adding automation command to clock template: {}", clockTemplateId);

		// Validate clock template exists
		validateClockTemplateExists(clockTemplateId);

		// Ensure request has correct clock template ID
		request.setClockTemplateId(clockTemplateId);

		// Create automation command
		AutomationCommandResponseDTO response = automationCommandService.create(request);

		// Validate clock after adding automation command
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(clockTemplateId);
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after adding automation command. Errors: {}", validationResult.getErrors());
		}

		return response;
	}

	@Override
	@Transactional
	public AutomationCommandResponseDTO updateAutomationCommand(UUID commandId, AutomationCommandRequestDTO request) {
		logger.info("Updating automation command: {}", commandId);

		// Update automation command
		AutomationCommandResponseDTO response = automationCommandService.update(commandId, request);

		// Validate clock after updating automation command
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(request.getClockTemplateId());
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after updating automation command. Errors: {}", validationResult.getErrors());
		}

		return response;
	}

	@Override
	@Transactional
	public void removeAutomationCommand(UUID commandId) {
		logger.info("Removing automation command: {}", commandId);

		// Get automation command to find clock template ID before deletion
		AutomationCommandResponseDTO command = automationCommandService.getById(commandId);
		UUID clockTemplateId = command.getClockTemplateId();

		// Delete automation command
		automationCommandService.delete(commandId);

		// Validate clock after removing automation command
		ClockValidationResultDTO validationResult = clockValidationService.validateClock(clockTemplateId);
		if (!validationResult.getIsValid()) {
			logger.warn("Clock validation failed after removing automation command. Errors: {}", validationResult.getErrors());
		}
	}

	/**
	 * Validate that a clock template exists.
	 * @param clockTemplateId The ID of the clock template to validate
	 * @throws NotFoundException if the clock template does not exist
	 */
	private void validateClockTemplateExists(UUID clockTemplateId) {
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			logger.warn("Clock template not found with ID: {}", clockTemplateId);
			throw new NotFoundException("Clock template not found with ID: " + clockTemplateId);
		}
	}

}

