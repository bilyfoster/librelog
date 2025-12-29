package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ClockTemplateRequestDTO;
import com.onelpro.librelog.dto.ClockTemplateResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.ChannelRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
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

	public ClockServiceImpl(
			ClockTemplateRepository clockTemplateRepository,
			ChannelRepository channelRepository) {
		this.clockTemplateRepository = clockTemplateRepository;
		this.channelRepository = channelRepository;
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

