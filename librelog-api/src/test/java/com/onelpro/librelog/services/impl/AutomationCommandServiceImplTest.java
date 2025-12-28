package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AutomationCommandRequestDTO;
import com.onelpro.librelog.enums.AutomationCommandType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AutomationCommand;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.AutomationCommandRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationCommandServiceImplTest {

	@Mock
	private AutomationCommandRepository automationCommandRepository;

	@Mock
	private ClockTemplateRepository clockTemplateRepository;

	@InjectMocks
	private AutomationCommandServiceImpl automationCommandService;

	private ClockTemplate clockTemplate;
	private AutomationCommand automationCommand;
	private UUID clockTemplateId;
	private UUID automationCommandId;

	@BeforeEach
	void setUp() {
		clockTemplateId = UUID.randomUUID();
		automationCommandId = UUID.randomUUID();

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

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("feedId", "SAT_001");
		parameters.put("duration", 30);

		automationCommand = AutomationCommand.builder()
				.id(automationCommandId)
				.clockTemplate(clockTemplate)
				.commandType(AutomationCommandType.SWITCH_TO_SATELLITE)
				.triggerTime(LocalTime.of(0, 0))
				.priority("HIGH")
				.parameters(parameters)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void create_When_ValidRequest_Expect_Success() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("feedId", "SAT_001");

		AutomationCommandRequestDTO request = AutomationCommandRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.commandType(AutomationCommandType.SWITCH_TO_SATELLITE)
				.triggerTime(LocalTime.of(0, 0))
				.priority("HIGH")
				.parameters(parameters)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(automationCommandRepository.save(any(AutomationCommand.class))).thenReturn(automationCommand);

		var result = automationCommandService.create(request);

		assertNotNull(result);
		assertEquals(automationCommandId, result.getId());
		assertEquals(AutomationCommandType.SWITCH_TO_SATELLITE, result.getCommandType());
		verify(automationCommandRepository).save(any(AutomationCommand.class));
	}

	@Test
	void create_When_ClockTemplateNotFound_Expect_NotFoundException() {
		AutomationCommandRequestDTO request = AutomationCommandRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.commandType(AutomationCommandType.SWITCH_TO_SATELLITE)
				.triggerTime(LocalTime.of(0, 0))
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> automationCommandService.create(request));
		verify(automationCommandRepository, never()).save(any(AutomationCommand.class));
	}

	@Test
	void getById_When_Exists_Expect_Success() {
		when(automationCommandRepository.findById(automationCommandId)).thenReturn(Optional.of(automationCommand));

		var result = automationCommandService.getById(automationCommandId);

		assertNotNull(result);
		assertEquals(automationCommandId, result.getId());
		assertEquals(AutomationCommandType.SWITCH_TO_SATELLITE, result.getCommandType());
	}

	@Test
	void getById_When_NotFound_Expect_NotFoundException() {
		when(automationCommandRepository.findById(automationCommandId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> automationCommandService.getById(automationCommandId));
	}

	@Test
	void getByClockTemplateId_When_Exists_Expect_Success() {
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(automationCommandRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(automationCommand));

		var result = automationCommandService.getByClockTemplateId(clockTemplateId);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(automationCommandId, result.get(0).getId());
	}

	@Test
	void getByClockTemplateId_When_ClockTemplateNotFound_Expect_NotFoundException() {
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> automationCommandService.getByClockTemplateId(clockTemplateId));
		verify(automationCommandRepository, never()).findByClockTemplateId(eq(clockTemplateId));
	}

	@Test
	void update_When_ValidRequest_Expect_Success() {
		Map<String, Object> newParameters = new HashMap<>();
		newParameters.put("feedId", "NET_002");

		AutomationCommandRequestDTO request = AutomationCommandRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.commandType(AutomationCommandType.SWITCH_TO_NETWORK)
				.triggerTime(LocalTime.of(0, 30))
				.priority("MEDIUM")
				.parameters(newParameters)
				.build();

		when(automationCommandRepository.findById(automationCommandId)).thenReturn(Optional.of(automationCommand));
		when(automationCommandRepository.save(any(AutomationCommand.class))).thenReturn(automationCommand);

		var result = automationCommandService.update(automationCommandId, request);

		assertNotNull(result);
		verify(automationCommandRepository).save(any(AutomationCommand.class));
	}

	@Test
	void update_When_NotFound_Expect_NotFoundException() {
		AutomationCommandRequestDTO request = AutomationCommandRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.commandType(AutomationCommandType.SWITCH_TO_NETWORK)
				.triggerTime(LocalTime.of(0, 30))
				.build();

		when(automationCommandRepository.findById(automationCommandId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> automationCommandService.update(automationCommandId, request));
		verify(automationCommandRepository, never()).save(any(AutomationCommand.class));
	}

	@Test
	void delete_When_Exists_Expect_Success() {
		when(automationCommandRepository.existsById(automationCommandId)).thenReturn(true);

		automationCommandService.delete(automationCommandId);

		verify(automationCommandRepository).deleteById(automationCommandId);
	}

	@Test
	void delete_When_NotFound_Expect_NotFoundException() {
		when(automationCommandRepository.existsById(automationCommandId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> automationCommandService.delete(automationCommandId));
		verify(automationCommandRepository, never()).deleteById(automationCommandId);
	}

}

