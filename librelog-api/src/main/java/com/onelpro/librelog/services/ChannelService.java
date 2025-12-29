package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ChannelRequestDTO;
import com.onelpro.librelog.dto.ChannelResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for channel operations.
 */
public interface ChannelService {

	ChannelResponseDTO create(ChannelRequestDTO request);

	ChannelResponseDTO getById(UUID id);

	List<ChannelResponseDTO> getAll();

	List<ChannelResponseDTO> getByStationId(UUID stationId);

	ChannelResponseDTO update(UUID id, ChannelRequestDTO request);

	void delete(UUID id);

}

