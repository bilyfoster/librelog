package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.BreakStructureRequestDTO;
import com.onelpro.librelog.dto.BreakStructureResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AvailType;
import com.onelpro.librelog.models.BreakStructure;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.AvailTypeRepository;
import com.onelpro.librelog.repositories.BreakStructureRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.services.BreakStructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of break structure service.
 */
@Service
public class BreakStructureServiceImpl implements BreakStructureService {

	private static final Logger logger = LoggerFactory.getLogger(BreakStructureServiceImpl.class);

	private final BreakStructureRepository breakStructureRepository;
	private final ClockTemplateRepository clockTemplateRepository;
	private final AvailTypeRepository availTypeRepository;

	public BreakStructureServiceImpl(
			BreakStructureRepository breakStructureRepository,
			ClockTemplateRepository clockTemplateRepository,
			AvailTypeRepository availTypeRepository) {
		this.breakStructureRepository = breakStructureRepository;
		this.clockTemplateRepository = clockTemplateRepository;
		this.availTypeRepository = availTypeRepository;
	}

	@Override
	@Transactional
	public BreakStructureResponseDTO create(BreakStructureRequestDTO request) {
		logger.info("Creating break structure with name: {} for clock template: {}", request.getName(), request.getClockTemplateId());

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
					return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
				});

		// Validate avail type if provided
		AvailType availType = null;
		if (request.getAvailTypeId() != null) {
			availType = availTypeRepository.findById(request.getAvailTypeId())
					.orElseThrow(() -> {
						logger.warn("Avail type not found with ID: {}", request.getAvailTypeId());
						return new NotFoundException("Avail type not found with ID: " + request.getAvailTypeId());
					});
		}

		// Validate timing constraints (start time + duration should not exceed 60 minutes)
		// For clock templates, we work within a 60-minute window (0:00 to 59:59)
		// Calculate total seconds from the start of the hour
		int startMinutes = request.getStartTime().getMinute();
		int startSeconds = request.getStartTime().getSecond();
		int totalSeconds = (startMinutes * 60) + startSeconds + request.getDurationSeconds();
		if (totalSeconds > 3600) {
			logger.warn("Break structure exceeds 60-minute clock limit");
			throw new BadRequestException("Break structure start time plus duration cannot exceed 60 minutes");
		}

		BreakStructure breakStructure = BreakStructure.builder()
				.clockTemplate(clockTemplate)
				.name(request.getName())
				.startTime(request.getStartTime())
				.durationSeconds(request.getDurationSeconds())
				.isFloating(request.getIsFloating() != null ? request.getIsFloating() : false)
				.availType(availType)
				.timingType(request.getTimingType())
				.transitionCode(request.getTransitionCode())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		breakStructure = breakStructureRepository.save(breakStructure);
		logger.info("Break structure created successfully with ID: {}", breakStructure.getId());

		return mapToResponseDTO(breakStructure);
	}

	@Override
	public BreakStructureResponseDTO getById(UUID id) {
		logger.debug("Fetching break structure with ID: {}", id);
		BreakStructure breakStructure = breakStructureRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Break structure not found with ID: {}", id);
					return new NotFoundException("Break structure not found with ID: " + id);
				});
		return mapToResponseDTO(breakStructure);
	}

	@Override
	public List<BreakStructureResponseDTO> getByClockTemplateId(UUID clockTemplateId) {
		logger.debug("Fetching break structures for clock template: {}", clockTemplateId);
		
		// Validate clock template exists
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			logger.warn("Clock template not found with ID: {}", clockTemplateId);
			throw new NotFoundException("Clock template not found with ID: " + clockTemplateId);
		}

		return breakStructureRepository.findByClockTemplateId(clockTemplateId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public BreakStructureResponseDTO update(UUID id, BreakStructureRequestDTO request) {
		logger.info("Updating break structure with ID: {}", id);
		BreakStructure breakStructure = breakStructureRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Break structure not found with ID: {}", id);
					return new NotFoundException("Break structure not found with ID: " + id);
				});

		// Validate clock template exists if changed
		if (!breakStructure.getClockTemplate().getId().equals(request.getClockTemplateId())) {
			ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
					.orElseThrow(() -> {
						logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
						return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
					});
			breakStructure.setClockTemplate(clockTemplate);
		}

		// Validate avail type if provided
		AvailType availType = null;
		if (request.getAvailTypeId() != null) {
			availType = availTypeRepository.findById(request.getAvailTypeId())
					.orElseThrow(() -> {
						logger.warn("Avail type not found with ID: {}", request.getAvailTypeId());
						return new NotFoundException("Avail type not found with ID: " + request.getAvailTypeId());
					});
		}

		// Validate timing constraints
		int startMinutes = request.getStartTime().getMinute();
		int startSeconds = request.getStartTime().getSecond();
		int totalSeconds = (startMinutes * 60) + startSeconds + request.getDurationSeconds();
		if (totalSeconds > 3600) {
			logger.warn("Break structure exceeds 60-minute clock limit");
			throw new BadRequestException("Break structure start time plus duration cannot exceed 60 minutes");
		}

		breakStructure.setName(request.getName());
		breakStructure.setStartTime(request.getStartTime());
		breakStructure.setDurationSeconds(request.getDurationSeconds());
		breakStructure.setIsFloating(request.getIsFloating() != null ? request.getIsFloating() : false);
		breakStructure.setAvailType(availType);
		breakStructure.setTimingType(request.getTimingType());
		breakStructure.setTransitionCode(request.getTransitionCode());
		breakStructure.setUpdatedAt(LocalDateTime.now());

		breakStructure = breakStructureRepository.save(breakStructure);
		logger.info("Break structure updated successfully with ID: {}", breakStructure.getId());

		return mapToResponseDTO(breakStructure);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting break structure with ID: {}", id);
		if (!breakStructureRepository.existsById(id)) {
			logger.warn("Break structure not found with ID: {}", id);
			throw new NotFoundException("Break structure not found with ID: " + id);
		}
		breakStructureRepository.deleteById(id);
		logger.info("Break structure deleted successfully with ID: {}", id);
	}

	private BreakStructureResponseDTO mapToResponseDTO(BreakStructure breakStructure) {
		ClockTemplate clockTemplate = breakStructure.getClockTemplate();
		AvailType availType = breakStructure.getAvailType();

		return BreakStructureResponseDTO.builder()
				.id(breakStructure.getId())
				.clockTemplateId(clockTemplate.getId())
				.clockTemplateName(clockTemplate.getName())
				.name(breakStructure.getName())
				.startTime(breakStructure.getStartTime())
				.durationSeconds(breakStructure.getDurationSeconds())
				.isFloating(breakStructure.getIsFloating())
				.availTypeId(availType != null ? availType.getId() : null)
				.availTypeName(availType != null ? availType.getName() : null)
				.timingType(breakStructure.getTimingType())
				.transitionCode(breakStructure.getTransitionCode())
				.createdAt(breakStructure.getCreatedAt())
				.updatedAt(breakStructure.getUpdatedAt())
				.build();
	}

}

