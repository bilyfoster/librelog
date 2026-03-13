package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.VoiceTrackRequestDTO;
import com.onelpro.librelog.dto.VoiceTrackResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.VoiceTrack;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.repositories.VoiceTrackRepository;
import com.onelpro.librelog.services.VoiceTrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of voice track management service.
 */
@Service
public class VoiceTrackServiceImpl implements VoiceTrackService {

	private static final Logger logger = LoggerFactory.getLogger(VoiceTrackServiceImpl.class);

	private final VoiceTrackRepository voiceTrackRepository;
	private final StationRepository stationRepository;

	public VoiceTrackServiceImpl(VoiceTrackRepository voiceTrackRepository,
	                             StationRepository stationRepository) {
		this.voiceTrackRepository = voiceTrackRepository;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public VoiceTrackResponseDTO create(VoiceTrackRequestDTO request) {
		logger.info("Creating voice track: {}", request.getTitle());

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

		VoiceTrack voiceTrack = VoiceTrack.builder()
				.title(request.getTitle())
				.station(station)
				.showName(request.getShowName())
				.segmentType(request.getSegmentType())
				.fileUrl(request.getFileUrl())
				.filePath(request.getFilePath())
				.durationSeconds(request.getDurationSeconds())
				.scheduledDate(request.getScheduledDate())
				.scheduledTime(request.getScheduledTime())
				.scriptText(request.getScriptText())
				.recordedText(request.getRecordedText())
				.status(request.getStatus() != null ? request.getStatus() : "DRAFT")
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		VoiceTrack saved = voiceTrackRepository.save(voiceTrack);
		logger.info("Voice track created successfully with ID: {}", saved.getId());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public VoiceTrackResponseDTO getById(UUID id) {
		logger.debug("Fetching voice track with ID: {}", id);
		VoiceTrack voiceTrack = voiceTrackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Voice track not found with id: " + id));
		return mapToResponseDTO(voiceTrack);
	}

	@Override
	@Transactional(readOnly = true)
	public List<VoiceTrackResponseDTO> getByStationId(UUID stationId) {
		logger.debug("Fetching voice tracks for station ID: {}", stationId);
		return voiceTrackRepository.findByStationId(stationId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<VoiceTrackResponseDTO> getByStationAndDate(UUID stationId, LocalDate date) {
		logger.debug("Fetching voice tracks for station ID: {} on date: {}", stationId, date);
		return voiceTrackRepository.findByStationIdAndScheduledDate(stationId, date).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<VoiceTrackResponseDTO> getAll() {
		logger.debug("Fetching all voice tracks");
		return voiceTrackRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public VoiceTrackResponseDTO update(UUID id, VoiceTrackRequestDTO request) {
		logger.info("Updating voice track with ID: {}", id);

		VoiceTrack voiceTrack = voiceTrackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Voice track not found with id: " + id));

		if (request.getStationId() != null && 
			(voiceTrack.getStation() == null || !voiceTrack.getStation().getId().equals(request.getStationId()))) {
			Station station = stationRepository.findById(request.getStationId())
					.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));
			voiceTrack.setStation(station);
		}

		voiceTrack.setTitle(request.getTitle());
		voiceTrack.setShowName(request.getShowName());
		voiceTrack.setSegmentType(request.getSegmentType());
		voiceTrack.setFileUrl(request.getFileUrl());
		voiceTrack.setFilePath(request.getFilePath());
		voiceTrack.setDurationSeconds(request.getDurationSeconds());
		voiceTrack.setScheduledDate(request.getScheduledDate());
		voiceTrack.setScheduledTime(request.getScheduledTime());
		voiceTrack.setScriptText(request.getScriptText());
		voiceTrack.setRecordedText(request.getRecordedText());
		voiceTrack.setUpdatedAt(LocalDateTime.now());

		VoiceTrack updated = voiceTrackRepository.save(voiceTrack);
		logger.info("Voice track updated successfully with ID: {}", updated.getId());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting voice track with ID: {}", id);
		if (!voiceTrackRepository.existsById(id)) {
			throw new NotFoundException("Voice track not found with id: " + id);
		}
		voiceTrackRepository.deleteById(id);
		logger.info("Voice track deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public VoiceTrackResponseDTO updateStatus(UUID id, String status) {
		logger.info("Updating voice track status for ID: {} to {}", id, status);

		VoiceTrack voiceTrack = voiceTrackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Voice track not found with id: " + id));

		voiceTrack.setStatus(status.toUpperCase());
		voiceTrack.setUpdatedAt(LocalDateTime.now());

		VoiceTrack updated = voiceTrackRepository.save(voiceTrack);
		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public VoiceTrackResponseDTO markAsRecorded(UUID id, String fileUrl, Integer durationSeconds) {
		logger.info("Marking voice track as recorded with ID: {}", id);

		VoiceTrack voiceTrack = voiceTrackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Voice track not found with id: " + id));

		voiceTrack.setStatus("RECORDED");
		voiceTrack.setFileUrl(fileUrl);
		voiceTrack.setDurationSeconds(durationSeconds);
		voiceTrack.setRecordedAt(LocalDateTime.now());
		voiceTrack.setUpdatedAt(LocalDateTime.now());

		VoiceTrack updated = voiceTrackRepository.save(voiceTrack);
		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public List<VoiceTrackResponseDTO> getUpcoming(UUID stationId) {
		logger.debug("Fetching upcoming voice tracks for station ID: {}", stationId);
		return voiceTrackRepository.findByStationIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(
				stationId, LocalDate.now()).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	private VoiceTrackResponseDTO mapToResponseDTO(VoiceTrack voiceTrack) {
		return VoiceTrackResponseDTO.builder()
				.id(voiceTrack.getId())
				.title(voiceTrack.getTitle())
				.stationId(voiceTrack.getStation() != null ? voiceTrack.getStation().getId() : null)
				.stationName(voiceTrack.getStation() != null ? voiceTrack.getStation().getName() : null)
				.showName(voiceTrack.getShowName())
				.segmentType(voiceTrack.getSegmentType())
				.fileUrl(voiceTrack.getFileUrl())
				.filePath(voiceTrack.getFilePath())
				.durationSeconds(voiceTrack.getDurationSeconds())
				.scheduledDate(voiceTrack.getScheduledDate())
				.scheduledTime(voiceTrack.getScheduledTime())
				.scriptText(voiceTrack.getScriptText())
				.recordedText(voiceTrack.getRecordedText())
				.status(voiceTrack.getStatus())
				.createdById(voiceTrack.getCreatedBy() != null ? voiceTrack.getCreatedBy().getId() : null)
				.createdByName(voiceTrack.getCreatedBy() != null ? voiceTrack.getCreatedBy().getEmail() : null)
				.recordedById(voiceTrack.getRecordedBy() != null ? voiceTrack.getRecordedBy().getId() : null)
				.recordedByName(voiceTrack.getRecordedBy() != null ? voiceTrack.getRecordedBy().getEmail() : null)
				.recordedAt(voiceTrack.getRecordedAt())
				.createdAt(voiceTrack.getCreatedAt())
				.updatedAt(voiceTrack.getUpdatedAt())
				.build();
	}

}
