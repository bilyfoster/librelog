package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.DaypartRequestDTO;
import com.onelpro.librelog.dto.DaypartResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Daypart;
import com.onelpro.librelog.models.DaypartCategory;
import com.onelpro.librelog.repositories.DaypartCategoryRepository;
import com.onelpro.librelog.repositories.DaypartRepository;
import com.onelpro.librelog.services.DaypartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of daypart service.
 */
@Service
public class DaypartServiceImpl implements DaypartService {

	private static final Logger logger = LoggerFactory.getLogger(DaypartServiceImpl.class);

	private final DaypartRepository daypartRepository;
	private final DaypartCategoryRepository daypartCategoryRepository;

	public DaypartServiceImpl(
			DaypartRepository daypartRepository,
			DaypartCategoryRepository daypartCategoryRepository) {
		this.daypartRepository = daypartRepository;
		this.daypartCategoryRepository = daypartCategoryRepository;
	}

	@Override
	@Transactional
	public DaypartResponseDTO create(DaypartRequestDTO request) {
		logger.info("Creating daypart with name: {}", request.getName());

		// Allow end time to be 00:00 (midnight) when start time is later in the day (wraps to next day)
		// e.g., 19:00 to 00:00 is valid (evening to midnight)
		// This handles dayparts that span midnight (e.g., evening drive 3PM-7PM, evening 7PM-midnight)
		boolean isValidTimeRange;
		if (request.getEndTime().equals(java.time.LocalTime.MIDNIGHT)) {
			// End time is midnight (00:00) - this is valid for any start time
			// It represents a daypart that ends at midnight (wraps to next day)
			isValidTimeRange = true;
		} else {
			// Normal case: end time must be after start time
			isValidTimeRange = request.getStartTime().isBefore(request.getEndTime());
		}
		
		if (!isValidTimeRange) {
			logger.warn("Daypart creation failed: invalid time range");
			throw new BadRequestException("Start time must be before end time, or end time can be 00:00 (midnight)");
		}

		DaypartCategory category = null;
		if (request.getCategoryId() != null) {
			category = daypartCategoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> {
						logger.warn("Daypart category not found with ID: {}", request.getCategoryId());
						return new NotFoundException("Daypart category not found with ID: " + request.getCategoryId());
					});
		}

		Daypart daypart = Daypart.builder()
				.name(request.getName())
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.category(category)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		daypart = daypartRepository.save(daypart);
		logger.info("Daypart created successfully with ID: {}", daypart.getId());

		return mapToResponseDTO(daypart);
	}

	@Override
	public DaypartResponseDTO getById(UUID id) {
		logger.debug("Fetching daypart with ID: {}", id);
		Daypart daypart = daypartRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Daypart not found with ID: {}", id);
					return new NotFoundException("Daypart not found with ID: " + id);
				});
		return mapToResponseDTO(daypart);
	}

	@Override
	public List<DaypartResponseDTO> getAll() {
		logger.debug("Fetching all dayparts");
		return daypartRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public DaypartResponseDTO update(UUID id, DaypartRequestDTO request) {
		logger.info("Updating daypart with ID: {}", id);
		Daypart daypart = daypartRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Daypart not found with ID: {}", id);
					return new NotFoundException("Daypart not found with ID: " + id);
				});

		// Allow end time to be 00:00 (midnight) when start time is later in the day (wraps to next day)
		// e.g., 19:00 to 00:00 is valid (evening to midnight)
		// This handles dayparts that span midnight (e.g., evening drive 3PM-7PM, evening 7PM-midnight)
		boolean isValidTimeRange;
		if (request.getEndTime().equals(java.time.LocalTime.MIDNIGHT)) {
			// End time is midnight (00:00) - this is valid for any start time
			// It represents a daypart that ends at midnight (wraps to next day)
			isValidTimeRange = true;
		} else {
			// Normal case: end time must be after start time
			isValidTimeRange = request.getStartTime().isBefore(request.getEndTime());
		}
		
		if (!isValidTimeRange) {
			logger.warn("Daypart update failed: invalid time range");
			throw new BadRequestException("Start time must be before end time, or end time can be 00:00 (midnight)");
		}

		DaypartCategory category = null;
		if (request.getCategoryId() != null) {
			category = daypartCategoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> {
						logger.warn("Daypart category not found with ID: {}", request.getCategoryId());
						return new NotFoundException("Daypart category not found with ID: " + request.getCategoryId());
					});
		}

		daypart.setName(request.getName());
		daypart.setStartTime(request.getStartTime());
		daypart.setEndTime(request.getEndTime());
		daypart.setCategory(category);
		daypart.setUpdatedAt(LocalDateTime.now());

		daypart = daypartRepository.save(daypart);
		logger.info("Daypart updated successfully with ID: {}", daypart.getId());

		return mapToResponseDTO(daypart);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting daypart with ID: {}", id);
		if (!daypartRepository.existsById(id)) {
			logger.warn("Daypart not found with ID: {}", id);
			throw new NotFoundException("Daypart not found with ID: " + id);
		}
		daypartRepository.deleteById(id);
		logger.info("Daypart deleted successfully with ID: {}", id);
	}

	private DaypartResponseDTO mapToResponseDTO(Daypart daypart) {
		return DaypartResponseDTO.builder()
				.id(daypart.getId())
				.name(daypart.getName())
				.startTime(daypart.getStartTime())
				.endTime(daypart.getEndTime())
				.categoryId(daypart.getCategory() != null ? daypart.getCategory().getId() : null)
				.categoryName(daypart.getCategory() != null ? daypart.getCategory().getName() : null)
				.createdAt(daypart.getCreatedAt())
				.updatedAt(daypart.getUpdatedAt())
				.build();
	}

}

