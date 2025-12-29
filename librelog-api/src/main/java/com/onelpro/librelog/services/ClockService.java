package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ClockTemplateRequestDTO;
import com.onelpro.librelog.dto.ClockTemplateResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for clock template operations.
 */
public interface ClockService {

	ClockTemplateResponseDTO create(ClockTemplateRequestDTO request);

	ClockTemplateResponseDTO getById(UUID id);

	List<ClockTemplateResponseDTO> getAll();

	List<ClockTemplateResponseDTO> getByChannelId(UUID channelId);

	ClockTemplateResponseDTO update(UUID id, ClockTemplateRequestDTO request);

	void delete(UUID id);

	/**
	 * Clones a clock template with all its associated elements (breaks, fixed assets, automation commands).
	 * @param sourceId The ID of the clock template to clone.
	 * @param newName The name for the cloned clock template.
	 * @return The response DTO of the cloned clock template.
	 */
	ClockTemplateResponseDTO cloneClockTemplate(UUID sourceId, String newName);

}

