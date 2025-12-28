package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.BreakStructureRequestDTO;
import com.onelpro.librelog.dto.BreakStructureResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for break structure operations.
 */
public interface BreakStructureService {

	/**
	 * Create a new break structure.
	 *
	 * @param request the break structure creation request
	 * @return the created break structure response
	 */
	BreakStructureResponseDTO create(BreakStructureRequestDTO request);

	/**
	 * Get a break structure by ID.
	 *
	 * @param id the break structure ID
	 * @return the break structure response
	 */
	BreakStructureResponseDTO getById(UUID id);

	/**
	 * Get all break structures for a clock template.
	 *
	 * @param clockTemplateId the clock template ID
	 * @return list of break structure responses
	 */
	List<BreakStructureResponseDTO> getByClockTemplateId(UUID clockTemplateId);

	/**
	 * Update an existing break structure.
	 *
	 * @param id      the break structure ID
	 * @param request the break structure update request
	 * @return the updated break structure response
	 */
	BreakStructureResponseDTO update(UUID id, BreakStructureRequestDTO request);

	/**
	 * Delete a break structure by ID.
	 *
	 * @param id the break structure ID
	 */
	void delete(UUID id);

}

