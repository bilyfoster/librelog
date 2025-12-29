package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ClockTemplateRequestDTO;
import com.onelpro.librelog.dto.ClockTemplateResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AutomationCommand;
import com.onelpro.librelog.models.BreakStructure;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.FixedAsset;
import com.onelpro.librelog.repositories.AutomationCommandRepository;
import com.onelpro.librelog.repositories.BreakStructureRepository;
import com.onelpro.librelog.repositories.ChannelRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.repositories.FixedAssetRepository;
import com.onelpro.librelog.services.ClockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of clock template service.
 */
@Service
public class ClockServiceImpl implements ClockService {

	private static final Logger logger = LoggerFactory.getLogger(ClockServiceImpl.class);

	private final ClockTemplateRepository clockTemplateRepository;
	private final ChannelRepository channelRepository;
	private final BreakStructureRepository breakStructureRepository;
	private final FixedAssetRepository fixedAssetRepository;
	private final AutomationCommandRepository automationCommandRepository;

	public ClockServiceImpl(
			ClockTemplateRepository clockTemplateRepository,
			ChannelRepository channelRepository,
			BreakStructureRepository breakStructureRepository,
			FixedAssetRepository fixedAssetRepository,
			AutomationCommandRepository automationCommandRepository) {
		this.clockTemplateRepository = clockTemplateRepository;
		this.channelRepository = channelRepository;
		this.breakStructureRepository = breakStructureRepository;
		this.fixedAssetRepository = fixedAssetRepository;
		this.automationCommandRepository = automationCommandRepository;
	}

	@Override
	@Transactional
	public ClockTemplateResponseDTO create(ClockTemplateRequestDTO request) {
		logger.info("Creating clock template with name: {} for channel: {}", request.getName(), request.getChannelId());

		Channel channel = channelRepository.findById(request.getChannelId())
				.orElseThrow(() -> {
					logger.warn("Channel not found with ID: {}", request.getChannelId());
					return new NotFoundException("Channel not found with ID: " + request.getChannelId());
				});

		ClockTemplate clockTemplate = ClockTemplate.builder()
				.channel(channel)
				.name(request.getName())
				.description(request.getDescription())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		clockTemplate = clockTemplateRepository.save(clockTemplate);
		logger.info("Clock template created successfully with ID: {}", clockTemplate.getId());

		return mapToResponseDTO(clockTemplate);
	}

