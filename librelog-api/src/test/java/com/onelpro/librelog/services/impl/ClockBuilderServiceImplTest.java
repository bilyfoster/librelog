package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClockBuilderServiceImplTest {

	@Mock
	private ClockService clockService;

	@Mock
	private BreakStructureService breakStructureService;

	@Mock
	private FixedAssetService fixedAssetService;

	@Mock
	private AutomationCommandService automationCommandService;

	@Mock
	private ClockValidationService clockValidationService;

	@Mock
	private ClockTemplateRepository clockTemplateRepository;

	@InjectMocks
	private ClockBuilderServiceImpl clockBuilderService;

	private UUID clockTemplateId;
	private ClockTemplateResponseDTO clockTemplateResponse;
	private ClockTemplate clockTemplate;

	@BeforeEach
	void setUp() {
		clockTemplateId = UUID.randomUUID();
		clockTemplateResponse = ClockTemplateResponseDTO.builder()
				.id(clockTemplateId)
				.name("Test Clock")
				.description("Test Description")
				.channelId(UUID.randomUUID())
				.channelName("Test Channel")
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		clockTemplate = ClockTemplate.builder()
				.id(clockTemplateId)
				.name("Test Clock")
				.build();
	}

	@Test
	void getClockStructure_When_ClockTemplateExists_Expect_StructureReturned() {
		// Arrange
		List<BreakStructureResponseDTO> breaks = new ArrayList<>();
		List<FixedAssetResponseDTO> fixedAssets = new ArrayList<>();
		List<AutomationCommandResponseDTO> commands = new ArrayList<>();

		when(clockService.getById(clockTemplateId)).thenReturn(clockTemplateResponse);
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);
		when(fixedAssetService.getByClockTemplateId(clockTemplateId)).thenReturn(fixedAssets);
		when(automationCommandService.getByClockTemplateId(clockTemplateId)).thenReturn(commands);

		// Act
		ClockTemplateWithBreaksDTO result = clockBuilderService.getClockStructure(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertEquals(clockTemplateId, result.getId());
		assertEquals("Test Clock", result.getName());
		assertEquals(breaks, result.getBreaks());
		assertEquals(fixedAssets, result.getFixedAssets());
		assertEquals(commands, result.getAutomationCommands());
	}

	@Test
	void getClockStructure_When_ClockTemplateNotFound_Expect_NotFoundException() {
		// Arrange
		when(clockService.getById(clockTemplateId)).thenReturn(null);

		// Act & Assert
		assertThrows(NotFoundException.class, () -> clockBuilderService.getClockStructure(clockTemplateId));
	}

	@Test
	void addBreak_When_ValidRequest_Expect_BreakAddedAndValidated() {
		// Arrange
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.name("Break 1")
				.startTime(LocalTime.of(10, 0))
				.durationSeconds(60)
				.build();

		BreakStructureResponseDTO response = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 1")
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureService.create(any(BreakStructureRequestDTO.class))).thenReturn(response);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		BreakStructureResponseDTO result = clockBuilderService.addBreak(clockTemplateId, request);

		// Assert
		assertNotNull(result);
		assertEquals("Break 1", result.getName());
		assertEquals(clockTemplateId, request.getClockTemplateId());
		verify(breakStructureService).create(any(BreakStructureRequestDTO.class));
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void updateBreak_When_ValidRequest_Expect_BreakUpdatedAndValidated() {
		// Arrange
		UUID breakId = UUID.randomUUID();
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Updated Break")
				.startTime(LocalTime.of(10, 0))
				.durationSeconds(60)
				.build();

		BreakStructureResponseDTO response = BreakStructureResponseDTO.builder()
				.id(breakId)
				.name("Updated Break")
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(breakStructureService.update(eq(breakId), any(BreakStructureRequestDTO.class))).thenReturn(response);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		BreakStructureResponseDTO result = clockBuilderService.updateBreak(breakId, request);

		// Assert
		assertNotNull(result);
		assertEquals("Updated Break", result.getName());
		verify(breakStructureService).update(eq(breakId), any(BreakStructureRequestDTO.class));
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void removeBreak_When_BreakExists_Expect_BreakRemovedAndValidated() {
		// Arrange
		UUID breakId = UUID.randomUUID();
		BreakStructureResponseDTO breakStructure = BreakStructureResponseDTO.builder()
				.id(breakId)
				.clockTemplateId(clockTemplateId)
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(breakStructureService.getById(breakId)).thenReturn(breakStructure);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		clockBuilderService.removeBreak(breakId);

		// Assert
		verify(breakStructureService).getById(breakId);
		verify(breakStructureService).delete(breakId);
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void addFixedAsset_When_ValidRequest_Expect_FixedAssetAddedAndValidated() {
		// Arrange
		FixedAssetRequestDTO request = FixedAssetRequestDTO.builder()
				.name("Legal ID")
				.assetType(com.onelpro.librelog.enums.AssetType.IM)
				.startTime(LocalTime.of(0, 0))
				.build();

		FixedAssetResponseDTO response = FixedAssetResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Legal ID")
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(fixedAssetService.create(any(FixedAssetRequestDTO.class))).thenReturn(response);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		FixedAssetResponseDTO result = clockBuilderService.addFixedAsset(clockTemplateId, request);

		// Assert
		assertNotNull(result);
		assertEquals("Legal ID", result.getName());
		assertEquals(clockTemplateId, request.getClockTemplateId());
		verify(fixedAssetService).create(any(FixedAssetRequestDTO.class));
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void updateFixedAsset_When_ValidRequest_Expect_FixedAssetUpdatedAndValidated() {
		// Arrange
		UUID fixedAssetId = UUID.randomUUID();
		FixedAssetRequestDTO request = FixedAssetRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Updated Legal ID")
				.assetType(com.onelpro.librelog.enums.AssetType.IM)
				.startTime(LocalTime.of(0, 0))
				.build();

		FixedAssetResponseDTO response = FixedAssetResponseDTO.builder()
				.id(fixedAssetId)
				.name("Updated Legal ID")
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(fixedAssetService.update(eq(fixedAssetId), any(FixedAssetRequestDTO.class))).thenReturn(response);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		FixedAssetResponseDTO result = clockBuilderService.updateFixedAsset(fixedAssetId, request);

		// Assert
		assertNotNull(result);
		assertEquals("Updated Legal ID", result.getName());
		verify(fixedAssetService).update(eq(fixedAssetId), any(FixedAssetRequestDTO.class));
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void removeFixedAsset_When_FixedAssetExists_Expect_FixedAssetRemovedAndValidated() {
		// Arrange
		UUID fixedAssetId = UUID.randomUUID();
		FixedAssetResponseDTO fixedAsset = FixedAssetResponseDTO.builder()
				.id(fixedAssetId)
				.clockTemplateId(clockTemplateId)
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(fixedAssetService.getById(fixedAssetId)).thenReturn(fixedAsset);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		clockBuilderService.removeFixedAsset(fixedAssetId);

		// Assert
		verify(fixedAssetService).getById(fixedAssetId);
		verify(fixedAssetService).delete(fixedAssetId);
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void addAutomationCommand_When_ValidRequest_Expect_CommandAddedAndValidated() {
		// Arrange
		AutomationCommandRequestDTO request = AutomationCommandRequestDTO.builder()
				.commandType(com.onelpro.librelog.enums.AutomationCommandType.START_RECORDING)
				.triggerTime(LocalTime.of(6, 0))
				.build();

		AutomationCommandResponseDTO response = AutomationCommandResponseDTO.builder()
				.id(UUID.randomUUID())
				.commandType(com.onelpro.librelog.enums.AutomationCommandType.START_RECORDING)
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(automationCommandService.create(any(AutomationCommandRequestDTO.class))).thenReturn(response);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		AutomationCommandResponseDTO result = clockBuilderService.addAutomationCommand(clockTemplateId, request);

		// Assert
		assertNotNull(result);
		assertEquals(com.onelpro.librelog.enums.AutomationCommandType.START_RECORDING, result.getCommandType());
		assertEquals(clockTemplateId, request.getClockTemplateId());
		verify(automationCommandService).create(any(AutomationCommandRequestDTO.class));
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void updateAutomationCommand_When_ValidRequest_Expect_CommandUpdatedAndValidated() {
		// Arrange
		UUID commandId = UUID.randomUUID();
		AutomationCommandRequestDTO request = AutomationCommandRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.commandType(com.onelpro.librelog.enums.AutomationCommandType.STOP_STREAMING)
				.triggerTime(LocalTime.of(10, 0))
				.build();

		AutomationCommandResponseDTO response = AutomationCommandResponseDTO.builder()
				.id(commandId)
				.commandType(com.onelpro.librelog.enums.AutomationCommandType.STOP_STREAMING)
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(automationCommandService.update(eq(commandId), any(AutomationCommandRequestDTO.class))).thenReturn(response);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		AutomationCommandResponseDTO result = clockBuilderService.updateAutomationCommand(commandId, request);

		// Assert
		assertNotNull(result);
		assertEquals(com.onelpro.librelog.enums.AutomationCommandType.STOP_STREAMING, result.getCommandType());
		verify(automationCommandService).update(eq(commandId), any(AutomationCommandRequestDTO.class));
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void removeAutomationCommand_When_CommandExists_Expect_CommandRemovedAndValidated() {
		// Arrange
		UUID commandId = UUID.randomUUID();
		AutomationCommandResponseDTO command = AutomationCommandResponseDTO.builder()
				.id(commandId)
				.clockTemplateId(clockTemplateId)
				.build();

		ClockValidationResultDTO validationResult = ClockValidationResultDTO.builder()
				.isValid(true)
				.build();

		when(automationCommandService.getById(commandId)).thenReturn(command);
		when(clockValidationService.validateClock(clockTemplateId)).thenReturn(validationResult);

		// Act
		clockBuilderService.removeAutomationCommand(commandId);

		// Assert
		verify(automationCommandService).getById(commandId);
		verify(automationCommandService).delete(commandId);
		verify(clockValidationService).validateClock(clockTemplateId);
	}

	@Test
	void addBreak_When_ClockTemplateNotFound_Expect_NotFoundException() {
		// Arrange
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.name("Break 1")
				.startTime(LocalTime.of(10, 0))
				.durationSeconds(60)
				.build();

		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(false);

		// Act & Assert
		assertThrows(NotFoundException.class, () -> clockBuilderService.addBreak(clockTemplateId, request));
		verify(breakStructureService, never()).create(any());
	}

}

