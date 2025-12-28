package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.BreakStructureRequestDTO;
import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.enums.TransitionCode;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AvailType;
import com.onelpro.librelog.models.BreakStructure;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.AvailTypeRepository;
import com.onelpro.librelog.repositories.BreakStructureRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
class BreakStructureServiceImplTest {

	@Mock
	private BreakStructureRepository breakStructureRepository;

	@Mock
	private ClockTemplateRepository clockTemplateRepository;

	@Mock
	private AvailTypeRepository availTypeRepository;

	@InjectMocks
	private BreakStructureServiceImpl breakStructureService;

	private ClockTemplate clockTemplate;
	private AvailType availType;
	private BreakStructure breakStructure;
	private UUID clockTemplateId;
	private UUID availTypeId;
	private UUID breakStructureId;

	@BeforeEach
	void setUp() {
		clockTemplateId = UUID.randomUUID();
		availTypeId = UUID.randomUUID();
		breakStructureId = UUID.randomUUID();

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

		availType = AvailType.builder()
				.id(availTypeId)
				.name("General")
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.build();

		breakStructure = BreakStructure.builder()
				.id(breakStructureId)
				.clockTemplate(clockTemplate)
				.name("Break A")
				.startTime(LocalTime.of(15, 0))
				.durationSeconds(180)
				.isFloating(false)
				.availType(availType)
				.timingType(TimingType.HARD_START)
				.transitionCode(TransitionCode.SEGUE)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void create_When_ValidRequest_Expect_Success() {
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Break A")
				.startTime(LocalTime.of(15, 0))
				.durationSeconds(180)
				.isFloating(false)
				.availTypeId(availTypeId)
				.timingType(TimingType.HARD_START)
				.transitionCode(TransitionCode.SEGUE)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.of(availType));
		when(breakStructureRepository.save(any(BreakStructure.class))).thenReturn(breakStructure);

		var result = breakStructureService.create(request);

		assertNotNull(result);
		assertEquals(breakStructureId, result.getId());
		assertEquals("Break A", result.getName());
		verify(breakStructureRepository).save(any(BreakStructure.class));
	}

	@Test
	void create_When_ClockTemplateNotFound_Expect_NotFoundException() {
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Break A")
				.startTime(LocalTime.of(15, 0))
				.durationSeconds(180)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> breakStructureService.create(request));
		verify(breakStructureRepository, never()).save(any(BreakStructure.class));
	}

	@Test
	void create_When_AvailTypeNotFound_Expect_NotFoundException() {
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Break A")
				.startTime(LocalTime.of(15, 0))
				.durationSeconds(180)
				.availTypeId(availTypeId)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> breakStructureService.create(request));
		verify(breakStructureRepository, never()).save(any(BreakStructure.class));
	}

	@Test
	void create_When_Exceeds60Minutes_Expect_BadRequestException() {
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Break A")
				.startTime(LocalTime.of(0, 50)) // 50 minutes into the hour
				.durationSeconds(660) // 11 minutes, would exceed 60 minutes (50 + 11 = 61)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));

		assertThrows(BadRequestException.class, () -> breakStructureService.create(request));
		verify(breakStructureRepository, never()).save(any(BreakStructure.class));
	}

	@Test
	void getById_When_Exists_Expect_Success() {
		when(breakStructureRepository.findById(breakStructureId)).thenReturn(Optional.of(breakStructure));

		var result = breakStructureService.getById(breakStructureId);

		assertNotNull(result);
		assertEquals(breakStructureId, result.getId());
		assertEquals("Break A", result.getName());
	}

	@Test
	void getById_When_NotFound_Expect_NotFoundException() {
		when(breakStructureRepository.findById(breakStructureId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> breakStructureService.getById(breakStructureId));
	}

	@Test
	void getByClockTemplateId_When_Exists_Expect_Success() {
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(breakStructureRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(breakStructure));

		var result = breakStructureService.getByClockTemplateId(clockTemplateId);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(breakStructureId, result.get(0).getId());
	}

	@Test
	void getByClockTemplateId_When_ClockTemplateNotFound_Expect_NotFoundException() {
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> breakStructureService.getByClockTemplateId(clockTemplateId));
		verify(breakStructureRepository, never()).findByClockTemplateId(eq(clockTemplateId));
	}

	@Test
	void update_When_ValidRequest_Expect_Success() {
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Break B")
				.startTime(LocalTime.of(0, 20)) // 20 minutes into the hour
				.durationSeconds(240) // 4 minutes, total 24 minutes (within 60)
				.isFloating(true)
				.build();

		when(breakStructureRepository.findById(breakStructureId)).thenReturn(Optional.of(breakStructure));
		when(breakStructureRepository.save(any(BreakStructure.class))).thenReturn(breakStructure);

		var result = breakStructureService.update(breakStructureId, request);

		assertNotNull(result);
		verify(breakStructureRepository).save(any(BreakStructure.class));
	}

	@Test
	void update_When_NotFound_Expect_NotFoundException() {
		BreakStructureRequestDTO request = BreakStructureRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Break B")
				.startTime(LocalTime.of(20, 0))
				.durationSeconds(240)
				.build();

		when(breakStructureRepository.findById(breakStructureId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> breakStructureService.update(breakStructureId, request));
		verify(breakStructureRepository, never()).save(any(BreakStructure.class));
	}

	@Test
	void delete_When_Exists_Expect_Success() {
		when(breakStructureRepository.existsById(breakStructureId)).thenReturn(true);

		breakStructureService.delete(breakStructureId);

		verify(breakStructureRepository).deleteById(breakStructureId);
	}

	@Test
	void delete_When_NotFound_Expect_NotFoundException() {
		when(breakStructureRepository.existsById(breakStructureId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> breakStructureService.delete(breakStructureId));
		verify(breakStructureRepository, never()).deleteById(breakStructureId);
	}

}

