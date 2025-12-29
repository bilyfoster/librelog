package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.GridDaypartMappingRequestDTO;
import com.onelpro.librelog.dto.GridRequestDTO;
import com.onelpro.librelog.dto.GridResponseDTO;
import com.onelpro.librelog.exceptions.ConflictException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.Daypart;
import com.onelpro.librelog.models.Grid;
import com.onelpro.librelog.models.GridDaypartMapping;
import com.onelpro.librelog.repositories.ChannelRepository;
import com.onelpro.librelog.repositories.DaypartRepository;
import com.onelpro.librelog.repositories.GridDaypartMappingRepository;
import com.onelpro.librelog.repositories.GridRepository;
import com.onelpro.librelog.services.GridService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of grid service.
 */
@Service
public class GridServiceImpl implements GridService {

	private static final Logger logger = LoggerFactory.getLogger(GridServiceImpl.class);

	private static final String[] DAY_NAMES = {
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	};

	private final GridRepository gridRepository;
	private final ChannelRepository channelRepository;
	private final GridDaypartMappingRepository gridDaypartMappingRepository;
	private final DaypartRepository daypartRepository;

	public GridServiceImpl(
			GridRepository gridRepository,
			ChannelRepository channelRepository,
			GridDaypartMappingRepository gridDaypartMappingRepository,
			DaypartRepository daypartRepository) {
		this.gridRepository = gridRepository;
		this.channelRepository = channelRepository;
		this.gridDaypartMappingRepository = gridDaypartMappingRepository;
		this.daypartRepository = daypartRepository;
	}

	@Override
	@Transactional
	public GridResponseDTO create(GridRequestDTO request) {
		logger.info("Creating grid with name: {} for channel: {}", request.getName(), request.getChannelId());

		Channel channel = channelRepository.findById(request.getChannelId())
				.orElseThrow(() -> {
					logger.warn("Channel not found with ID: {}", request.getChannelId());
					return new NotFoundException("Channel not found with ID: " + request.getChannelId());
				});

		LocalDateTime now = LocalDateTime.now();
		Grid grid = Grid.builder()
				.name(request.getName())
				.channel(channel)
				.description(request.getDescription())
				.isActive(request.getIsActive())
				.createdAt(now)
				.updatedAt(now)
				.build();

		grid = gridRepository.save(grid);
		logger.info("Grid created successfully with ID: {}", grid.getId());

		return mapToResponseDTO(grid);
	}

