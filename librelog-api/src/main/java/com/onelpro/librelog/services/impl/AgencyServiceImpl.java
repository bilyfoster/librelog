package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AgencyRequestDTO;
import com.onelpro.librelog.dto.AgencyResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Agency;
import com.onelpro.librelog.repositories.AgencyRepository;
import com.onelpro.librelog.services.AgencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of agency service.
 */
@Service
public class AgencyServiceImpl implements AgencyService {

	private static final Logger logger = LoggerFactory.getLogger(AgencyServiceImpl.class);

	private final AgencyRepository agencyRepository;

	public AgencyServiceImpl(AgencyRepository agencyRepository) {
		this.agencyRepository = agencyRepository;
	}

	@Override
	@Transactional
	public AgencyResponseDTO create(AgencyRequestDTO request) {
		logger.info("Creating agency with name: {}", request.getName());

		if (agencyRepository.findByName(request.getName()).isPresent()) {
			logger.warn("Agency creation failed: name already exists - {}", request.getName());
			throw new BadRequestException("Agency with name " + request.getName() + " already exists");
		}

		Agency agency = Agency.builder()
				.name(request.getName())
				.legalName(request.getLegalName())
				.taxId(request.getTaxId())
				.addressLine1(request.getAddressLine1())
				.addressLine2(request.getAddressLine2())
				.city(request.getCity())
				.state(request.getState())
				.zipCode(request.getZipCode())
				.country(request.getCountry())
				.phone(request.getPhone())
				.email(request.getEmail())
				.website(request.getWebsite())
				.notes(request.getNotes())
				.isActive(request.getIsActive() != null ? request.getIsActive() : true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		agency = agencyRepository.save(agency);
		logger.info("Agency created successfully with ID: {}", agency.getId());

		return mapToResponseDTO(agency);
	}

	@Override
	public AgencyResponseDTO getById(UUID id) {
		logger.debug("Fetching agency with ID: {}", id);
		Agency agency = agencyRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Agency not found with ID: {}", id);
					return new NotFoundException("Agency not found with ID: " + id);
				});
		return mapToResponseDTO(agency);
	}

	@Override
	public List<AgencyResponseDTO> getAll() {
		logger.debug("Fetching all agencies");
		return agencyRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AgencyResponseDTO update(UUID id, AgencyRequestDTO request) {
		logger.info("Updating agency with ID: {}", id);
		Agency agency = agencyRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Agency not found with ID: {}", id);
					return new NotFoundException("Agency not found with ID: " + id);
				});

		if (!agency.getName().equals(request.getName()) &&
				agencyRepository.findByName(request.getName()).isPresent()) {
			logger.warn("Agency update failed: name already exists - {}", request.getName());
			throw new BadRequestException("Agency with name " + request.getName() + " already exists");
		}

		agency.setName(request.getName());
		agency.setLegalName(request.getLegalName());
		agency.setTaxId(request.getTaxId());
		agency.setAddressLine1(request.getAddressLine1());
		agency.setAddressLine2(request.getAddressLine2());
		agency.setCity(request.getCity());
		agency.setState(request.getState());
		agency.setZipCode(request.getZipCode());
		agency.setCountry(request.getCountry());
		agency.setPhone(request.getPhone());
		agency.setEmail(request.getEmail());
		agency.setWebsite(request.getWebsite());
		agency.setNotes(request.getNotes());
		if (request.getIsActive() != null) {
			agency.setIsActive(request.getIsActive());
		}
		agency.setUpdatedAt(LocalDateTime.now());

		agency = agencyRepository.save(agency);
		logger.info("Agency updated successfully with ID: {}", agency.getId());

		return mapToResponseDTO(agency);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting agency with ID: {}", id);
		if (!agencyRepository.existsById(id)) {
			logger.warn("Agency not found with ID: {}", id);
			throw new NotFoundException("Agency not found with ID: " + id);
		}
		agencyRepository.deleteById(id);
		logger.info("Agency deleted successfully with ID: {}", id);
	}

	private AgencyResponseDTO mapToResponseDTO(Agency agency) {
		return AgencyResponseDTO.builder()
				.id(agency.getId())
				.name(agency.getName())
				.legalName(agency.getLegalName())
				.taxId(agency.getTaxId())
				.addressLine1(agency.getAddressLine1())
				.addressLine2(agency.getAddressLine2())
				.city(agency.getCity())
				.state(agency.getState())
				.zipCode(agency.getZipCode())
				.country(agency.getCountry())
				.phone(agency.getPhone())
				.email(agency.getEmail())
				.website(agency.getWebsite())
				.notes(agency.getNotes())
				.isActive(agency.getIsActive())
				.createdAt(agency.getCreatedAt())
				.updatedAt(agency.getUpdatedAt())
				.build();
	}

}

