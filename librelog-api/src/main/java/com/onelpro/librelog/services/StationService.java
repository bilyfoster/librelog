package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for station operations.
 */
public interface StationService {

	StationResponseDTO create(StationRequestDTO request);

	StationResponseDTO getById(UUID id);

	List<StationResponseDTO> getAll();

	StationResponseDTO update(UUID id, StationRequestDTO request);

	void delete(UUID id);

}