	@Override
	public GridResponseDTO getById(UUID id) {
		logger.debug("Fetching grid with ID: {}", id);
		Grid grid = gridRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Grid not found with ID: {}", id);
					return new NotFoundException("Grid not found with ID: " + id);
				});
		return mapToResponseDTO(grid);
	}

	@Override
	public List<GridResponseDTO> getAll() {
		logger.debug("Fetching all grids");
		return gridRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<GridResponseDTO> getByChannelId(UUID channelId) {
		logger.debug("Fetching grids for channel: {}", channelId);
		return gridRepository.findByChannelId(channelId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public GridResponseDTO update(UUID id, GridRequestDTO request) {
		logger.info("Updating grid with ID: {}", id);
		Grid grid = gridRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Grid not found with ID: {}", id);
					return new NotFoundException("Grid not found with ID: " + id);
				});

		Channel channel = channelRepository.findById(request.getChannelId())
				.orElseThrow(() -> {
					logger.warn("Channel not found with ID: {}", request.getChannelId());
					return new NotFoundException("Channel not found with ID: " + request.getChannelId());
				});

		grid.setName(request.getName());
		grid.setChannel(channel);
		grid.setDescription(request.getDescription());
		grid.setIsActive(request.getIsActive());
		grid.setUpdatedAt(LocalDateTime.now());

		grid = gridRepository.save(grid);
		logger.info("Grid updated successfully with ID: {}", grid.getId());

		return mapToResponseDTO(grid);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting grid with ID: {}", id);
		if (!gridRepository.existsById(id)) {
			logger.warn("Grid not found with ID: {}", id);
			throw new NotFoundException("Grid not found with ID: " + id);
		}
		// Delete mappings first (cascade should handle this, but being explicit)
		gridDaypartMappingRepository.deleteByGridId(id);
		gridRepository.deleteById(id);
		logger.info("Grid deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public GridResponseDTO.GridDaypartMappingResponseDTO addDaypartMapping(GridDaypartMappingRequestDTO request) {
		logger.info("Adding daypart mapping: grid={}, daypart={}, dayOfWeek={}", 
				request.getGridId(), request.getDaypartId(), request.getDayOfWeek());

		Grid grid = gridRepository.findById(request.getGridId())
				.orElseThrow(() -> {
					logger.warn("Grid not found with ID: {}", request.getGridId());
					return new NotFoundException("Grid not found with ID: " + request.getGridId());
				});

		Daypart daypart = daypartRepository.findById(request.getDaypartId())
				.orElseThrow(() -> {
					logger.warn("Daypart not found with ID: {}", request.getDaypartId());
					return new NotFoundException("Daypart not found with ID: " + request.getDaypartId());
				});

		// Check for existing mapping (grid + daypart + dayOfWeek must be unique)
		if (gridDaypartMappingRepository.findByGridIdAndDayOfWeek(
				request.getGridId(), request.getDayOfWeek()).stream()
				.anyMatch(m -> m.getDaypart().getId().equals(request.getDaypartId()))) {
			logger.warn("Daypart mapping already exists: grid={}, daypart={}, dayOfWeek={}", 
					request.getGridId(), request.getDaypartId(), request.getDayOfWeek());
			throw new ConflictException("Daypart mapping already exists for this grid, daypart, and day of week");
		}

		// Convert dayOfWeek from 0-6 (Sunday-Saturday) to 1-7 (Monday-Sunday) for database
		// Database uses: 1=Monday, 2=Tuesday, ..., 7=Sunday
		Integer dbDayOfWeek = convertDayOfWeek(request.getDayOfWeek());

		LocalDateTime now = LocalDateTime.now();
		GridDaypartMapping mapping = GridDaypartMapping.builder()
				.grid(grid)
				.daypart(daypart)
				.dayOfWeek(dbDayOfWeek)
				.createdAt(now)
				.updatedAt(now)
				.build();

		mapping = gridDaypartMappingRepository.save(mapping);
		logger.info("Daypart mapping created successfully with ID: {}", mapping.getId());

		return mapMappingToResponseDTO(mapping);
	}

	@Override
	@Transactional
	public void removeDaypartMapping(UUID mappingId) {
		logger.info("Removing daypart mapping with ID: {}", mappingId);
		if (!gridDaypartMappingRepository.existsById(mappingId)) {
			logger.warn("Daypart mapping not found with ID: {}", mappingId);
			throw new NotFoundException("Daypart mapping not found with ID: " + mappingId);
		}
		gridDaypartMappingRepository.deleteById(mappingId);
		logger.info("Daypart mapping deleted successfully with ID: {}", mappingId);
	}

	@Override
	public List<GridResponseDTO.GridDaypartMappingResponseDTO> getDaypartMappingsByGridId(UUID gridId) {
		logger.debug("Fetching daypart mappings for grid: {}", gridId);
		return gridDaypartMappingRepository.findByGridId(gridId).stream()
				.map(this::mapMappingToResponseDTO)
				.collect(Collectors.toList());
	}

	private GridResponseDTO mapToResponseDTO(Grid grid) {
		Channel channel = grid.getChannel();
		List<GridResponseDTO.GridDaypartMappingResponseDTO> mappings = 
				gridDaypartMappingRepository.findByGridId(grid.getId()).stream()
						.map(this::mapMappingToResponseDTO)
						.collect(Collectors.toList());

		return GridResponseDTO.builder()
				.id(grid.getId())
				.name(grid.getName())
				.channelId(channel.getId())
				.channelName(channel.getName())
				.description(grid.getDescription())
				.isActive(grid.getIsActive())
				.daypartMappings(mappings)
				.createdAt(grid.getCreatedAt())
				.updatedAt(grid.getUpdatedAt())
				.build();
	}

	private GridResponseDTO.GridDaypartMappingResponseDTO mapMappingToResponseDTO(GridDaypartMapping mapping) {
		Daypart daypart = mapping.getDaypart();
		Integer dbDayOfWeek = mapping.getDayOfWeek();
		
		// Convert from database format (1-7, Monday-Sunday) to API format (0-6, Sunday-Saturday)
		Integer apiDayOfWeek = convertDayOfWeekFromDb(dbDayOfWeek);
		
		String timeRange = String.format("%s - %s", 
				daypart.getStartTime().toString().substring(0, 5),
				daypart.getEndTime().toString().substring(0, 5));

		return GridResponseDTO.GridDaypartMappingResponseDTO.builder()
				.id(mapping.getId())
				.daypartId(daypart.getId())
				.daypartName(daypart.getName())
				.daypartTimeRange(timeRange)
				.dayOfWeek(apiDayOfWeek)
				.dayOfWeekName(DAY_NAMES[apiDayOfWeek])
				.build();
	}

	/**
	 * Converts day of week from API format (0-6, Sunday-Saturday) to database format (1-7, Monday-Sunday).
	 */
	private Integer convertDayOfWeek(Integer apiDayOfWeek) {
		// API: 0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday
		// DB: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
		if (apiDayOfWeek == 0) {
			return 7; // Sunday
		}
		return apiDayOfWeek; // Monday-Saturday map directly
	}

	/**
	 * Converts day of week from database format (1-7, Monday-Sunday) to API format (0-6, Sunday-Saturday).
	 */
	private Integer convertDayOfWeekFromDb(Integer dbDayOfWeek) {
		// DB: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
		// API: 0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday
		if (dbDayOfWeek == 7) {
			return 0; // Sunday
		}
		return dbDayOfWeek; // Monday-Saturday map directly
	}

}

