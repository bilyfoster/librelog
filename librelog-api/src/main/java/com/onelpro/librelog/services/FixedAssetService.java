package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.FixedAssetRequestDTO;
import com.onelpro.librelog.dto.FixedAssetResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for fixed asset operations.
 */
public interface FixedAssetService {

	/**
	 * Create a new fixed asset.
	 *
	 * @param request the fixed asset creation request
	 * @return the created fixed asset response
	 */
	FixedAssetResponseDTO create(FixedAssetRequestDTO request);

	/**
	 * Get a fixed asset by ID.
	 *
	 * @param id the fixed asset ID
	 * @return the fixed asset response
	 */
	FixedAssetResponseDTO getById(UUID id);

	/**
	 * Get all fixed assets for a clock template.
	 *
	 * @param clockTemplateId the clock template ID
	 * @return list of fixed asset responses
	 */
	List<FixedAssetResponseDTO> getByClockTemplateId(UUID clockTemplateId);

	/**
	 * Update an existing fixed asset.
	 *
	 * @param id      the fixed asset ID
	 * @param request the fixed asset update request
	 * @return the updated fixed asset response
	 */
	FixedAssetResponseDTO update(UUID id, FixedAssetRequestDTO request);

	/**
	 * Delete a fixed asset by ID.
	 *
	 * @param id the fixed asset ID
	 */
	void delete(UUID id);

}

