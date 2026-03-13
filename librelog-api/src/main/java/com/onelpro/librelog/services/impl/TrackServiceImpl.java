package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.TrackRequestDTO;
import com.onelpro.librelog.dto.TrackResponseDTO;
import com.onelpro.librelog.enums.TrackType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.Track;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.repositories.TrackRepository;
import com.onelpro.librelog.services.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of track management service.
 */
@Service
public class TrackServiceImpl implements TrackService {

	private static final Logger logger = LoggerFactory.getLogger(TrackServiceImpl.class);

	private final TrackRepository trackRepository;
	private final StationRepository stationRepository;

	public TrackServiceImpl(TrackRepository trackRepository, StationRepository stationRepository) {
		this.trackRepository = trackRepository;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public TrackResponseDTO create(TrackRequestDTO request) {
		logger.info("Creating track: {} by {}", request.getTitle(), request.getArtist());

		Station station = null;
		if (request.getStationId() != null) {
			station = stationRepository.findById(request.getStationId())
					.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));
		}

		Track track = Track.builder()
				.title(request.getTitle())
				.artist(request.getArtist())
				.album(request.getAlbum())
				.type(request.getType() != null ? request.getType() : TrackType.MUSIC)
				.genre(request.getGenre())
				.durationSeconds(request.getDurationSeconds())
				.filepath(request.getFilepath())
				.libretimeId(request.getLibretimeId())
				.station(station)
				.isrc(request.getIsrc())
				.year(request.getYear())
				.bpm(request.getBpm())
				.rating(request.getRating())
				.playCount(0)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		Track saved = trackRepository.save(track);
		logger.info("Track created successfully with ID: {}", saved.getId());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public TrackResponseDTO getById(UUID id) {
		logger.debug("Fetching track with ID: {}", id);
		Track track = trackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Track not found with id: " + id));
		return mapToResponseDTO(track);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TrackResponseDTO> getByStationId(UUID stationId) {
		logger.debug("Fetching tracks for station ID: {}", stationId);
		return trackRepository.findByStationId(stationId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TrackResponseDTO> getByType(UUID stationId, TrackType type) {
		logger.debug("Fetching tracks for station ID: {} and type: {}", stationId, type);
		return trackRepository.findByStationIdAndType(stationId, type).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TrackResponseDTO> getAll() {
		logger.debug("Fetching all tracks");
		return trackRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public TrackResponseDTO update(UUID id, TrackRequestDTO request) {
		logger.info("Updating track with ID: {}", id);

		Track track = trackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Track not found with id: " + id));

		if (request.getStationId() != null && 
			(track.getStation() == null || !track.getStation().getId().equals(request.getStationId()))) {
			Station station = stationRepository.findById(request.getStationId())
					.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));
			track.setStation(station);
		}

		track.setTitle(request.getTitle());
		track.setArtist(request.getArtist());
		track.setAlbum(request.getAlbum());
		track.setType(request.getType());
		track.setGenre(request.getGenre());
		track.setDurationSeconds(request.getDurationSeconds());
		track.setFilepath(request.getFilepath());
		track.setLibretimeId(request.getLibretimeId());
		track.setIsrc(request.getIsrc());
		track.setYear(request.getYear());
		track.setBpm(request.getBpm());
		track.setRating(request.getRating());
		track.setUpdatedAt(LocalDateTime.now());

		Track updated = trackRepository.save(track);
		logger.info("Track updated successfully with ID: {}", updated.getId());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting track with ID: {}", id);
		if (!trackRepository.existsById(id)) {
			throw new NotFoundException("Track not found with id: " + id);
		}
		trackRepository.deleteById(id);
		logger.info("Track deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TrackResponseDTO> search(String query) {
		logger.debug("Searching tracks with query: {}", query);
		return trackRepository.searchByTitleOrArtist(query).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public TrackResponseDTO recordPlay(UUID id) {
		logger.info("Recording play for track ID: {}", id);

		Track track = trackRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Track not found with id: " + id));

		track.setPlayCount(track.getPlayCount() + 1);
		track.setLastPlayed(LocalDateTime.now());
		track.setUpdatedAt(LocalDateTime.now());

		Track updated = trackRepository.save(track);
		return mapToResponseDTO(updated);
	}

	private TrackResponseDTO mapToResponseDTO(Track track) {
		return TrackResponseDTO.builder()
				.id(track.getId())
				.title(track.getTitle())
				.artist(track.getArtist())
				.album(track.getAlbum())
				.type(track.getType())
				.genre(track.getGenre())
				.durationSeconds(track.getDurationSeconds())
				.filepath(track.getFilepath())
				.libretimeId(track.getLibretimeId())
				.stationId(track.getStation() != null ? track.getStation().getId() : null)
				.stationName(track.getStation() != null ? track.getStation().getName() : null)
				.isrc(track.getIsrc())
				.year(track.getYear())
				.bpm(track.getBpm())
				.rating(track.getRating())
				.playCount(track.getPlayCount())
				.lastPlayed(track.getLastPlayed())
				.createdAt(track.getCreatedAt())
				.updatedAt(track.getUpdatedAt())
				.build();
	}

}
