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
	private final org.springframework.beans.factory.ObjectProvider<com.onelpro.librelog.services.LibreTimeFileSyncService> fileSyncServiceProvider;
	private final org.springframework.beans.factory.ObjectProvider<com.onelpro.librelog.services.LibreTimeSyncHistoryService> syncHistoryServiceProvider;

	public LibreTimeSyncServiceImpl(
			ClockBuilderService clockBuilderService,
			LibreTimeClient libreTimeClient,
			org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider,
			org.springframework.beans.factory.ObjectProvider<com.onelpro.librelog.services.LibreTimeFileSyncService> fileSyncServiceProvider,
			org.springframework.beans.factory.ObjectProvider<com.onelpro.librelog.services.LibreTimeSyncHistoryService> syncHistoryServiceProvider) {
		this.clockBuilderService = clockBuilderService;
		this.libreTimeClient = libreTimeClient;
		// Use ObjectMapper from provider, or create a new one if not available
		this.objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper());
		this.fileSyncServiceProvider = fileSyncServiceProvider;
		this.syncHistoryServiceProvider = syncHistoryServiceProvider;
	}

	@Override
	public ClockExportValidationResultDTO validateClockTemplate(UUID clockTemplateId) {
		logger.info("Validating clock template {} before export", clockTemplateId);
		ClockExportValidationResultDTO result = ClockExportValidationResultDTO.builder()
				.clockTemplateId(clockTemplateId)
				.isValid(true)
				.errors(new ArrayList<>())
				.warnings(new ArrayList<>())
				.totalItems(0)
				.validItems(0)
				.invalidItems(0)
				.build();

		try {
			ClockTemplateWithBreaksDTO clock = clockBuilderService.getClockStructure(clockTemplateId);

			// Validate clock name
			if (clock.getName() == null || clock.getName().trim().isEmpty()) {
				result.addError("name", "Clock template name is required");
				result.setIsValid(false);
			}

			int totalItems = 0;
			int validItems = 0;

			// Validate breaks
			if (clock.getBreaks() != null) {
				for (BreakStructureResponseDTO breakItem : clock.getBreaks()) {
					totalItems++;
					boolean isValid = true;

					// Validate break name
					if (breakItem.getName() == null || breakItem.getName().trim().isEmpty()) {
						result.addError("breaks[" + totalItems + "].name", "Break name is required");
						isValid = false;
					}

					// Validate start time
					if (breakItem.getStartTime() == null) {
						result.addError("breaks[" + totalItems + "].startTime", "Break start time is required");
						isValid = false;
					}

					// Validate duration
					if (breakItem.getDurationSeconds() == null || breakItem.getDurationSeconds() <= 0) {
						result.addError("breaks[" + totalItems + "].durationSeconds", "Break duration must be greater than 0");
						isValid = false;
					}

					// Check if referenced LibreTime resources exist
					if (breakItem.getLibreTimePlaylistId() != null) {
						// Could check if playlist exists in LibreTime
						result.addWarning("breaks[" + totalItems + "].libreTimePlaylistId", 
								"Playlist existence not verified in LibreTime");
					}
					if (breakItem.getLibreTimeSmartBlockId() != null) {
						result.addWarning("breaks[" + totalItems + "].libreTimeSmartBlockId", 
								"Smart block existence not verified in LibreTime");
					}

					if (isValid) {
						validItems++;
					}
				}
			}

			// Validate fixed assets
			if (clock.getFixedAssets() != null) {
				for (FixedAssetResponseDTO asset : clock.getFixedAssets()) {
					totalItems++;
					boolean isValid = true;

					// Validate asset name
					if (asset.getName() == null || asset.getName().trim().isEmpty()) {
						result.addError("fixedAssets[" + totalItems + "].name", "Asset name is required");
						isValid = false;
					}

					// Validate start time
					if (asset.getStartTime() == null) {
						result.addError("fixedAssets[" + totalItems + "].startTime", "Asset start time is required");
						isValid = false;
					}

					// Check if file exists in LibreTime (if cart ID is provided)
					if (asset.getLibreTimeCartId() != null) {
						try {
							com.onelpro.librelog.services.LibreTimeFileSyncService fileSyncService = 
									fileSyncServiceProvider.getIfAvailable();
							if (fileSyncService != null) {
								com.onelpro.librelog.dto.SyncStatusResponseDTO syncStatus = 
										fileSyncService.getSyncStatus(null, asset.getLibreTimeCartId());
								if (syncStatus == null || syncStatus.getSyncStatus() == null) {
									result.addWarning("fixedAssets[" + totalItems + "].libreTimeCartId", 
											"File existence in LibreTime not verified");
								}
							} else {
								result.addWarning("fixedAssets[" + totalItems + "].libreTimeCartId", 
										"File sync service not available - cannot verify file existence in LibreTime");
							}
						} catch (Exception e) {
							result.addWarning("fixedAssets[" + totalItems + "].libreTimeCartId", 
									"Could not verify file existence in LibreTime: " + e.getMessage());
						}
					} else {
						result.addWarning("fixedAssets[" + totalItems + "].libreTimeCartId", 
								"Asset does not have a LibreTime cart ID - may not exist in LibreTime");
					}

					if (isValid) {
						validItems++;
					}
				}
			}

			// Validate automation commands
			if (clock.getAutomationCommands() != null) {
				for (AutomationCommandResponseDTO command : clock.getAutomationCommands()) {
					totalItems++;
					boolean isValid = true;

					// Validate command type
					if (command.getCommandType() == null) {
						result.addError("automationCommands[" + totalItems + "].commandType", 
								"Command type is required");
						isValid = false;
					}

					// Validate trigger time
					if (command.getTriggerTime() == null) {
						result.addError("automationCommands[" + totalItems + "].triggerTime", 
								"Command trigger time is required");
						isValid = false;
					}

					if (isValid) {
						validItems++;
					}
				}
			}

			result.setTotalItems(totalItems);
			result.setValidItems(validItems);
			result.setInvalidItems(totalItems - validItems);

			// Check for timing conflicts within the clock
			validateTimingConflicts(clock, result);

			if (!result.getErrors().isEmpty()) {
				result.setIsValid(false);
			}

		} catch (Exception e) {
			logger.error("Error validating clock template {}: {}", clockTemplateId, e.getMessage());
			result.addError("general", "Error validating clock template: " + e.getMessage());
			result.setIsValid(false);
		}

		logger.info("Clock template {} validation completed. Valid: {}, Errors: {}, Warnings: {}", 
				clockTemplateId, result.getIsValid(), result.getErrors().size(), result.getWarnings().size());
		return result;
	}

	/**
	 * Validates timing conflicts within a clock template.
	 */
	private void validateTimingConflicts(ClockTemplateWithBreaksDTO clock, ClockExportValidationResultDTO result) {
		List<com.onelpro.librelog.dto.ClockExportValidationResultDTO.ValidationWarningDTO> timingWarnings = new ArrayList<>();

		// Check for overlapping items (simplified - would need more sophisticated logic for real conflict detection)
		// This is a basic check - in production, you'd want more sophisticated conflict detection
		if (clock.getBreaks() != null && clock.getFixedAssets() != null) {
			for (BreakStructureResponseDTO breakItem : clock.getBreaks()) {
				if (breakItem.getStartTime() == null || breakItem.getDurationSeconds() == null) {
					continue;
				}
				java.time.Duration breakStart = java.time.Duration.between(
						java.time.LocalTime.MIDNIGHT, breakItem.getStartTime());
				java.time.Duration breakEnd = breakStart.plusSeconds(breakItem.getDurationSeconds());

				for (FixedAssetResponseDTO asset : clock.getFixedAssets()) {
					if (asset.getStartTime() == null) {
						continue;
					}
					java.time.Duration assetStart = java.time.Duration.between(
							java.time.LocalTime.MIDNIGHT, asset.getStartTime());

					// Check if asset overlaps with break
					if (assetStart.compareTo(breakStart) >= 0 && assetStart.compareTo(breakEnd) < 0) {
						result.addWarning("timing", 
								String.format("Fixed asset '%s' overlaps with break '%s' at %s", 
										asset.getName(), breakItem.getName(), asset.getStartTime()));
					}
				}
			}
		}
	}

	@Override
	public LibreTimeExportDTO exportClock(UUID clockTemplateId) {
		logger.info("Exporting clock template {} to LibreTime format", clockTemplateId);
		
		// Perform validation first
		ClockExportValidationResultDTO validation = validateClockTemplate(clockTemplateId);
		if (!validation.getIsValid() && !validation.getErrors().isEmpty()) {
			logger.warn("Exporting clock template {} with validation errors: {}", 
					clockTemplateId, validation.getErrors().size());
		}

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
	public ClockExportResultDTO pushClockToLibreTime(UUID clockTemplateId) {
		logger.info("Pushing clock template {} to LibreTime API", clockTemplateId);

		// Create sync history record
		com.onelpro.librelog.services.LibreTimeSyncHistoryService syncHistoryService = 
				syncHistoryServiceProvider.getIfAvailable();
		java.util.UUID historyId = null;
		if (syncHistoryService != null) {
			try {
				// TODO: Get actual user ID from security context
				java.util.UUID userId = java.util.UUID.randomUUID(); // Placeholder
				var history = syncHistoryService.createSyncHistory(
						com.onelpro.librelog.enums.SyncType.LOG_EXPORT,
						userId,
						null);
				historyId = history.getId();
			} catch (Exception e) {
				logger.warn("Failed to create sync history record: {}", e.getMessage());
			}
		}

		ClockExportResultDTO result = ClockExportResultDTO.builder()
				.clockTemplateId(clockTemplateId)
				.success(false)
				.exportedAt(LocalDateTime.now())
				.totalShowInstances(0)
				.successfulShowInstances(0)
				.failedShowInstances(0)
				.failures(new ArrayList<>())
				.warnings(new ArrayList<>())
				.build();

		// Validate before export
		ClockExportValidationResultDTO validation = validateClockTemplate(clockTemplateId);
		if (!validation.getIsValid()) {
			result.setMessage("Clock template validation failed. Please fix errors before exporting.");
			for (ClockExportValidationResultDTO.ValidationErrorDTO error : validation.getErrors()) {
				result.addFailure("validation", error.getMessage());
			}
			result.setFailedShowInstances(validation.getInvalidItems());
			
			// Update sync history
			if (syncHistoryService != null && historyId != null) {
				try {
					syncHistoryService.completeSyncHistory(historyId, "failed", 0, result.getFailedShowInstances(), 
							"Validation failed");
				} catch (Exception e) {
					logger.warn("Failed to update sync history record: {}", e.getMessage());
				}
			}
			return result;
		}

		// Add validation warnings to result
		for (ClockExportValidationResultDTO.ValidationWarningDTO warning : validation.getWarnings()) {
			result.addWarning("validation", warning.getMessage());
		}

		try {
			LibreTimeExportDTO export = exportClock(clockTemplateId);
			result.setTotalShowInstances(export.getShowInstances() != null ? export.getShowInstances().size() : 0);

			// Check for scheduling conflicts
			List<String> conflicts = detectSchedulingConflicts(export);
			if (!conflicts.isEmpty()) {
				for (String conflict : conflicts) {
					result.addWarning("scheduling", conflict);
				}
			}

			String json = objectMapper.writeValueAsString(export);
			String libreTimeResponse = libreTimeClient.exportClock(json).block();
			result.setLibreTimeResponse(libreTimeResponse);
			result.setSuccess(true);
			result.setMessage("Clock template exported successfully to LibreTime");
			result.setSuccessfulShowInstances(result.getTotalShowInstances());

			// Update sync history
			if (syncHistoryService != null && historyId != null) {
				try {
					syncHistoryService.completeSyncHistory(historyId, "completed", 
							result.getSuccessfulShowInstances(), result.getFailedShowInstances(), null);
				} catch (Exception e) {
					logger.warn("Failed to update sync history record: {}", e.getMessage());
				}
			}

		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize clock export to JSON: {}", e.getMessage());
			result.setMessage("Failed to serialize clock export: " + e.getMessage());
			result.addFailure("serialization", "Failed to serialize clock export: " + e.getMessage());
			result.setFailedShowInstances(result.getTotalShowInstances());
			
			// Update sync history
			if (syncHistoryService != null && historyId != null) {
				try {
					syncHistoryService.completeSyncHistory(historyId, "failed", 0, result.getFailedShowInstances(), 
							e.getMessage());
				} catch (Exception ex) {
					logger.warn("Failed to update sync history record: {}", ex.getMessage());
				}
			}
		} catch (Exception e) {
			logger.error("Failed to push clock to LibreTime: {}", e.getMessage());
			result.setMessage("Failed to push clock to LibreTime: " + e.getMessage());
			result.addFailure("api", "Failed to push clock to LibreTime: " + e.getMessage());
			result.setFailedShowInstances(result.getTotalShowInstances());
			
			// Update sync history
			if (syncHistoryService != null && historyId != null) {
				try {
					syncHistoryService.completeSyncHistory(historyId, "failed", 0, result.getFailedShowInstances(), 
							e.getMessage());
				} catch (Exception ex) {
					logger.warn("Failed to update sync history record: {}", ex.getMessage());
				}
			}
		}

		return result;
	}

	@Override
	public LibreTimeExportDTO generateLogFromClock(UUID clockTemplateId, LocalDate startDate, LocalDate endDate) {
		logger.info("Generating log from clock template {} for date range {} to {}", 
				clockTemplateId, startDate, endDate);

		ClockTemplateWithBreaksDTO clock = clockBuilderService.getClockStructure(clockTemplateId);
		List<LibreTimeExportDTO.LibreTimeShowInstance> showInstances = new ArrayList<>();

		// Generate show instances for each day in the range
		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {
			List<LibreTimeExportDTO.LibreTimeShowInstance> dayInstances = generateShowInstancesForDate(clock, currentDate);
			showInstances.addAll(dayInstances);
			currentDate = currentDate.plusDays(1);
		}

		LibreTimeExportDTO log = LibreTimeExportDTO.builder()
				.name(clock.getName() + " - " + startDate + " to " + endDate)
				.description("Log generated from clock template: " + clock.getName() + " for date range " + startDate + " to " + endDate)
				.showInstances(showInstances)
				.build();

		logger.info("Log generated successfully for clock template {} and date range {} to {}", 
				clockTemplateId, startDate, endDate);
		return log;
	}

	@Override
	public LibreTimeExportDTO generateLogFromClock(UUID clockTemplateId, LocalDate date) {
		logger.info("Generating log from clock template {} for date {}", clockTemplateId, date);

		ClockTemplateWithBreaksDTO clock = clockBuilderService.getClockStructure(clockTemplateId);

		// Generate show instances for each hour of the day (24 hours)
		List<LibreTimeExportDTO.LibreTimeShowInstance> showInstances = new ArrayList<>();

		List<LibreTimeExportDTO.LibreTimeShowInstance> dayInstances = generateShowInstancesForDate(clock, date);

		LibreTimeExportDTO log = LibreTimeExportDTO.builder()
				.name(clock.getName() + " - " + date.toString())
				.description("Daily log generated from clock template: " + clock.getName())
				.showInstances(dayInstances)
				.build();

		logger.info("Log generated successfully for clock template {} and date {}", clockTemplateId, date);
		return log;
	}

	/**
	 * Generates show instances for a specific date (24 hours).
	 */
	private List<LibreTimeExportDTO.LibreTimeShowInstance> generateShowInstancesForDate(
			ClockTemplateWithBreaksDTO clock, LocalDate date) {
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

		return showInstances;
	}

	@Override
	public List<String> detectSchedulingConflicts(LibreTimeExportDTO export) {
		logger.debug("Detecting scheduling conflicts for export: {}", export.getName());
		List<String> conflicts = new ArrayList<>();

		if (export.getShowInstances() == null || export.getShowInstances().isEmpty()) {
			return conflicts;
		}

		// Check for overlapping show instances within the export
		for (int i = 0; i < export.getShowInstances().size(); i++) {
			LibreTimeExportDTO.LibreTimeShowInstance instance1 = export.getShowInstances().get(i);
			LocalDateTime start1 = LocalDateTime.parse(instance1.getStartTime(), ISO_DATE_TIME);
			LocalDateTime end1 = LocalDateTime.parse(instance1.getEndTime(), ISO_DATE_TIME);

			for (int j = i + 1; j < export.getShowInstances().size(); j++) {
				LibreTimeExportDTO.LibreTimeShowInstance instance2 = export.getShowInstances().get(j);
				LocalDateTime start2 = LocalDateTime.parse(instance2.getStartTime(), ISO_DATE_TIME);
				LocalDateTime end2 = LocalDateTime.parse(instance2.getEndTime(), ISO_DATE_TIME);

				// Check if instances overlap
				if (start1.isBefore(end2) && start2.isBefore(end1)) {
					conflicts.add(String.format(
							"Show instance '%s' (%s to %s) overlaps with '%s' (%s to %s)",
							instance1.getShowName(), instance1.getStartTime(), instance1.getEndTime(),
							instance2.getShowName(), instance2.getStartTime(), instance2.getEndTime()));
				}
			}
		}

		// Note: In a full implementation, you would also check against existing shows in LibreTime
		// This would require querying the LibreTime API for existing show instances

		logger.debug("Found {} scheduling conflicts", conflicts.size());
		return conflicts;
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

