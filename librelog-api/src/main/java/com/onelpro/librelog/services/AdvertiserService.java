package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AdvertiserRequestDTO;
import com.onelpro.librelog.dto.AdvertiserResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for advertiser operations.
 */
public interface AdvertiserService {

	AdvertiserResponseDTO create(AdvertiserRequestDTO request);

	AdvertiserResponseDTO getById(UUID id);

	List<AdvertiserResponseDTO> getAll();

	List<AdvertiserResponseDTO> getByAgencyId(UUID agencyId);

	List<AdvertiserResponseDTO> getBySalesRepId(UUID salesRepId);

	AdvertiserResponseDTO update(UUID id, AdvertiserRequestDTO request);

	void delete(UUID id);

}

