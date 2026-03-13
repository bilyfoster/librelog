package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.CampaignRequestDTO;
import com.onelpro.librelog.dto.CampaignResponseDTO;
import com.onelpro.librelog.enums.CampaignStatus;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Campaign;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

	@Mock
	private CampaignRepository campaignRepository;

	@Mock
	private StationRepository stationRepository;

	@InjectMocks
	private CampaignServiceImpl campaignService;

	private UUID stationId;
	private Station station;
	private Campaign campaign;
	private CampaignRequestDTO requestDTO;

	@BeforeEach
	void setUp() {
		stationId = UUID.randomUUID();
		station = new Station();
		station.setId(stationId);
		station.setName("Test Station");

		campaign = Campaign.builder()
				.id(UUID.randomUUID())
				.name("Test Campaign")
				.station(station)
				.advertiserName("Test Advertiser")
				.startDate(LocalDate.now())
				.endDate(LocalDate.now().plusDays(30))
				.status(CampaignStatus.ACTIVE)
				.totalSpots(10)
				.build();

		requestDTO = CampaignRequestDTO.builder()
				.name("Test Campaign")
				.stationId(stationId)
				.advertiserName("Test Advertiser")
				.startDate(LocalDate.now())
				.endDate(LocalDate.now().plusDays(30))
				.budget(new BigDecimal("5000.00"))
				.build();
	}

	@Test
	void create_Success() {
		when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
		when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

		CampaignResponseDTO result = campaignService.create(requestDTO);

		assertNotNull(result);
		assertEquals("Test Campaign", result.getName());
		assertEquals("Test Advertiser", result.getAdvertiserName());
		verify(campaignRepository).save(any(Campaign.class));
	}

	@Test
	void create_StationNotFound_ThrowsException() {
		when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> campaignService.create(requestDTO));
	}

	@Test
	void getById_Success() {
		when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));

		CampaignResponseDTO result = campaignService.getById(campaign.getId());

		assertNotNull(result);
		assertEquals(campaign.getId(), result.getId());
		assertEquals("Test Campaign", result.getName());
	}

	@Test
	void getById_NotFound_ThrowsException() {
		UUID randomId = UUID.randomUUID();
		when(campaignRepository.findById(randomId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> campaignService.getById(randomId));
	}

	@Test
	void getByStationId_Success() {
		when(campaignRepository.findByStationId(stationId)).thenReturn(Collections.singletonList(campaign));

		var results = campaignService.getByStationId(stationId);

		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Test Campaign", results.get(0).getName());
	}

	@Test
	void updateStatus_Success() {
		when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));
		when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

		CampaignResponseDTO result = campaignService.updateStatus(campaign.getId(), "PAUSED");

		assertNotNull(result);
		verify(campaignRepository).save(any(Campaign.class));
	}

	@Test
	void delete_Success() {
		when(campaignRepository.existsById(campaign.getId())).thenReturn(true);
		doNothing().when(campaignRepository).deleteById(campaign.getId());

		assertDoesNotThrow(() -> campaignService.delete(campaign.getId()));
		verify(campaignRepository).deleteById(campaign.getId());
	}

	@Test
	void delete_NotFound_ThrowsException() {
		UUID randomId = UUID.randomUUID();
		when(campaignRepository.existsById(randomId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> campaignService.delete(randomId));
	}

}
