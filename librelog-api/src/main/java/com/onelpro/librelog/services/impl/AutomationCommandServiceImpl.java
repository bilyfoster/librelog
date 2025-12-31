package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AutomationCommandRequestDTO;
import com.onelpro.librelog.dto.AutomationCommandResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AutomationCommand;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.AutomationCommandRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.services.AutomationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of automation command service.
 */
@Service
public class AutomationCommandServiceImpl implements AutomationCommandService {

	private static final Logger logger = LoggerFactory.getLogger(AutomationCommandServiceImpl.class);

	private final AutomationCommandRepository automationCommandRepository;
	private final ClockTemplateRepository clockTemplateRepository;

	public AutomationCommandServiceImpl(
			AutomationCommandRepository automationCommandRepository,
			ClockTemplateRepository clockTemplateRepository) {
		this.automationCommandRepository = automationCommandRepository;
		this.clockTemplateRepository = clockTemplateRepository;
	}

	@Override
	@Transactional
	public AutomationCommandResponseDTO create(AutomationCommandRequestDTO request) {
		logger.info("Creating automation command with type: {} for clock template: {}", request.getCommandType(), request.getClockTemplateId());

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
					return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
				});

		AutomationCommand automationCommand = AutomationCommand.builder()
				.clockTemplate(clockTemplate)
				.commandType(request.getCommandType())
				.triggerTime(request.getTriggerTime())
				.priority(request.getPriority())
				.parameters(request.getParameters())
				// LibreTime fields
				.libreTimePlaylistId(request.getLibreTimePlaylistId())
				.libreTimeSmartBlockId(request.getLibreTimeSmartBlockId())
				.libreTimeCommandType(request.getLibreTimeCommandType())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		automationCommand = automationCommandRepository.save(automationCommand);
		logger.info("Automation command created successfully with ID: {}", automationCommand.getId());

		return mapToResponseDTO(automationCommand);
	}

	@Override
	public AutomationCommandResponseDTO getById(UUID id) {
		logger.debug("Fetching automation command with ID: {}", id);
		AutomationCommand automationCommand = automationCommandRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Automation command not found with ID: {}", id);
					return new NotFoundException("Automation command not found with ID: " + id);
				});
		return mapToResponseDTO(automationCommand);
	}

	@Override
	public List<AutomationCommandResponseDTO> getByClockTemplateId(UUID clockTemplateId) {
		logger.debug("Fetching automation commands for clock template: {}", clockTemplateId);
		
		// Validate clock template exists
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			logger.warn("Clock template not found with ID: {}", clockTemplateId);
			throw new NotFoundException("Clock template not found with ID: " + clockTemplateId);
		}

		return automationCommandRepository.findByClockTemplateId(clockTemplateId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AutomationCommandResponseDTO update(UUID id, AutomationCommandRequestDTO request) {
		logger.info("Updating automation command with ID: {}", id);
		AutomationCommand automationCommand = automationCommandRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Automation command not found with ID: {}", id);
					return new NotFoundException("Automation command not found with ID: " + id);
				});

		// Validate clock template exists if changed
		if (!automationCommand.getClockTemplate().getId().equals(request.getClockTemplateId())) {
			ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
					.orElseThrow(() -> {
						logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
						return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
					});
			automationCommand.setClockTemplate(clockTemplate);
		}

		automationCommand.setCommandType(request.getCommandType());
		automationCommand.setTriggerTime(request.getTriggerTime());
		automationCommand.setPriority(request.getPriority());
		automationCommand.setParameters(request.getParameters());
		// LibreTime fields
		automationCommand.setLibreTimePlaylistId(request.getLibreTimePlaylistId());
		automationCommand.setLibreTimeSmartBlockId(request.getLibreTimeSmartBlockId());
		automationCommand.setLibreTimeCommandType(request.getLibreTimeCommandType());
		automationCommand.setUpdatedAt(LocalDateTime.now());

		automationCommand = automationCommandRepository.save(automationCommand);
		logger.info("Automation command updated successfully with ID: {}", automationCommand.getId());

		return mapToResponseDTO(automationCommand);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting automation command with ID: {}", id);
		if (!automationCommandRepository.existsById(id)) {
			logger.warn("Automation command not found with ID: {}", id);
			throw new NotFoundException("Automation command not found with ID: " + id);
		}
		automationCommandRepository.deleteById(id);
		logger.info("Automation command deleted successfully with ID: {}", id);
	}

	private AutomationCommandResponseDTO mapToResponseDTO(AutomationCommand automationCommand) {
		ClockTemplate clockTemplate = automationCommand.getClockTemplate();

		return AutomationCommandResponseDTO.builder()
				.id(automationCommand.getId())
				.clockTemplateId(clockTemplate.getId())
				.clockTemplateName(clockTemplate.getName())
				.commandType(automationCommand.getCommandType())
				.triggerTime(automationCommand.getTriggerTime())
				.priority(automationCommand.getPriority())
				.parameters(automationCommand.getParameters())
				// LibreTime fields
				.libreTimePlaylistId(automationCommand.getLibreTimePlaylistId())
				.libreTimeSmartBlockId(automationCommand.getLibreTimeSmartBlockId())
				.libreTimeCommandType(automationCommand.getLibreTimeCommandType())
				.createdAt(automationCommand.getCreatedAt())
				.updatedAt(automationCommand.getUpdatedAt())
				.build();
	}

}

