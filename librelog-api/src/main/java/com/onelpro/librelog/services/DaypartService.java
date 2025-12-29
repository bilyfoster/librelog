package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.DaypartRequestDTO;
import com.onelpro.librelog.dto.DaypartResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for daypart operations.
 */
public interface DaypartService {

	DaypartResponseDTO create(DaypartRequestDTO request);

	DaypartResponseDTO getById(UUID id);

	List<DaypartResponseDTO> getAll();

	DaypartResponseDTO update(UUID id, DaypartRequestDTO request);

	void delete(UUID id);

}

