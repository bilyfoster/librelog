package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ConflictDetailDTO;
import com.onelpro.librelog.dto.ClockValidationResultDTO;
import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AutomationCommand;
import com.onelpro.librelog.models.BreakStructure;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.FixedAsset;
import com.onelpro.librelog.repositories.AutomationCommandRepository;
import com.onelpro.librelog.repositories.BreakStructureRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.repositories.FixedAssetRepository;
import com.onelpro.librelog.services.ClockValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of clock validation service.
 */
@Service
public class ClockValidationServiceImpl implements ClockValidationService {

	private static final Logger logger = LoggerFactory.getLogger(ClockValidationServiceImpl.class);
	private static final int CLOCK_DURATION_SECONDS = 3600; // 60 minutes

	private final ClockTemplateRepository clockTemplateRepository;
	private final BreakStructureRepository breakStructureRepository;
	private final FixedAssetRepository fixedAssetRepository;
	private final AutomationCommandRepository automationCommandRepository;

	public ClockValidationServiceImpl(
			ClockTemplateRepository clockTemplateRepository,
			BreakStructureRepository breakStructureRepository,
			FixedAssetRepository fixedAssetRepository,
			AutomationCommandRepository automationCommandRepository) {
		this.clockTemplateRepository = clockTemplateRepository;
		this.breakStructureRepository = breakStructureRepository;
		this.fixedAssetRepository = fixedAssetRepository;
		this.automationCommandRepository = automationCommandRepository;
	}

