package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.SpotRequestDTO;
import com.onelpro.librelog.dto.SpotResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for spot management operations.
 */
public interface SpotService {

	/**
	 * Create a new spot.
	 */
	SpotResponseDTO create(SpotRequestDTO request);

	/**
	 * Get spot by ID.
	 */
	SpotResponseDTO getById(UUID id);

	/**
	 * Get all spots for a campaign.
	 */
	List<SpotResponseDTO> getByCampaignId(UUID campaignId);

	/**
	 * Get all spots for a station on a specific date.
	 */
	List<SpotResponseDTO> getByStationAndDate(UUID stationId, LocalDate date);

	/**
	 * Get spots within a date range.
	 */
	List<SpotResponseDTO> getByStationAndDateRange(UUID stationId, LocalDate startDate, LocalDate endDate);

	/**
	 * Update a spot.
	 */
	SpotResponseDTO update(UUID id, SpotRequestDTO request);

	/**
	 * Delete a spot.
	 */
	void delete(UUID id);

	/**
	 * Update spot status.
	 */
	SpotResponseDTO updateStatus(UUID id, String status);

	/**
	 * Mark spot as aired.
	 */
	SpotResponseDTO markAsAired(UUID id);

	/**
	 * Create a makegood spot.
	 */
	SpotResponseDTO createMakegood(UUID missedSpotId, SpotRequestDTO request);

}
