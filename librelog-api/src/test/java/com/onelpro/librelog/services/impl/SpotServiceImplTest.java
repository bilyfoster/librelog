package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.SpotRequestDTO;
import com.onelpro.librelog.dto.SpotResponseDTO;
import com.onelpro.librelog.enums.SpotStatus;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Campaign;
import com.onelpro.librelog.models.Spot;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.SpotRepository;
import com.onelpro.librelog.repositories.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotServiceImplTest {

	@Mock
	private SpotRepository spotRepository;

	@Mock
	private CampaignRepository campaignRepository;

	@Mock
	private StationRepository stationRepository;

	@InjectMocks
	private SpotServiceImpl spotService;

	private UUID campaignId;
	private UUID stationId;
	private Campaign campaign;
	private Station station;
	private Spot spot;
	private SpotRequestDTO requestDTO;

	@BeforeEach
	void setUp() {
		campaignId = UUID.randomUUID();
		stationId = UUID.randomUUID();

		station = new Station();
		station.setId(stationId);
		station.setName("Test Station");

		campaign = new Campaign();
		campaign.setId(campaignId);
		campaign.setName("Test Campaign");
		campaign.setStation(station);

		spot = Spot.builder()
				.id(UUID.randomUUID())
				.campaign(campaign)
				.station(station)
				.scheduledDate(LocalDate.now())
				.scheduledTime("10:00:00")
				.spotLength(30)
				.status(SpotStatus.SCHEDULED)
				.build();

		requestDTO = SpotRequestDTO.builder()
				.campaignId(campaignId)
				.stationId(stationId)
				.scheduledDate(LocalDate.now())
				.scheduledTime("10:00:00")
				.spotLength(30)
				.build();
	}

	@Test
	void create_Success() {
		when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
		when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
		when(spotRepository.save(any(Spot.class))).thenReturn(spot);
		when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

		SpotResponseDTO result = spotService.create(requestDTO);

		assertNotNull(result);
		assertEquals("10:00:00", result.getScheduledTime());
		assertEquals(30, result.getSpotLength());
		verify(spotRepository).save(any(Spot.class));
	}

	@Test
	void create_CampaignNotFound_ThrowsException() {
		when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> spotService.create(requestDTO));
	}

	@Test
	void getById_Success() {
		when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));

		SpotResponseDTO result = spotService.getById(spot.getId());

		assertNotNull(result);
		assertEquals(spot.getId(), result.getId());
		assertEquals(SpotStatus.SCHEDULED, result.getStatus());
	}

	@Test
	void markAsAired_Success() {
		when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
		when(spotRepository.save(any(Spot.class))).thenReturn(spot);
		when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

		SpotResponseDTO result = spotService.markAsAired(spot.getId());

		assertNotNull(result);
		assertEquals(SpotStatus.AIRED, result.getStatus());
		assertNotNull(result.getActualAirTime());
	}

	@Test
	void createMakegood_Success() {
		Spot missedSpot = Spot.builder()
				.id(UUID.randomUUID())
				.campaign(campaign)
				.station(station)
				.scheduledDate(LocalDate.now().minusDays(1))
				.scheduledTime("10:00:00")
				.spotLength(30)
				.status(SpotStatus.MISSED)
				.build();

		Spot makegoodSpot = Spot.builder()
				.id(UUID.randomUUID())
				.campaign(campaign)
				.station(station)
				.scheduledDate(LocalDate.now())
				.scheduledTime("14:00:00")
				.spotLength(30)
				.status(SpotStatus.MAKEGOOD_SCHEDULED)
				.build();

		when(spotRepository.findById(missedSpot.getId())).thenReturn(Optional.of(missedSpot));
		when(spotRepository.save(any(Spot.class))).thenReturn(makegoodSpot);
		when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

		SpotRequestDTO makegoodRequest = SpotRequestDTO.builder()
				.campaignId(campaignId)
				.stationId(stationId)
				.scheduledDate(LocalDate.now())
				.scheduledTime("14:00:00")
				.spotLength(30)
				.build();

		SpotResponseDTO result = spotService.createMakegood(missedSpot.getId(), makegoodRequest);

		assertNotNull(result);
		assertEquals(SpotStatus.MAKEGOOD_SCHEDULED, result.getStatus());
	}

	@Test
	void getByCampaignId_Success() {
		when(spotRepository.findByCampaignId(campaignId)).thenReturn(Collections.singletonList(spot));

		var results = spotService.getByCampaignId(campaignId);

		assertNotNull(results);
		assertEquals(1, results.size());
	}

	@Test
	void delete_Success() {
		when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
		doNothing().when(spotRepository).deleteById(spot.getId());

		assertDoesNotThrow(() -> spotService.delete(spot.getId()));
		verify(spotRepository).deleteById(spot.getId());
	}

}