	@Override
	public ClockTemplateResponseDTO getById(UUID id) {
		logger.debug("Fetching clock template with ID: {}", id);
		ClockTemplate clockTemplate = clockTemplateRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", id);
					return new NotFoundException("Clock template not found with ID: " + id);
				});
		return mapToResponseDTO(clockTemplate);
	}

	@Override
	public List<ClockTemplateResponseDTO> getAll() {
		logger.debug("Fetching all clock templates");
		return clockTemplateRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ClockTemplateResponseDTO> getByChannelId(UUID channelId) {
		logger.debug("Fetching clock templates for channel: {}", channelId);
		return clockTemplateRepository.findByChannelId(channelId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ClockTemplateResponseDTO update(UUID id, ClockTemplateRequestDTO request) {
		logger.info("Updating clock template with ID: {}", id);
		ClockTemplate clockTemplate = clockTemplateRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", id);
					return new NotFoundException("Clock template not found with ID: " + id);
				});

		Channel channel = channelRepository.findById(request.getChannelId())
				.orElseThrow(() -> {
					logger.warn("Channel not found with ID: {}", request.getChannelId());
					return new NotFoundException("Channel not found with ID: " + request.getChannelId());
				});

		clockTemplate.setChannel(channel);
		clockTemplate.setName(request.getName());
		clockTemplate.setDescription(request.getDescription());
		clockTemplate.setIsActive(request.getIsActive());
		clockTemplate.setUpdatedAt(LocalDateTime.now());

		clockTemplate = clockTemplateRepository.save(clockTemplate);
		logger.info("Clock template updated successfully with ID: {}", clockTemplate.getId());

		return mapToResponseDTO(clockTemplate);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting clock template with ID: {}", id);
		if (!clockTemplateRepository.existsById(id)) {
			logger.warn("Clock template not found with ID: {}", id);
			throw new NotFoundException("Clock template not found with ID: " + id);
		}
		clockTemplateRepository.deleteById(id);
		logger.info("Clock template deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public ClockTemplateResponseDTO cloneClockTemplate(UUID sourceId, String newName) {
		logger.info("Cloning clock template with ID: {} to new name: {}", sourceId, newName);

		// Fetch source clock template
		ClockTemplate source = clockTemplateRepository.findById(sourceId)
				.orElseThrow(() -> {
					logger.warn("Source clock template not found with ID: {}", sourceId);
					return new NotFoundException("Clock template not found with ID: " + sourceId);
				});

		// Validate new name
		if (newName == null || newName.trim().isEmpty()) {
			logger.warn("Clone failed: new name is required");
			throw new BadRequestException("New name is required for cloning");
		}

		// Create new clock template
		LocalDateTime now = LocalDateTime.now();
		ClockTemplate cloned = ClockTemplate.builder()
				.channel(source.getChannel())
				.name(newName.trim())
				.description(source.getDescription() != null ? "Clone of: " + source.getDescription() : null)
				.isActive(source.getIsActive())
				.createdAt(now)
				.updatedAt(now)
				.build();

		cloned = clockTemplateRepository.save(cloned);
		logger.info("Cloned clock template created with ID: {}", cloned.getId());

		// Clone break structures
		List<BreakStructure> sourceBreaks = breakStructureRepository.findByClockTemplateId(sourceId);
		for (BreakStructure sourceBreak : sourceBreaks) {
			BreakStructure clonedBreak = BreakStructure.builder()
					.clockTemplate(cloned)
					.name(sourceBreak.getName())
					.startTime(sourceBreak.getStartTime())
					.durationSeconds(sourceBreak.getDurationSeconds())
					.isFloating(sourceBreak.getIsFloating())
					.availType(sourceBreak.getAvailType())
					.timingType(sourceBreak.getTimingType())
					.transitionCode(sourceBreak.getTransitionCode())
					.createdAt(now)
					.updatedAt(now)
					.build();
			breakStructureRepository.save(clonedBreak);
		}
		logger.debug("Cloned {} break structures", sourceBreaks.size());

		// Clone fixed assets
		List<FixedAsset> sourceAssets = fixedAssetRepository.findByClockTemplateId(sourceId);
		for (FixedAsset sourceAsset : sourceAssets) {
			FixedAsset clonedAsset = FixedAsset.builder()
					.clockTemplate(cloned)
					.name(sourceAsset.getName())
					.assetType(sourceAsset.getAssetType())
					.startTime(sourceAsset.getStartTime())
					.assetIdentifier(sourceAsset.getAssetIdentifier())
					.timingType(sourceAsset.getTimingType())
					.createdAt(now)
					.updatedAt(now)
					.build();
			fixedAssetRepository.save(clonedAsset);
		}
		logger.debug("Cloned {} fixed assets", sourceAssets.size());

		// Clone automation commands
		List<AutomationCommand> sourceCommands = automationCommandRepository.findByClockTemplateId(sourceId);
		for (AutomationCommand sourceCommand : sourceCommands) {
			AutomationCommand clonedCommand = AutomationCommand.builder()
					.clockTemplate(cloned)
					.commandType(sourceCommand.getCommandType())
					.triggerTime(sourceCommand.getTriggerTime())
					.priority(sourceCommand.getPriority())
					.parameters(sourceCommand.getParameters() != null ? 
							new java.util.HashMap<>(sourceCommand.getParameters()) : null)
					.createdAt(now)
					.updatedAt(now)
					.build();
			automationCommandRepository.save(clonedCommand);
		}
		logger.debug("Cloned {} automation commands", sourceCommands.size());

		logger.info("Clock template cloned successfully. Source: {}, Clone: {}", sourceId, cloned.getId());
		return mapToResponseDTO(cloned);
	}

	private ClockTemplateResponseDTO mapToResponseDTO(ClockTemplate clockTemplate) {
		Channel channel = clockTemplate.getChannel();
		return ClockTemplateResponseDTO.builder()
				.id(clockTemplate.getId())
				.channelId(channel.getId())
				.channelName(channel.getName())
				.stationId(channel.getStation().getId())
				.stationCallSign(channel.getStation().getCallSign())
				.stationName(channel.getStation().getName())
				.name(clockTemplate.getName())
				.description(clockTemplate.getDescription())
				.isActive(clockTemplate.getIsActive())
				.createdAt(clockTemplate.getCreatedAt())
				.updatedAt(clockTemplate.getUpdatedAt())
				.build();
	}

}

