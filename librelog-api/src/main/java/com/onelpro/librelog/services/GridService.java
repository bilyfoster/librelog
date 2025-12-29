package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.GridDaypartMappingRequestDTO;
import com.onelpro.librelog.dto.GridRequestDTO;
import com.onelpro.librelog.dto.GridResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing weekly grids and daypart mappings.
 */
public interface GridService {

	GridResponseDTO create(GridRequestDTO request);

	GridResponseDTO getById(UUID id);

	List<GridResponseDTO> getAll();

	List<GridResponseDTO> getByChannelId(UUID channelId);

	GridResponseDTO update(UUID id, GridRequestDTO request);

	void delete(UUID id);

	GridResponseDTO.GridDaypartMappingResponseDTO addDaypartMapping(GridDaypartMappingRequestDTO request);

	void removeDaypartMapping(UUID mappingId);

	List<GridResponseDTO.GridDaypartMappingResponseDTO> getDaypartMappingsByGridId(UUID gridId);

}

