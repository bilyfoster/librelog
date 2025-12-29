package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.DaypartAssignmentRequestDTO;
import com.onelpro.librelog.dto.DaypartAssignmentResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing daypart-to-clock template assignments.
 */
public interface DaypartAssignmentService {

	DaypartAssignmentResponseDTO create(DaypartAssignmentRequestDTO request);

	DaypartAssignmentResponseDTO getById(UUID id);

	List<DaypartAssignmentResponseDTO> getByDaypartId(UUID daypartId);

	List<DaypartAssignmentResponseDTO> getByClockTemplateId(UUID clockTemplateId);

	DaypartAssignmentResponseDTO update(UUID id, DaypartAssignmentRequestDTO request);

	void delete(UUID id);

	void deleteByDaypartId(UUID daypartId);

	void deleteByClockTemplateId(UUID clockTemplateId);

}

