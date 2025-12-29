package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.DaypartAssignmentRequestDTO;
import com.onelpro.librelog.dto.DaypartAssignmentResponseDTO;
import com.onelpro.librelog.exceptions.ConflictException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.Daypart;
import com.onelpro.librelog.models.DaypartAssignment;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.repositories.DaypartAssignmentRepository;
import com.onelpro.librelog.repositories.DaypartRepository;
import com.onelpro.librelog.services.DaypartAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of daypart assignment service.
 */
@Service
public class DaypartAssignmentServiceImpl implements DaypartAssignmentService {

	private static final Logger logger = LoggerFactory.getLogger(DaypartAssignmentServiceImpl.class);

	private final DaypartAssignmentRepository daypartAssignmentRepository;
	private final DaypartRepository daypartRepository;
	private final ClockTemplateRepository clockTemplateRepository;

	public DaypartAssignmentServiceImpl(
			DaypartAssignmentRepository daypartAssignmentRepository,
			DaypartRepository daypartRepository,
			ClockTemplateRepository clockTemplateRepository) {
		this.daypartAssignmentRepository = daypartAssignmentRepository;
		this.daypartRepository = daypartRepository;
		this.clockTemplateRepository = clockTemplateRepository;
	}

	@Override
	@Transactional
	public DaypartAssignmentResponseDTO create(DaypartAssignmentRequestDTO request) {
		logger.info("Creating daypart assignment: daypart={}, clockTemplate={}", 
				request.getDaypartId(), request.getClockTemplateId());

		// Validate daypart exists
		Daypart daypart = daypartRepository.findById(request.getDaypartId())
				.orElseThrow(() -> {
					logger.warn("Daypart not found with ID: {}", request.getDaypartId());
					return new NotFoundException("Daypart not found with ID: " + request.getDaypartId());
				});

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
					return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
				});

		// Check for existing assignment
		if (daypartAssignmentRepository.findByDaypartIdAndClockTemplateId(
				request.getDaypartId(), request.getClockTemplateId()).isPresent()) {
			logger.warn("Daypart assignment already exists: daypart={}, clockTemplate={}", 
					request.getDaypartId(), request.getClockTemplateId());
			throw new ConflictException("Daypart assignment already exists for this daypart and clock template");
		}

		LocalDateTime now = LocalDateTime.now();
		DaypartAssignment assignment = DaypartAssignment.builder()
				.daypart(daypart)
				.clockTemplate(clockTemplate)
				.createdAt(now)
				.updatedAt(now)
				.build();

		assignment = daypartAssignmentRepository.save(assignment);
		logger.info("Daypart assignment created successfully with ID: {}", assignment.getId());

		return mapToResponseDTO(assignment);
	}

	@Override
	public DaypartAssignmentResponseDTO getById(UUID id) {
		logger.debug("Fetching daypart assignment with ID: {}", id);
		DaypartAssignment assignment = daypartAssignmentRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Daypart assignment not found with ID: {}", id);
					return new NotFoundException("Daypart assignment not found with ID: " + id);
				});
		return mapToResponseDTO(assignment);
	}

	@Override
	public List<DaypartAssignmentResponseDTO> getByDaypartId(UUID daypartId) {
		logger.debug("Fetching daypart assignments for daypart: {}", daypartId);
		return daypartAssignmentRepository.findByDaypartId(daypartId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<DaypartAssignmentResponseDTO> getByClockTemplateId(UUID clockTemplateId) {
		logger.debug("Fetching daypart assignments for clock template: {}", clockTemplateId);
		return daypartAssignmentRepository.findByClockTemplateId(clockTemplateId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public DaypartAssignmentResponseDTO update(UUID id, DaypartAssignmentRequestDTO request) {
		logger.info("Updating daypart assignment with ID: {}", id);
		DaypartAssignment assignment = daypartAssignmentRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Daypart assignment not found with ID: {}", id);
					return new NotFoundException("Daypart assignment not found with ID: " + id);
				});

		// Validate daypart exists
		Daypart daypart = daypartRepository.findById(request.getDaypartId())
				.orElseThrow(() -> {
					logger.warn("Daypart not found with ID: {}", request.getDaypartId());
					return new NotFoundException("Daypart not found with ID: " + request.getDaypartId());
				});

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
					return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
				});

		// Check for existing assignment (excluding current one)
		daypartAssignmentRepository.findByDaypartIdAndClockTemplateId(
				request.getDaypartId(), request.getClockTemplateId())
				.ifPresent(existing -> {
					if (!existing.getId().equals(id)) {
						logger.warn("Daypart assignment already exists: daypart={}, clockTemplate={}", 
								request.getDaypartId(), request.getClockTemplateId());
						throw new ConflictException("Daypart assignment already exists for this daypart and clock template");
					}
				});

		assignment.setDaypart(daypart);
		assignment.setClockTemplate(clockTemplate);
		assignment.setUpdatedAt(LocalDateTime.now());

		assignment = daypartAssignmentRepository.save(assignment);
		logger.info("Daypart assignment updated successfully with ID: {}", assignment.getId());

		return mapToResponseDTO(assignment);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting daypart assignment with ID: {}", id);
		if (!daypartAssignmentRepository.existsById(id)) {
			logger.warn("Daypart assignment not found with ID: {}", id);
			throw new NotFoundException("Daypart assignment not found with ID: " + id);
		}
		daypartAssignmentRepository.deleteById(id);
		logger.info("Daypart assignment deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public void deleteByDaypartId(UUID daypartId) {
		logger.info("Deleting all daypart assignments for daypart: {}", daypartId);
		daypartAssignmentRepository.deleteByDaypartId(daypartId);
		logger.info("Daypart assignments deleted successfully for daypart: {}", daypartId);
	}

	@Override
	@Transactional
	public void deleteByClockTemplateId(UUID clockTemplateId) {
		logger.info("Deleting all daypart assignments for clock template: {}", clockTemplateId);
		daypartAssignmentRepository.deleteByClockTemplateId(clockTemplateId);
		logger.info("Daypart assignments deleted successfully for clock template: {}", clockTemplateId);
	}

	private DaypartAssignmentResponseDTO mapToResponseDTO(DaypartAssignment assignment) {
		Daypart daypart = assignment.getDaypart();
		ClockTemplate clockTemplate = assignment.getClockTemplate();
		
		String timeRange = String.format("%s - %s", 
				daypart.getStartTime().toString().substring(0, 5),
				daypart.getEndTime().toString().substring(0, 5));

		return DaypartAssignmentResponseDTO.builder()
				.id(assignment.getId())
				.daypartId(daypart.getId())
				.daypartName(daypart.getName())
				.daypartTimeRange(timeRange)
				.clockTemplateId(clockTemplate.getId())
				.clockTemplateName(clockTemplate.getName())
				.createdAt(assignment.getCreatedAt())
				.updatedAt(assignment.getUpdatedAt())
				.build();
	}

}

