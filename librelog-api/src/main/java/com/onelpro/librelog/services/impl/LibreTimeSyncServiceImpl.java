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
			org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
		this.clockBuilderService = clockBuilderService;
		this.libreTimeClient = libreTimeClient;
		// Use ObjectMapper from provider, or create a new one if not available
		this.objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper());
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
				// Determine item type based on LibreTime mapping
				String itemType = "break";
				if (breakItem.getLibreTimePlaylistId() != null) {
					itemType = "playlist";
				} else if (breakItem.getLibreTimeSmartBlockId() != null) {
					itemType = "smart_block";
				}
				
				LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
						.type(itemType)
						.name(breakItem.getName())
						.startTime(breakItem.getStartTime().format(TIME_FORMAT))
						.durationSeconds(breakItem.getDurationSeconds())
						.transition(breakItem.getTransitionCode() != null ? 
								breakItem.getTransitionCode().name() : "SEGUE")
						// Map transition codes to fade settings
						.fadeIn(mapTransitionToFadeIn(breakItem.getTransitionCode()))
						.fadeOut(mapTransitionToFadeOut(breakItem.getTransitionCode()))
						.build();
				showInstance.getItems().add(item);
			}
		}

		// Convert fixed assets to LibreTime items
		if (clock.getFixedAssets() != null) {
			for (FixedAssetResponseDTO asset : clock.getFixedAssets()) {
				LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
						.type("file") // LibreTime uses "file" type for fixed assets
						.name(asset.getName())
						.startTime(asset.getStartTime().format(TIME_FORMAT))
						.assetType(asset.getAssetType() != null ? asset.getAssetType().name() : null)
						.transition(asset.getTimingType() != null ? 
								asset.getTimingType().name() : "HARD_START")
						// Include LibreTime-specific fields
						.cueIn(asset.getCueInMs() != null ? String.valueOf(asset.getCueInMs()) : null)
						.cueOut(asset.getCueOutMs() != null ? String.valueOf(asset.getCueOutMs()) : null)
						.fadeIn(asset.getFadeInMs() != null ? String.valueOf(asset.getFadeInMs()) : null)
						.fadeOut(asset.getFadeOutMs() != null ? String.valueOf(asset.getFadeOutMs()) : null)
						.build();
				showInstance.getItems().add(item);
			}
		}

		// Convert automation commands to LibreTime items
		if (clock.getAutomationCommands() != null) {
			for (AutomationCommandResponseDTO command : clock.getAutomationCommands()) {
				// Map automation commands to LibreTime playlists or smart blocks
				String itemType = "playlist"; // Default to playlist
				if (command.getLibreTimeSmartBlockId() != null) {
					itemType = "smart_block";
				} else if (command.getLibreTimePlaylistId() != null) {
					itemType = "playlist";
				} else {
					// Map command types to LibreTime equivalents
					itemType = mapCommandTypeToLibreTime(command.getCommandType().name());
				}
				
				LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
						.type(itemType)
						.name(command.getCommandType().name())
						.startTime(command.getTriggerTime().format(TIME_FORMAT))
						.commandType(command.getLibreTimeCommandType() != null ? 
								command.getLibreTimeCommandType() : command.getCommandType().name())
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

			// Add breaks (same logic as exportClock)
			if (clock.getBreaks() != null) {
				for (BreakStructureResponseDTO breakItem : clock.getBreaks()) {
					String itemType = "break";
					if (breakItem.getLibreTimePlaylistId() != null) {
						itemType = "playlist";
					} else if (breakItem.getLibreTimeSmartBlockId() != null) {
						itemType = "smart_block";
					}
					
					LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
							.type(itemType)
							.name(breakItem.getName())
							.startTime(breakItem.getStartTime().format(TIME_FORMAT))
							.durationSeconds(breakItem.getDurationSeconds())
							.transition(breakItem.getTransitionCode() != null ? 
									breakItem.getTransitionCode().name() : "SEGUE")
							.fadeIn(mapTransitionToFadeIn(breakItem.getTransitionCode()))
							.fadeOut(mapTransitionToFadeOut(breakItem.getTransitionCode()))
							.build();
					showInstance.getItems().add(item);
				}
			}

			// Add fixed assets (same logic as exportClock)
			if (clock.getFixedAssets() != null) {
				for (FixedAssetResponseDTO asset : clock.getFixedAssets()) {
					LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
							.type("file")
							.name(asset.getName())
							.startTime(asset.getStartTime().format(TIME_FORMAT))
							.assetType(asset.getAssetType() != null ? asset.getAssetType().name() : null)
							.transition(asset.getTimingType() != null ? 
									asset.getTimingType().name() : "HARD_START")
							.cueIn(asset.getCueInMs() != null ? String.valueOf(asset.getCueInMs()) : null)
							.cueOut(asset.getCueOutMs() != null ? String.valueOf(asset.getCueOutMs()) : null)
							.fadeIn(asset.getFadeInMs() != null ? String.valueOf(asset.getFadeInMs()) : null)
							.fadeOut(asset.getFadeOutMs() != null ? String.valueOf(asset.getFadeOutMs()) : null)
							.build();
					showInstance.getItems().add(item);
				}
			}

			// Add automation commands (same logic as exportClock)
			if (clock.getAutomationCommands() != null) {
				for (AutomationCommandResponseDTO command : clock.getAutomationCommands()) {
					String itemType = "playlist";
					if (command.getLibreTimeSmartBlockId() != null) {
						itemType = "smart_block";
					} else if (command.getLibreTimePlaylistId() != null) {
						itemType = "playlist";
					} else {
						itemType = mapCommandTypeToLibreTime(command.getCommandType().name());
					}
					
					LibreTimeExportDTO.LibreTimeItem item = LibreTimeExportDTO.LibreTimeItem.builder()
							.type(itemType)
							.name(command.getCommandType().name())
							.startTime(command.getTriggerTime().format(TIME_FORMAT))
							.commandType(command.getLibreTimeCommandType() != null ? 
									command.getLibreTimeCommandType() : command.getCommandType().name())
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

	/**
	 * Maps WideOrbit transition codes to LibreTime fade in duration (in milliseconds).
	 * 
	 * @param transitionCode The WideOrbit transition code
	 * @return Fade in duration as string (milliseconds), or null for no fade
	 */
	private String mapTransitionToFadeIn(com.onelpro.librelog.enums.TransitionCode transitionCode) {
		if (transitionCode == null) {
			return "2000"; // Default 2 second fade in for SEGUE
		}
		switch (transitionCode) {
			case SEGUE:
				return "2000"; // 2 second fade in
			case OVERLAP:
				return "3000"; // 3 second fade in for overlap
			case HARD_START:
				return "0"; // No fade
			default:
				return "2000";
		}
	}

	/**
	 * Maps WideOrbit transition codes to LibreTime fade out duration (in milliseconds).
	 * 
	 * @param transitionCode The WideOrbit transition code
	 * @return Fade out duration as string (milliseconds), or null for no fade
	 */
	private String mapTransitionToFadeOut(com.onelpro.librelog.enums.TransitionCode transitionCode) {
		if (transitionCode == null) {
			return "2000"; // Default 2 second fade out for SEGUE
		}
		switch (transitionCode) {
			case SEGUE:
				return "2000"; // 2 second fade out
			case OVERLAP:
				return "1000"; // 1 second fade out for overlap (crossfade)
			case HARD_START:
				return "0"; // No fade
			default:
				return "2000";
		}
	}

	/**
	 * Maps automation command types to LibreTime item types.
	 * 
	 * @param commandType The automation command type name
	 * @return LibreTime item type (playlist, live_input, etc.)
	 */
	private String mapCommandTypeToLibreTime(String commandType) {
		if (commandType == null) {
			return "playlist";
		}
		switch (commandType) {
			case "SWITCH_TO_SATELLITE":
			case "SWITCH_TO_NETWORK":
				return "live_input";
			case "START_STREAMING":
			case "STOP_STREAMING":
				return "playlist"; // Streaming handled via playlist
			case "TRIGGER_EAS":
				return "file"; // EAS alerts are typically files
			default:
				return "playlist";
		}
	}

}

