package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AgencyRequestDTO;
import com.onelpro.librelog.dto.AgencyResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for agency operations.
 */
public interface AgencyService {

	AgencyResponseDTO create(AgencyRequestDTO request);

	AgencyResponseDTO getById(UUID id);

	List<AgencyResponseDTO> getAll();

	AgencyResponseDTO update(UUID id, AgencyRequestDTO request);

	void delete(UUID id);

}

