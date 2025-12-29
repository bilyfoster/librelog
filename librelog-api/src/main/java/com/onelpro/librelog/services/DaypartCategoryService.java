package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.DaypartCategoryRequestDTO;
import com.onelpro.librelog.dto.DaypartCategoryResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for daypart category operations.
 */
public interface DaypartCategoryService {

	DaypartCategoryResponseDTO create(DaypartCategoryRequestDTO request);

	DaypartCategoryResponseDTO getById(UUID id);

	List<DaypartCategoryResponseDTO> getAll();

	DaypartCategoryResponseDTO update(UUID id, DaypartCategoryRequestDTO request);

	void delete(UUID id);

}

