package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.SalesRepRequestDTO;
import com.onelpro.librelog.dto.SalesRepResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.SalesRep;
import com.onelpro.librelog.models.SalesTeam;
import com.onelpro.librelog.models.SalesOffice;
import com.onelpro.librelog.models.SalesRegion;
import com.onelpro.librelog.repositories.SalesRepRepository;
import com.onelpro.librelog.repositories.SalesTeamRepository;
import com.onelpro.librelog.repositories.SalesOfficeRepository;
import com.onelpro.librelog.repositories.SalesRegionRepository;
import com.onelpro.librelog.services.SalesRepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of sales representative service.
 */
@Service
public class SalesRepServiceImpl implements SalesRepService {

	private static final Logger logger = LoggerFactory.getLogger(SalesRepServiceImpl.class);

	private final SalesRepRepository salesRepRepository;
	private final SalesTeamRepository salesTeamRepository;
	private final SalesOfficeRepository salesOfficeRepository;
	private final SalesRegionRepository salesRegionRepository;

	public SalesRepServiceImpl(
			SalesRepRepository salesRepRepository,
			SalesTeamRepository salesTeamRepository,
			SalesOfficeRepository salesOfficeRepository,
			SalesRegionRepository salesRegionRepository) {
		this.salesRepRepository = salesRepRepository;
		this.salesTeamRepository = salesTeamRepository;
		this.salesOfficeRepository = salesOfficeRepository;
		this.salesRegionRepository = salesRegionRepository;
	}

