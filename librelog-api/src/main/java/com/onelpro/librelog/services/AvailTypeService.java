package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AvailTypeRequestDTO;
import com.onelpro.librelog.dto.AvailTypeResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for avail type operations.
 */
public interface AvailTypeService {

	/**
	 * Create a new avail type.
	 *
	 * @param request the avail type creation request
	 * @return the created avail type response
	 */
	AvailTypeResponseDTO create(AvailTypeRequestDTO request);

	/**
	 * Get an avail type by ID.
	 *
	 * @param id the avail type ID
	 * @return the avail type response
	 */
	AvailTypeResponseDTO getById(UUID id);

	/**
	 * Get all avail types.
	 *
	 * @return list of avail type responses
	 */
	List<AvailTypeResponseDTO> getAll();

	/**
	 * Get all active avail types.
	 *
	 * @return list of active avail type responses
	 */
	List<AvailTypeResponseDTO> getActive();

	/**
	 * Update an existing avail type.
	 *
	 * @param id      the avail type ID
	 * @param request the avail type update request
	 * @return the updated avail type response
	 */
	AvailTypeResponseDTO update(UUID id, AvailTypeRequestDTO request);

	/**
	 * Delete an avail type by ID.
	 *
	 * @param id the avail type ID
	 */
	void delete(UUID id);

}