	@Override
	public ClockValidationResultDTO validateClock(UUID clockTemplateId) {
		logger.debug("Validating clock template: {}", clockTemplateId);
		ClockValidationResultDTO result = ClockValidationResultDTO.builder().build();

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(clockTemplateId)
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", clockTemplateId);
					return new NotFoundException("Clock template not found with ID: " + clockTemplateId);
				});

		// Run all validation checks and merge results
		ClockValidationResultDTO timingResult = validateTiming(clockTemplateId);
		ClockValidationResultDTO overlapResult = checkOverlaps(clockTemplateId);
		ClockValidationResultDTO conflictResult = detectConflicts(clockTemplateId);

		// Merge results
		result.getErrors().addAll(timingResult.getErrors());
		result.getErrors().addAll(overlapResult.getErrors());
		result.getErrors().addAll(conflictResult.getErrors());
		result.getWarnings().addAll(timingResult.getWarnings());
		result.getWarnings().addAll(overlapResult.getWarnings());
		result.getWarnings().addAll(conflictResult.getWarnings());
		result.getConflictDetails().addAll(overlapResult.getConflictDetails());
		result.getConflictDetails().addAll(conflictResult.getConflictDetails());

		// Update isValid flag if any errors or conflicts exist
		if (!result.getErrors().isEmpty() || !result.getConflictDetails().isEmpty()) {
			result.setIsValid(false);
		}

		logger.debug("Clock validation complete. Valid: {}, Errors: {}, Warnings: {}", 
				result.getIsValid(), result.getErrors().size(), result.getWarnings().size());

		return result;
	}

	@Override
	public ClockValidationResultDTO detectConflicts(UUID clockTemplateId) {
		logger.debug("Detecting conflicts in clock template: {}", clockTemplateId);
		ClockValidationResultDTO result = ClockValidationResultDTO.builder().build();

		// Validate clock template exists
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			result.addError("Clock template not found with ID: " + clockTemplateId);
			return result;
		}

		List<AutomationCommand> commands = automationCommandRepository.findByClockTemplateId(clockTemplateId);

		// Check for high-priority automation commands within 30 seconds of each other
		for (int i = 0; i < commands.size(); i++) {
			AutomationCommand cmd1 = commands.get(i);
			if (!"HIGH".equals(cmd1.getPriority())) {
				continue;
			}

			for (int j = i + 1; j < commands.size(); j++) {
				AutomationCommand cmd2 = commands.get(j);
				if (!"HIGH".equals(cmd2.getPriority())) {
					continue;
				}

				long secondsBetween = Math.abs(
						cmd1.getTriggerTime().toSecondOfDay() - cmd2.getTriggerTime().toSecondOfDay()
				);

				if (secondsBetween < 30) {
					ConflictDetailDTO conflict = ConflictDetailDTO.builder()
							.conflictType("PRIORITY_CONFLICT")
							.elementType("AUTOMATION_COMMAND")
							.elementName(cmd1.getCommandType().toString())
							.startTime(cmd1.getTriggerTime())
							.description("Two high-priority automation commands within 30 seconds may cause automation system crashes")
							.conflictingElementName(cmd2.getCommandType().toString())
							.conflictingStartTime(cmd2.getTriggerTime())
							.build();
					result.addConflict(conflict);
				}
			}
		}

		return result;
	}

	@Override
	public ClockValidationResultDTO checkOverlaps(UUID clockTemplateId) {
		logger.debug("Checking overlaps in clock template: {}", clockTemplateId);
		ClockValidationResultDTO result = ClockValidationResultDTO.builder().build();

		// Validate clock template exists
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			result.addError("Clock template not found with ID: " + clockTemplateId);
			return result;
		}

		List<BreakStructure> breaks = breakStructureRepository.findByClockTemplateId(clockTemplateId);
		List<FixedAsset> fixedAssets = fixedAssetRepository.findByClockTemplateId(clockTemplateId);

		// Check for overlapping breaks
		for (int i = 0; i < breaks.size(); i++) {
			BreakStructure break1 = breaks.get(i);
			LocalTime break1Start = break1.getStartTime();
			LocalTime break1End = break1Start.plusSeconds(break1.getDurationSeconds());

			for (int j = i + 1; j < breaks.size(); j++) {
				BreakStructure break2 = breaks.get(j);
				LocalTime break2Start = break2.getStartTime();
				LocalTime break2End = break2Start.plusSeconds(break2.getDurationSeconds());

				if (overlaps(break1Start, break1End, break2Start, break2End)) {
					ConflictDetailDTO conflict = ConflictDetailDTO.builder()
							.conflictType("OVERLAP")
							.elementType("BREAK")
							.elementName(break1.getName())
							.startTime(break1Start)
							.endTime(break1End)
							.description("Commercial breaks overlap in time")
							.conflictingElementName(break2.getName())
							.conflictingStartTime(break2Start)
							.conflictingEndTime(break2End)
							.build();
					result.addConflict(conflict);
				}
			}
		}

		// Check for breaks overlapping with fixed assets
		for (BreakStructure breakStructure : breaks) {
			LocalTime breakStart = breakStructure.getStartTime();
			LocalTime breakEnd = breakStart.plusSeconds(breakStructure.getDurationSeconds());

			for (FixedAsset fixedAsset : fixedAssets) {
				LocalTime assetStart = fixedAsset.getStartTime();
				// Fixed assets are typically short (seconds), so we'll check if break overlaps with asset time
				// For simplicity, we'll consider a fixed asset as a 1-second window
				LocalTime assetEnd = assetStart.plusSeconds(1);

				if (overlaps(breakStart, breakEnd, assetStart, assetEnd)) {
					ConflictDetailDTO conflict = ConflictDetailDTO.builder()
							.conflictType("OVERLAP")
							.elementType("BREAK")
							.elementName(breakStructure.getName())
							.startTime(breakStart)
							.endTime(breakEnd)
							.description("Commercial break overlaps with fixed asset")
							.conflictingElementName(fixedAsset.getName())
							.conflictingStartTime(assetStart)
							.conflictingEndTime(assetEnd)
							.build();
					result.addConflict(conflict);
				}
			}
		}

		return result;
	}

	@Override
	public ClockValidationResultDTO validateTiming(UUID clockTemplateId) {
		logger.debug("Validating timing in clock template: {}", clockTemplateId);
		ClockValidationResultDTO result = ClockValidationResultDTO.builder().build();

		// Validate clock template exists
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			result.addError("Clock template not found with ID: " + clockTemplateId);
			return result;
		}

		List<BreakStructure> breaks = breakStructureRepository.findByClockTemplateId(clockTemplateId);
		List<FixedAsset> fixedAssets = fixedAssetRepository.findByClockTemplateId(clockTemplateId);

		// Validate breaks don't exceed 60-minute clock
		for (BreakStructure breakStructure : breaks) {
			int startMinutes = breakStructure.getStartTime().getMinute();
			int startSeconds = breakStructure.getStartTime().getSecond();
			int totalSeconds = (startMinutes * 60) + startSeconds + breakStructure.getDurationSeconds();

			if (totalSeconds > CLOCK_DURATION_SECONDS) {
				result.addError(String.format(
						"Break '%s' exceeds 60-minute clock limit (starts at %s, duration %d seconds)",
						breakStructure.getName(),
						breakStructure.getStartTime(),
						breakStructure.getDurationSeconds()
				));
			}

			// Validate hard start breaks have exact minute timing
			if (breakStructure.getTimingType() == TimingType.HARD_START) {
				if (breakStructure.getStartTime().getSecond() != 0) {
					result.addWarning(String.format(
							"Break '%s' is marked as HARD_START but has non-zero seconds (%d). Hard starts should be at exact minutes.",
							breakStructure.getName(),
							breakStructure.getStartTime().getSecond()
					));
				}
			}
		}

		// Validate fixed assets don't exceed 60-minute clock
		for (FixedAsset fixedAsset : fixedAssets) {
			int startMinutes = fixedAsset.getStartTime().getMinute();
			int startSeconds = fixedAsset.getStartTime().getSecond();
			// Fixed assets are typically very short (1-5 seconds), so we'll check if they're within bounds
			if (startMinutes >= 60) {
				result.addError(String.format(
						"Fixed asset '%s' start time exceeds 60-minute clock limit (starts at %s)",
						fixedAsset.getName(),
						fixedAsset.getStartTime()
				));
			}

			// Validate hard start fixed assets have exact timing
			if (fixedAsset.getTimingType() == TimingType.HARD_START) {
				if (fixedAsset.getStartTime().getSecond() != 0) {
					result.addWarning(String.format(
							"Fixed asset '%s' is marked as HARD_START but has non-zero seconds (%d). Hard starts should be at exact minutes.",
							fixedAsset.getName(),
							fixedAsset.getStartTime().getSecond()
					));
				}
			}
		}

		return result;
	}

	/**
	 * Check if two time ranges overlap.
	 */
	private boolean overlaps(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
		// Handle wrap-around (end time is before start time, meaning it wraps to next hour)
		// For simplicity, we'll assume all times are within the same 60-minute window
		return start1.isBefore(end2) && start2.isBefore(end1);
	}

}

