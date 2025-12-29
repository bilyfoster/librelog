package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AvailTypeRequestDTO;
import com.onelpro.librelog.dto.AvailTypeResponseDTO;
import com.onelpro.librelog.exceptions.ConflictException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AvailType;
import com.onelpro.librelog.repositories.AvailTypeRepository;
import com.onelpro.librelog.services.AvailTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of avail type service.
 */
@Service
public class AvailTypeServiceImpl implements AvailTypeService {

	private static final Logger logger = LoggerFactory.getLogger(AvailTypeServiceImpl.class);

	private final AvailTypeRepository availTypeRepository;

	public AvailTypeServiceImpl(AvailTypeRepository availTypeRepository) {
		this.availTypeRepository = availTypeRepository;
	}

	@Override
	@Transactional
	public AvailTypeResponseDTO create(AvailTypeRequestDTO request) {
		logger.info("Creating avail type with name: {}", request.getName());

		// Check if name already exists
		if (availTypeRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
			logger.warn("Avail type with name already exists: {}", request.getName());
			throw new ConflictException("Avail type with name already exists: " + request.getName());
		}

		AvailType availType = AvailType.builder()
				.name(request.getName())
				.description(request.getDescription())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		availType = availTypeRepository.save(availType);
		logger.info("Avail type created successfully with ID: {}", availType.getId());

		return mapToResponseDTO(availType);
	}

	@Override
	public AvailTypeResponseDTO getById(UUID id) {
		logger.debug("Fetching avail type with ID: {}", id);
		AvailType availType = availTypeRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Avail type not found with ID: {}", id);
					return new NotFoundException("Avail type not found with ID: " + id);
				});
		return mapToResponseDTO(availType);
	}

	@Override
	public List<AvailTypeResponseDTO> getAll() {
		logger.debug("Fetching all avail types");
		return availTypeRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<AvailTypeResponseDTO> getActive() {
		logger.debug("Fetching all active avail types");
		return availTypeRepository.findByIsActiveTrue().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AvailTypeResponseDTO update(UUID id, AvailTypeRequestDTO request) {
		logger.info("Updating avail type with ID: {}", id);
		AvailType availType = availTypeRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Avail type not found with ID: {}", id);
					return new NotFoundException("Avail type not found with ID: " + id);
				});

		// Check if name already exists (excluding current record)
		availTypeRepository.findByNameIgnoreCase(request.getName())
				.ifPresent(existing -> {
					if (!existing.getId().equals(id)) {
						logger.warn("Avail type with name already exists: {}", request.getName());
						throw new ConflictException("Avail type with name already exists: " + request.getName());
					}
				});

		availType.setName(request.getName());
		availType.setDescription(request.getDescription());
		availType.setIsActive(request.getIsActive());
		availType.setUpdatedAt(LocalDateTime.now());

		availType = availTypeRepository.save(availType);
		logger.info("Avail type updated successfully with ID: {}", availType.getId());

		return mapToResponseDTO(availType);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting avail type with ID: {}", id);
		if (!availTypeRepository.existsById(id)) {
			logger.warn("Avail type not found with ID: {}", id);
			throw new NotFoundException("Avail type not found with ID: " + id);
		}
		availTypeRepository.deleteById(id);
		logger.info("Avail type deleted successfully with ID: {}", id);
	}

	private AvailTypeResponseDTO mapToResponseDTO(AvailType availType) {
		return AvailTypeResponseDTO.builder()
				.id(availType.getId())
				.name(availType.getName())
				.description(availType.getDescription())
				.isActive(availType.getIsActive())
				.createdAt(availType.getCreatedAt())
				.updatedAt(availType.getUpdatedAt())
				.build();
	}

}

