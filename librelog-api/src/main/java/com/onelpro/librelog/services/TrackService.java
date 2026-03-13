package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.TrackRequestDTO;
import com.onelpro.librelog.dto.TrackResponseDTO;
import com.onelpro.librelog.enums.TrackType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for track/music library management.
 */
public interface TrackService {

	/**
	 * Create a new track.
	 */
	TrackResponseDTO create(TrackRequestDTO request);

	/**
	 * Get track by ID.
	 */
	TrackResponseDTO getById(UUID id);

	/**
	 * Get all tracks for a station.
	 */
	List<TrackResponseDTO> getByStationId(UUID stationId);

	/**
	 * Get tracks by type.
	 */
	List<TrackResponseDTO> getByType(UUID stationId, TrackType type);

	/**
	 * Get all tracks.
	 */
	List<TrackResponseDTO> getAll();

	/**
	 * Update a track.
	 */
	TrackResponseDTO update(UUID id, TrackRequestDTO request);

	/**
	 * Delete a track.
	 */
	void delete(UUID id);

	/**
	 * Search tracks by title or artist.
	 */
	List<TrackResponseDTO> search(String query);

	/**
	 * Record track play (increment play count and update last played).
	 */
	TrackResponseDTO recordPlay(UUID id);

}
