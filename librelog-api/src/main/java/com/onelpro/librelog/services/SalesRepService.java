package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.SalesRepRequestDTO;
import com.onelpro.librelog.dto.SalesRepResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for sales representative operations.
 */
public interface SalesRepService {

	SalesRepResponseDTO create(SalesRepRequestDTO request);

	SalesRepResponseDTO getById(UUID id);

	List<SalesRepResponseDTO> getAll();

	List<SalesRepResponseDTO> getBySalesTeamId(UUID salesTeamId);

	SalesRepResponseDTO update(UUID id, SalesRepRequestDTO request);

	void delete(UUID id);

}

