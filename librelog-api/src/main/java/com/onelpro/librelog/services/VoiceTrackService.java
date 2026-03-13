package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.VoiceTrackRequestDTO;
import com.onelpro.librelog.dto.VoiceTrackResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for voice track management.
 */
public interface VoiceTrackService {

	/**
	 * Create a new voice track.
	 */
	VoiceTrackResponseDTO create(VoiceTrackRequestDTO request);

	/**
	 * Get voice track by ID.
	 */
	VoiceTrackResponseDTO getById(UUID id);

	/**
	 * Get all voice tracks for a station.
	 */
	List<VoiceTrackResponseDTO> getByStationId(UUID stationId);

	/**
	 * Get voice tracks scheduled for a specific date.
	 */
	List<VoiceTrackResponseDTO> getByStationAndDate(UUID stationId, LocalDate date);

	/**
	 * Get all voice tracks.
	 */
	List<VoiceTrackResponseDTO> getAll();

	/**
	 * Update a voice track.
	 */
	VoiceTrackResponseDTO update(UUID id, VoiceTrackRequestDTO request);

	/**
	 * Delete a voice track.
	 */
	void delete(UUID id);

	/**
	 * Update voice track status.
	 */
	VoiceTrackResponseDTO updateStatus(UUID id, String status);

	/**
	 * Mark voice track as recorded.
	 */
	VoiceTrackResponseDTO markAsRecorded(UUID id, String fileUrl, Integer durationSeconds);

	/**
	 * Get upcoming scheduled voice tracks.
	 */
	List<VoiceTrackResponseDTO> getUpcoming(UUID stationId);

}
