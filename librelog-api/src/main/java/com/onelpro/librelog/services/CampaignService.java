package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.CampaignRequestDTO;
import com.onelpro.librelog.dto.CampaignResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for campaign management operations.
 */
public interface CampaignService {

	/**
	 * Create a new campaign.
	 */
	CampaignResponseDTO create(CampaignRequestDTO request);

	/**
	 * Get campaign by ID.
	 */
	CampaignResponseDTO getById(UUID id);

	/**
	 * Get all campaigns for a station.
	 */
	List<CampaignResponseDTO> getByStationId(UUID stationId);

	/**
	 * Get all campaigns.
	 */
	List<CampaignResponseDTO> getAll();

	/**
	 * Update a campaign.
	 */
	CampaignResponseDTO update(UUID id, CampaignRequestDTO request);

	/**
	 * Delete a campaign.
	 */
	void delete(UUID id);

	/**
	 * Update campaign status.
	 */
	CampaignResponseDTO updateStatus(UUID id, String status);

	/**
	 * Get active campaigns for a station.
	 */
	List<CampaignResponseDTO> getActiveCampaigns(UUID stationId);

}
