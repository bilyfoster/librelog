package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ClockValidationResultDTO;
import com.onelpro.librelog.enums.AutomationCommandType;
import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AutomationCommand;
import com.onelpro.librelog.models.BreakStructure;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.FixedAsset;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.AutomationCommandRepository;
import com.onelpro.librelog.repositories.BreakStructureRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.repositories.FixedAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClockValidationServiceImplTest {

	@Mock
	private ClockTemplateRepository clockTemplateRepository;

	@Mock
	private BreakStructureRepository breakStructureRepository;

	@Mock
	private FixedAssetRepository fixedAssetRepository;

	@Mock
	private AutomationCommandRepository automationCommandRepository;

	@InjectMocks
	private ClockValidationServiceImpl clockValidationService;

	private ClockTemplate clockTemplate;
	private UUID clockTemplateId;

	@BeforeEach
	void setUp() {
		clockTemplateId = UUID.randomUUID();

		Station station = Station.builder()
				.id(UUID.randomUUID())
				.name("Test Station")
				.callSign("TEST")
				.build();

		Channel channel = Channel.builder()
				.id(UUID.randomUUID())
				.station(station)
				.name("HD-1")
				.build();

		clockTemplate = ClockTemplate.builder()
				.id(clockTemplateId)
				.channel(channel)
				.name("Morning Clock")
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void validateClock_When_ClockTemplateNotFound_Expect_NotFoundException() {
		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> clockValidationService.validateClock(clockTemplateId));
	}

	@Test
	void validateClock_When_ValidClock_Expect_Success() {
		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());
		when(automationCommandRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());

		ClockValidationResultDTO result = clockValidationService.validateClock(clockTemplateId);

		assertNotNull(result);
		assertTrue(result.getIsValid());
		assertEquals(0, result.getErrors().size());
		assertEquals(0, result.getWarnings().size());
		assertEquals(0, result.getConflictDetails().size());
	}

	@Test
	void checkOverlaps_When_BreaksOverlap_Expect_Conflict() {
		BreakStructure break1 = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break A")
				.startTime(LocalTime.of(0, 15))
				.durationSeconds(180)
				.build();

		BreakStructure break2 = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break B")
				.startTime(LocalTime.of(0, 17))
				.durationSeconds(180)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(break1, break2));
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());

		ClockValidationResultDTO result = clockValidationService.checkOverlaps(clockTemplateId);

		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertTrue(result.getConflictDetails().size() > 0);
	}

	@Test
	void checkOverlaps_When_NoOverlaps_Expect_Success() {
		BreakStructure break1 = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break A")
				.startTime(LocalTime.of(0, 15))
				.durationSeconds(180)
				.build();

		BreakStructure break2 = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break B")
				.startTime(LocalTime.of(0, 25))
				.durationSeconds(180)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(break1, break2));
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());

		ClockValidationResultDTO result = clockValidationService.checkOverlaps(clockTemplateId);

		assertNotNull(result);
		assertTrue(result.getIsValid());
		assertEquals(0, result.getConflictDetails().size());
	}

	@Test
	void detectConflicts_When_HighPriorityCommandsWithin30Seconds_Expect_Conflict() {
		AutomationCommand cmd1 = AutomationCommand.builder()
				.id(UUID.randomUUID())
				.commandType(AutomationCommandType.SWITCH_TO_SATELLITE)
				.triggerTime(LocalTime.of(0, 0))
				.priority("HIGH")
				.build();

		AutomationCommand cmd2 = AutomationCommand.builder()
				.id(UUID.randomUUID())
				.commandType(AutomationCommandType.START_RECORDING)
				.triggerTime(LocalTime.of(0, 0, 15))
				.priority("HIGH")
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(automationCommandRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(cmd1, cmd2));

		ClockValidationResultDTO result = clockValidationService.detectConflicts(clockTemplateId);

		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertTrue(result.getConflictDetails().size() > 0);
	}

	@Test
	void validateTiming_When_BreakExceeds60Minutes_Expect_Error() {
		BreakStructure breakStructure = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break A")
				.startTime(LocalTime.of(0, 55))
				.durationSeconds(600) // 10 minutes, would exceed 60-minute limit
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(breakStructure));
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());

		ClockValidationResultDTO result = clockValidationService.validateTiming(clockTemplateId);

		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertTrue(result.getErrors().size() > 0);
	}

	@Test
	void validateTiming_When_TotalBreakTimeExceeds18Minutes_Expect_Warning() {
		BreakStructure break1 = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break A")
				.startTime(LocalTime.of(0, 15))
				.durationSeconds(600) // 10 minutes
				.build();

		BreakStructure break2 = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break B")
				.startTime(LocalTime.of(0, 30))
				.durationSeconds(600) // 10 minutes
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(break1, break2));
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());

		ClockValidationResultDTO result = clockValidationService.validateTiming(clockTemplateId);

		assertNotNull(result);
		assertTrue(result.getWarnings().size() > 0);
	}

	@Test
	void validateTiming_When_HardStartHasNonZeroSeconds_Expect_Warning() {
		BreakStructure breakStructure = BreakStructure.builder()
				.id(UUID.randomUUID())
				.name("Break A")
				.startTime(LocalTime.of(0, 15, 30))
				.durationSeconds(180)
				.timingType(TimingType.HARD_START)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(breakStructure));
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId)).thenReturn(Collections.emptyList());

		ClockValidationResultDTO result = clockValidationService.validateTiming(clockTemplateId);

		assertNotNull(result);
		assertTrue(result.getWarnings().size() > 0);
	}

}

