package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.SpotCopyRequestDTO;
import com.onelpro.librelog.dto.SpotCopyResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for spot copy/script management.
 */
public interface SpotCopyService {

	/**
	 * Create a new copy version for a campaign.
	 */
	SpotCopyResponseDTO create(SpotCopyRequestDTO request);

	/**
	 * Get copy by ID.
	 */
	SpotCopyResponseDTO getById(UUID id);

	/**
	 * Get all copy versions for a campaign.
	 */
	List<SpotCopyResponseDTO> getByCampaignId(UUID campaignId);

	/**
	 * Get the latest approved copy for a campaign.
	 */
	SpotCopyResponseDTO getLatestApprovedByCampaignId(UUID campaignId);

	/**
	 * Update copy.
	 */
	SpotCopyResponseDTO update(UUID id, SpotCopyRequestDTO request);

	/**
	 * Delete copy.
	 */
	void delete(UUID id);

	/**
	 * Approve copy.
	 */
	SpotCopyResponseDTO approve(UUID id);

	/**
	 * Reject copy with reason.
	 */
	SpotCopyResponseDTO reject(UUID id, String reason);

	/**
	 * Get copy pending approval.
	 */
	List<SpotCopyResponseDTO> getPendingApproval();

}
