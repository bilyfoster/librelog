package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ChannelRequestDTO;
import com.onelpro.librelog.dto.ChannelResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.ChannelRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of channel service.
 */
@Service
public class ChannelServiceImpl implements ChannelService {

	private static final Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

	private final ChannelRepository channelRepository;
	private final StationRepository stationRepository;

	public ChannelServiceImpl(ChannelRepository channelRepository, StationRepository stationRepository) {
		this.channelRepository = channelRepository;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public ChannelResponseDTO create(ChannelRequestDTO request) {
		logger.info("Creating channel with name: {} for station: {}", request.getName(), request.getStationId());

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> {
					logger.warn("Station not found with ID: {}", request.getStationId());
					return new NotFoundException("Station not found with ID: " + request.getStationId());
				});

		Channel channel = Channel.builder()
				.station(station)
				.name(request.getName())
				.channelNumber(request.getChannelNumber())
				.formatType(request.getFormatType())
				.description(request.getDescription())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		channel = channelRepository.save(channel);
		logger.info("Channel created successfully with ID: {}", channel.getId());

		return mapToResponseDTO(channel);
	}

	@Override
	public ChannelResponseDTO getById(UUID id) {
		logger.debug("Fetching channel with ID: {}", id);
		Channel channel = channelRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Channel not found with ID: {}", id);
					return new NotFoundException("Channel not found with ID: " + id);
				});
		return mapToResponseDTO(channel);
	}

	@Override
	public List<ChannelResponseDTO> getAll() {
		logger.debug("Fetching all channels");
		return channelRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ChannelResponseDTO> getByStationId(UUID stationId) {
		logger.debug("Fetching channels for station: {}", stationId);
		return channelRepository.findByStationId(stationId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ChannelResponseDTO update(UUID id, ChannelRequestDTO request) {
		logger.info("Updating channel with ID: {}", id);
		Channel channel = channelRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Channel not found with ID: {}", id);
					return new NotFoundException("Channel not found with ID: " + id);
				});

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> {
					logger.warn("Station not found with ID: {}", request.getStationId());
					return new NotFoundException("Station not found with ID: " + request.getStationId());
				});

		channel.setStation(station);
		channel.setName(request.getName());
		channel.setChannelNumber(request.getChannelNumber());
		channel.setFormatType(request.getFormatType());
		channel.setDescription(request.getDescription());
		channel.setIsActive(request.getIsActive());
		channel.setUpdatedAt(LocalDateTime.now());

		channel = channelRepository.save(channel);
		logger.info("Channel updated successfully with ID: {}", channel.getId());

		return mapToResponseDTO(channel);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting channel with ID: {}", id);
		if (!channelRepository.existsById(id)) {
			logger.warn("Channel not found with ID: {}", id);
			throw new NotFoundException("Channel not found with ID: " + id);
		}
		channelRepository.deleteById(id);
		logger.info("Channel deleted successfully with ID: {}", id);
	}

	private ChannelResponseDTO mapToResponseDTO(Channel channel) {
		return ChannelResponseDTO.builder()
				.id(channel.getId())
				.stationId(channel.getStation().getId())
				.stationName(channel.getStation().getName())
				.stationCallSign(channel.getStation().getCallSign())
				.name(channel.getName())
				.channelNumber(channel.getChannelNumber())
				.formatType(channel.getFormatType())
				.description(channel.getDescription())
				.isActive(channel.getIsActive())
				.createdAt(channel.getCreatedAt())
				.updatedAt(channel.getUpdatedAt())
				.build();
	}
}

