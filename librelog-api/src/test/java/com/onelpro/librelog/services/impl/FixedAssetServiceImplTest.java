package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.FixedAssetRequestDTO;
import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.FixedAsset;
import com.onelpro.librelog.models.Station;
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
class FixedAssetServiceImplTest {

	@Mock
	private FixedAssetRepository fixedAssetRepository;

	@Mock
	private ClockTemplateRepository clockTemplateRepository;

	@InjectMocks
	private FixedAssetServiceImpl fixedAssetService;

	private ClockTemplate clockTemplate;
	private FixedAsset fixedAsset;
	private UUID clockTemplateId;
	private UUID fixedAssetId;

	@BeforeEach
	void setUp() {
		clockTemplateId = UUID.randomUUID();
		fixedAssetId = UUID.randomUUID();

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

		fixedAsset = FixedAsset.builder()
				.id(fixedAssetId)
				.clockTemplate(clockTemplate)
				.name("Legal ID")
				.assetType(AssetType.ID)
				.startTime(LocalTime.of(0, 0))
				.assetIdentifier("LEGAL_ID_001")
				.timingType(TimingType.HARD_START)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void create_When_ValidRequest_Expect_Success() {
		FixedAssetRequestDTO request = FixedAssetRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Legal ID")
				.assetType(AssetType.ID)
				.startTime(LocalTime.of(0, 0))
				.assetIdentifier("LEGAL_ID_001")
				.timingType(TimingType.HARD_START)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(fixedAsset);

		var result = fixedAssetService.create(request);

		assertNotNull(result);
		assertEquals(fixedAssetId, result.getId());
		assertEquals("Legal ID", result.getName());
		verify(fixedAssetRepository).save(any(FixedAsset.class));
	}

	@Test
	void create_When_ClockTemplateNotFound_Expect_NotFoundException() {
		FixedAssetRequestDTO request = FixedAssetRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("Legal ID")
				.assetType(AssetType.ID)
				.startTime(LocalTime.of(0, 0))
				.timingType(TimingType.HARD_START)
				.build();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> fixedAssetService.create(request));
		verify(fixedAssetRepository, never()).save(any(FixedAsset.class));
	}

	@Test
	void getById_When_Exists_Expect_Success() {
		when(fixedAssetRepository.findById(fixedAssetId)).thenReturn(Optional.of(fixedAsset));

		var result = fixedAssetService.getById(fixedAssetId);

		assertNotNull(result);
		assertEquals(fixedAssetId, result.getId());
		assertEquals("Legal ID", result.getName());
	}

	@Test
	void getById_When_NotFound_Expect_NotFoundException() {
		when(fixedAssetRepository.findById(fixedAssetId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> fixedAssetService.getById(fixedAssetId));
	}

	@Test
	void getByClockTemplateId_When_Exists_Expect_Success() {
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(true);
		when(fixedAssetRepository.findByClockTemplateId(clockTemplateId))
				.thenReturn(List.of(fixedAsset));

		var result = fixedAssetService.getByClockTemplateId(clockTemplateId);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(fixedAssetId, result.get(0).getId());
	}

	@Test
	void getByClockTemplateId_When_ClockTemplateNotFound_Expect_NotFoundException() {
		when(clockTemplateRepository.existsById(clockTemplateId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> fixedAssetService.getByClockTemplateId(clockTemplateId));
		verify(fixedAssetRepository, never()).findByClockTemplateId(eq(clockTemplateId));
	}

	@Test
	void update_When_ValidRequest_Expect_Success() {
		FixedAssetRequestDTO request = FixedAssetRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("News Intro")
				.assetType(AssetType.VT)
				.startTime(LocalTime.of(0, 5))
				.timingType(TimingType.HARD_START)
				.build();

		when(fixedAssetRepository.findById(fixedAssetId)).thenReturn(Optional.of(fixedAsset));
		when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(fixedAsset);

		var result = fixedAssetService.update(fixedAssetId, request);

		assertNotNull(result);
		verify(fixedAssetRepository).save(any(FixedAsset.class));
	}

	@Test
	void update_When_NotFound_Expect_NotFoundException() {
		FixedAssetRequestDTO request = FixedAssetRequestDTO.builder()
				.clockTemplateId(clockTemplateId)
				.name("News Intro")
				.assetType(AssetType.VT)
				.startTime(LocalTime.of(0, 5))
				.timingType(TimingType.HARD_START)
				.build();

		when(fixedAssetRepository.findById(fixedAssetId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> fixedAssetService.update(fixedAssetId, request));
		verify(fixedAssetRepository, never()).save(any(FixedAsset.class));
	}

	@Test
	void delete_When_Exists_Expect_Success() {
		when(fixedAssetRepository.existsById(fixedAssetId)).thenReturn(true);

		fixedAssetService.delete(fixedAssetId);

		verify(fixedAssetRepository).deleteById(fixedAssetId);
	}

	@Test
	void delete_When_NotFound_Expect_NotFoundException() {
		when(fixedAssetRepository.existsById(fixedAssetId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> fixedAssetService.delete(fixedAssetId));
		verify(fixedAssetRepository, never()).deleteById(fixedAssetId);
	}

}

