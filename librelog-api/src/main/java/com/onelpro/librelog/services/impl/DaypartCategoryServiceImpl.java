package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.DaypartCategoryRequestDTO;
import com.onelpro.librelog.dto.DaypartCategoryResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.DaypartCategory;
import com.onelpro.librelog.repositories.DaypartCategoryRepository;
import com.onelpro.librelog.services.DaypartCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of daypart category service.
 */
@Service
public class DaypartCategoryServiceImpl implements DaypartCategoryService {

	private static final Logger logger = LoggerFactory.getLogger(DaypartCategoryServiceImpl.class);

	private final DaypartCategoryRepository daypartCategoryRepository;

	public DaypartCategoryServiceImpl(DaypartCategoryRepository daypartCategoryRepository) {
		this.daypartCategoryRepository = daypartCategoryRepository;
	}

	@Override
	@Transactional
	public DaypartCategoryResponseDTO create(DaypartCategoryRequestDTO request) {
		logger.info("Creating daypart category with name: {}", request.getName());

		if (daypartCategoryRepository.findByName(request.getName()).isPresent()) {
			logger.warn("Daypart category creation failed: name already exists - {}", request.getName());
			throw new BadRequestException("Daypart category with name " + request.getName() + " already exists");
		}

		DaypartCategory category = DaypartCategory.builder()
				.name(request.getName())
				.description(request.getDescription())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		category = daypartCategoryRepository.save(category);
		logger.info("Daypart category created successfully with ID: {}", category.getId());

		return mapToResponseDTO(category);
	}

	@Override
	public DaypartCategoryResponseDTO getById(UUID id) {
		logger.debug("Fetching daypart category with ID: {}", id);
		DaypartCategory category = daypartCategoryRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Daypart category not found with ID: {}", id);
					return new NotFoundException("Daypart category not found with ID: " + id);
				});
		return mapToResponseDTO(category);
	}

	@Override
	public List<DaypartCategoryResponseDTO> getAll() {
		logger.debug("Fetching all daypart categories");
		return daypartCategoryRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public DaypartCategoryResponseDTO update(UUID id, DaypartCategoryRequestDTO request) {
		logger.info("Updating daypart category with ID: {}", id);
		DaypartCategory category = daypartCategoryRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Daypart category not found with ID: {}", id);
					return new NotFoundException("Daypart category not found with ID: " + id);
				});

		if (!category.getName().equals(request.getName()) &&
				daypartCategoryRepository.findByName(request.getName()).isPresent()) {
			logger.warn("Daypart category update failed: name already exists - {}", request.getName());
			throw new BadRequestException("Daypart category with name " + request.getName() + " already exists");
		}

		category.setName(request.getName());
		category.setDescription(request.getDescription());
		category.setUpdatedAt(LocalDateTime.now());

		category = daypartCategoryRepository.save(category);
		logger.info("Daypart category updated successfully with ID: {}", category.getId());

		return mapToResponseDTO(category);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting daypart category with ID: {}", id);
		if (!daypartCategoryRepository.existsById(id)) {
			logger.warn("Daypart category not found with ID: {}", id);
			throw new NotFoundException("Daypart category not found with ID: " + id);
		}
		daypartCategoryRepository.deleteById(id);
		logger.info("Daypart category deleted successfully with ID: {}", id);
	}

	private DaypartCategoryResponseDTO mapToResponseDTO(DaypartCategory category) {
		return DaypartCategoryResponseDTO.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.createdAt(category.getCreatedAt())
				.updatedAt(category.getUpdatedAt())
				.build();
	}
}

