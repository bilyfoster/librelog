package com.onelpro.librelog.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.integrations.LibreTimeClient;
import com.onelpro.librelog.services.ClockBuilderService;
import com.onelpro.librelog.services.LibreTimeSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of LibreTime synchronization service.
 * Converts internal clock template format to LibreTime format and handles API communication.
 */
@Service
public class LibreTimeSyncServiceImpl implements LibreTimeSyncService {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeSyncServiceImpl.class);
	private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final ClockBuilderService clockBuilderService;
	private final LibreTimeClient libreTimeClient;
	private final ObjectMapper objectMapper;

	public LibreTimeSyncServiceImpl(
			ClockBuilderService clockBuilderService,
			LibreTimeClient libreTimeClient,
			ObjectMapper objectMapper) {
		this.clockBuilderService = clockBuilderService;
		this.libreTimeClient = libreTimeClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public LibreTimeExportDTO exportClock(UUID clockTemplateId) {
		logger.info("Exporting clock template {} to LibreTime format", clockTemplateId);

		ClockTemplateWithBreaksDTO clock = clockBuilderService.getClockStructure(clockTemplateId);

		// Create show instance for the clock (60-minute show)
		LibreTimeExportDTO.LibreTimeShowInstance showInstance = LibreTimeExportDTO.LibreTimeShowInstance.builder()
				.showName(clock.getName())
				.startTime(LocalDateTime.now().format(ISO_DATE_TIME))
				.endTime(LocalDateTime.now().plusHours(1).format(ISO_DATE_TIME))
				.items(new ArrayList<>())
				.build();

		// Convert breaks to LibreTime items
		if (clock.getBreaks() != null) {
			for (BreakStructureResponseDTO breakItem : clock.getBreaks()) {
				LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
						.type("break")
						.name(breakItem.getName())
						.startTime(breakItem.getStartTime().format(TIME_FORMAT))
						.durationSeconds(breakItem.getDurationSeconds())
						.transition(breakItem.getTransitionCode() != null ? 
								breakItem.getTransitionCode().name() : "SEGUE")
						.build();
				showInstance.getItems().add(item);
			}
		}

		// Convert fixed assets to LibreTime items
		if (clock.getFixedAssets() != null) {
			for (FixedAssetResponseDTO asset : clock.getFixedAssets()) {
				LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
						.type("fixed_asset")
						.name(asset.getName())
						.startTime(asset.getStartTime().format(TIME_FORMAT))
						.assetType(asset.getAssetType() != null ? asset.getAssetType().name() : null)
						.transition(asset.getTimingType() != null ? 
								asset.getTimingType().name() : "HARD_START")
						.build();
				showInstance.getItems().add(item);
			}
		}

		// Convert automation commands to LibreTime items
		if (clock.getAutomationCommands() != null) {
			for (AutomationCommandResponseDTO command : clock.getAutomationCommands()) {
				LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
						.type("command")
						.name(command.getCommandType().name())
						.startTime(command.getTriggerTime().format(TIME_FORMAT))
						.commandType(command.getCommandType().name())
						.build();
				showInstance.getItems().add(item);
			}
		}

		// Sort items by start time
		showInstance.getItems().sort(Comparator.comparing(LibreTimeExportDTO.LibreTimeItem::getStartTime));

		LibreTimeExportDTO export = LibreTimeExportDTO.builder()
				.name(clock.getName())
				.description(clock.getDescription())
				.showInstances(List.of(showInstance))
				.build();

		logger.info("Clock template {} exported successfully to LibreTime format", clockTemplateId);
		return export;
	}

	@Override
	public String pushClockToLibreTime(UUID clockTemplateId) {
		logger.info("Pushing clock template {} to LibreTime API", clockTemplateId);

		LibreTimeExportDTO export = exportClock(clockTemplateId);

		try {
			String json = objectMapper.writeValueAsString(export);
			return libreTimeClient.exportClock(json).block();
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize clock export to JSON: {}", e.getMessage());
			throw new RuntimeException("Failed to serialize clock export", e);
		} catch (Exception e) {
			logger.error("Failed to push clock to LibreTime: {}", e.getMessage());
			throw new RuntimeException("Failed to push clock to LibreTime", e);
		}
	}

	@Override
	public LibreTimeExportDTO generateLogFromClock(UUID clockTemplateId, LocalDate date) {
		logger.info("Generating log from clock template {} for date {}", clockTemplateId, date);

		ClockTemplateWithBreaksDTO clock = clockBuilderService.getClockStructure(clockTemplateId);

		// Generate show instances for each hour of the day (24 hours)
		List<LibreTimeExportDTO.LibreTimeShowInstance> showInstances = new ArrayList<>();

		for (int hour = 0; hour < 24; hour++) {
			LocalDateTime startDateTime = date.atTime(hour, 0);
			LocalDateTime endDateTime = startDateTime.plusHours(1);

			LibreTimeExportDTO.LibreTimeShowInstance showInstance = LibreTimeExportDTO.LibreTimeShowInstance.builder()
					.showName(clock.getName() + " - " + hour + ":00")
					.startTime(startDateTime.format(ISO_DATE_TIME))
					.endTime(endDateTime.format(ISO_DATE_TIME))
					.items(new ArrayList<>())
					.build();

			// Add breaks
			if (clock.getBreaks() != null) {
				for (BreakStructureResponseDTO breakItem : clock.getBreaks()) {
					LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
							.type("break")
							.name(breakItem.getName())
							.startTime(breakItem.getStartTime().format(TIME_FORMAT))
							.durationSeconds(breakItem.getDurationSeconds())
							.transition(breakItem.getTransitionCode() != null ? 
									breakItem.getTransitionCode().name() : "SEGUE")
							.build();
					showInstance.getItems().add(item);
				}
			}

			// Add fixed assets
			if (clock.getFixedAssets() != null) {
				for (FixedAssetResponseDTO asset : clock.getFixedAssets()) {
					LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
							.type("fixed_asset")
							.name(asset.getName())
							.startTime(asset.getStartTime().format(TIME_FORMAT))
							.assetType(asset.getAssetType() != null ? asset.getAssetType().name() : null)
							.transition(asset.getTimingType() != null ? 
									asset.getTimingType().name() : "HARD_START")
							.build();
					showInstance.getItems().add(item);
				}
			}

			// Add automation commands
			if (clock.getAutomationCommands() != null) {
				for (AutomationCommandResponseDTO command : clock.getAutomationCommands()) {
					LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
							.type("command")
							.name(command.getCommandType().name())
							.startTime(command.getTriggerTime().format(TIME_FORMAT))
							.commandType(command.getCommandType().name())
							.build();
					showInstance.getItems().add(item);
				}
			}

			// Sort items by start time
			showInstance.getItems().sort(Comparator.comparing(LibreTimeExportDTO.LibreTimeItem::getStartTime));

			showInstances.add(showInstance);
		}

		LibreTimeExportDTO log = LibreTimeExportDTO.builder()
				.name(clock.getName() + " - " + date.toString())
				.description("Daily log generated from clock template: " + clock.getName())
				.showInstances(showInstances)
				.build();

		logger.info("Log generated successfully for clock template {} and date {}", clockTemplateId, date);
		return log;
	}

	@Override
	public String pushLogToLibreTime(UUID clockTemplateId, LocalDate date) {
		logger.info("Pushing log for clock template {} and date {} to LibreTime API", clockTemplateId, date);

		LibreTimeExportDTO log = generateLogFromClock(clockTemplateId, date);

		try {
			String json = objectMapper.writeValueAsString(log);
			return libreTimeClient.pushLog(json).block();
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize log to JSON: {}", e.getMessage());
			throw new RuntimeException("Failed to serialize log", e);
		} catch (Exception e) {
			logger.error("Failed to push log to LibreTime: {}", e.getMessage());
			throw new RuntimeException("Failed to push log to LibreTime", e);
		}
	}

}