	@Override
	@Transactional
	public SalesRepResponseDTO create(SalesRepRequestDTO request) {
		logger.info("Creating sales rep with email: {}", request.getEmail());

		if (salesRepRepository.existsByEmail(request.getEmail())) {
			logger.warn("Sales rep creation failed: email already exists - {}", request.getEmail());
			throw new BadRequestException("Sales rep with email " + request.getEmail() + " already exists");
		}

		if (request.getSalesTeamId() != null && !salesTeamRepository.existsById(request.getSalesTeamId())) {
			logger.warn("Sales team not found with ID: {}", request.getSalesTeamId());
			throw new NotFoundException("Sales team not found with ID: " + request.getSalesTeamId());
		}

		if (request.getSalesOfficeId() != null && !salesOfficeRepository.existsById(request.getSalesOfficeId())) {
			logger.warn("Sales office not found with ID: {}", request.getSalesOfficeId());
			throw new NotFoundException("Sales office not found with ID: " + request.getSalesOfficeId());
		}

		if (request.getSalesRegionId() != null && !salesRegionRepository.existsById(request.getSalesRegionId())) {
			logger.warn("Sales region not found with ID: {}", request.getSalesRegionId());
			throw new NotFoundException("Sales region not found with ID: " + request.getSalesRegionId());
		}

		SalesRep salesRep = SalesRep.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.email(request.getEmail())
				.phone(request.getPhone())
				.employeeId(request.getEmployeeId())
				.salesTeamId(request.getSalesTeamId())
				.salesOfficeId(request.getSalesOfficeId())
				.salesRegionId(request.getSalesRegionId())
				.commissionRate(request.getCommissionRate())
				.isActive(request.getIsActive() != null ? request.getIsActive() : true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		salesRep = salesRepRepository.save(salesRep);
		logger.info("Sales rep created successfully with ID: {}", salesRep.getId());

		return mapToResponseDTO(salesRep);
	}

	@Override
	public SalesRepResponseDTO getById(UUID id) {
		logger.debug("Fetching sales rep with ID: {}", id);
		SalesRep salesRep = salesRepRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Sales rep not found with ID: {}", id);
					return new NotFoundException("Sales rep not found with ID: " + id);
				});
		return mapToResponseDTO(salesRep);
	}

	@Override
	public List<SalesRepResponseDTO> getAll() {
		logger.debug("Fetching all sales reps");
		return salesRepRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<SalesRepResponseDTO> getBySalesTeamId(UUID salesTeamId) {
		logger.debug("Fetching sales reps for sales team: {}", salesTeamId);
		return salesRepRepository.findBySalesTeamId(salesTeamId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SalesRepResponseDTO update(UUID id, SalesRepRequestDTO request) {
		logger.info("Updating sales rep with ID: {}", id);
		SalesRep salesRep = salesRepRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Sales rep not found with ID: {}", id);
					return new NotFoundException("Sales rep not found with ID: " + id);
				});

		if (!salesRep.getEmail().equals(request.getEmail()) &&
				salesRepRepository.existsByEmail(request.getEmail())) {
			logger.warn("Sales rep update failed: email already exists - {}", request.getEmail());
			throw new BadRequestException("Sales rep with email " + request.getEmail() + " already exists");
		}

		if (request.getSalesTeamId() != null && !salesTeamRepository.existsById(request.getSalesTeamId())) {
			logger.warn("Sales team not found with ID: {}", request.getSalesTeamId());
			throw new NotFoundException("Sales team not found with ID: " + request.getSalesTeamId());
		}

		if (request.getSalesOfficeId() != null && !salesOfficeRepository.existsById(request.getSalesOfficeId())) {
			logger.warn("Sales office not found with ID: {}", request.getSalesOfficeId());
			throw new NotFoundException("Sales office not found with ID: " + request.getSalesOfficeId());
		}

		if (request.getSalesRegionId() != null && !salesRegionRepository.existsById(request.getSalesRegionId())) {
			logger.warn("Sales region not found with ID: {}", request.getSalesRegionId());
			throw new NotFoundException("Sales region not found with ID: " + request.getSalesRegionId());
		}

		salesRep.setFirstName(request.getFirstName());
		salesRep.setLastName(request.getLastName());
		salesRep.setEmail(request.getEmail());
		salesRep.setPhone(request.getPhone());
		salesRep.setEmployeeId(request.getEmployeeId());
		salesRep.setSalesTeamId(request.getSalesTeamId());
		salesRep.setSalesOfficeId(request.getSalesOfficeId());
		salesRep.setSalesRegionId(request.getSalesRegionId());
		salesRep.setCommissionRate(request.getCommissionRate());
		if (request.getIsActive() != null) {
			salesRep.setIsActive(request.getIsActive());
		}
		salesRep.setUpdatedAt(LocalDateTime.now());

		salesRep = salesRepRepository.save(salesRep);
		logger.info("Sales rep updated successfully with ID: {}", salesRep.getId());

		return mapToResponseDTO(salesRep);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting sales rep with ID: {}", id);
		if (!salesRepRepository.existsById(id)) {
			logger.warn("Sales rep not found with ID: {}", id);
			throw new NotFoundException("Sales rep not found with ID: " + id);
		}
		salesRepRepository.deleteById(id);
		logger.info("Sales rep deleted successfully with ID: {}", id);
	}

	private SalesRepResponseDTO mapToResponseDTO(SalesRep salesRep) {
		String salesTeamName = null;
		if (salesRep.getSalesTeamId() != null) {
			salesTeamName = salesTeamRepository.findById(salesRep.getSalesTeamId())
					.map(SalesTeam::getName)
					.orElse(null);
		}

		String salesOfficeName = null;
		if (salesRep.getSalesOfficeId() != null) {
			salesOfficeName = salesOfficeRepository.findById(salesRep.getSalesOfficeId())
					.map(SalesOffice::getName)
					.orElse(null);
		}

		String salesRegionName = null;
		if (salesRep.getSalesRegionId() != null) {
			salesRegionName = salesRegionRepository.findById(salesRep.getSalesRegionId())
					.map(SalesRegion::getName)
					.orElse(null);
		}

		return SalesRepResponseDTO.builder()
				.id(salesRep.getId())
				.firstName(salesRep.getFirstName())
				.lastName(salesRep.getLastName())
				.email(salesRep.getEmail())
				.phone(salesRep.getPhone())
				.employeeId(salesRep.getEmployeeId())
				.salesTeamId(salesRep.getSalesTeamId())
				.salesTeamName(salesTeamName)
				.salesOfficeId(salesRep.getSalesOfficeId())
				.salesOfficeName(salesOfficeName)
				.salesRegionId(salesRep.getSalesRegionId())
				.salesRegionName(salesRegionName)
				.commissionRate(salesRep.getCommissionRate())
				.isActive(salesRep.getIsActive())
				.createdAt(salesRep.getCreatedAt())
				.updatedAt(salesRep.getUpdatedAt())
				.build();
	}

}

