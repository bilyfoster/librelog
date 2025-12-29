package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;
import com.onelpro.librelog.enums.StationType;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Organization;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.ClusterRepository;
import com.onelpro.librelog.repositories.MarketRepository;
import com.onelpro.librelog.repositories.OrganizationRepository;
import com.onelpro.librelog.repositories.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceImplTest {

	@Mock
	private StationRepository stationRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private MarketRepository marketRepository;

	@Mock
	private ClusterRepository clusterRepository;

	@InjectMocks
	private StationServiceImpl stationService;

	private StationRequestDTO requestDTO;
	private Station testStation;
	private Organization testOrganization;

	@BeforeEach
	void setUp() {
		UUID orgId = UUID.randomUUID();
		testOrganization = Organization.builder()
				.id(orgId)
				.name("Test Organization")
				.isActive(true)
				.build();

		requestDTO = StationRequestDTO.builder()
				.organizationId(orgId)
				.callSign("KABC")
				.name("ABC Radio")
				.frequency("101.1 FM")
				.stationType(StationType.FM)
				.build();

		testStation = Station.builder()
				.id(UUID.randomUUID())
				.organization(testOrganization)
				.callSign("KABC")
				.name("ABC Radio")
				.frequency("101.1 FM")
				.stationType(StationType.FM)
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void create_When_ValidRequest_Expect_Success() {
		when(stationRepository.existsByCallSign(requestDTO.getCallSign())).thenReturn(false);
		when(organizationRepository.findById(requestDTO.getOrganizationId())).thenReturn(Optional.of(testOrganization));
		when(stationRepository.save(any(Station.class))).thenReturn(testStation);

		StationResponseDTO response = stationService.create(requestDTO);

		assertNotNull(response);
		assertEquals(testStation.getId(), response.getId());
		assertEquals(testStation.getCallSign(), response.getCallSign());
		assertEquals(testStation.getName(), response.getName());
		verify(stationRepository).save(any(Station.class));
	}

	@Test
	void create_When_CallSignExists_Expect_BadRequestException() {
		when(stationRepository.existsByCallSign(requestDTO.getCallSign())).thenReturn(true);

		assertThrows(BadRequestException.class, () -> stationService.create(requestDTO));
		verify(stationRepository, never()).save(any(Station.class));
		verify(organizationRepository, never()).findById(any(UUID.class));
	}

	@Test
	void getById_When_StationExists_Expect_Success() {
		when(stationRepository.findById(testStation.getId())).thenReturn(Optional.of(testStation));

		StationResponseDTO response = stationService.getById(testStation.getId());

		assertNotNull(response);
		assertEquals(testStation.getId(), response.getId());
	}

	@Test
	void getById_When_StationNotFound_Expect_NotFoundException() {
		UUID nonExistentId = UUID.randomUUID();
		when(stationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> stationService.getById(nonExistentId));
	}

	@Test
	void getAll_When_StationsExist_Expect_Success() {
		Station station2 = Station.builder()
				.id(UUID.randomUUID())
				.organization(testOrganization)
				.callSign("KXYZ")
				.name("XYZ Radio")
				.stationType(StationType.AM)
				.isActive(true)
				.build();
		when(stationRepository.findAll()).thenReturn(List.of(testStation, station2));

		List<StationResponseDTO> response = stationService.getAll();

		assertNotNull(response);
		assertEquals(2, response.size());
	}

	@Test
	void update_When_ValidRequest_Expect_Success() {
		StationRequestDTO updateRequest = StationRequestDTO.builder()
				.organizationId(testOrganization.getId())
				.callSign("KABC-UPDATED")
				.name("ABC Radio Updated")
				.stationType(StationType.FM)
				.build();
		when(stationRepository.findById(testStation.getId())).thenReturn(Optional.of(testStation));
		when(stationRepository.existsByCallSign(updateRequest.getCallSign())).thenReturn(false);
		when(organizationRepository.findById(updateRequest.getOrganizationId())).thenReturn(Optional.of(testOrganization));
		when(stationRepository.save(any(Station.class))).thenReturn(testStation);

		StationResponseDTO response = stationService.update(testStation.getId(), updateRequest);

		assertNotNull(response);
		verify(stationRepository).save(any(Station.class));
	}

	@Test
	void update_When_StationNotFound_Expect_NotFoundException() {
		UUID nonExistentId = UUID.randomUUID();
		when(stationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> stationService.update(nonExistentId, requestDTO));
		verify(stationRepository, never()).save(any(Station.class));
	}

	@Test
	void delete_When_StationExists_Expect_Success() {
		when(stationRepository.existsById(testStation.getId())).thenReturn(true);

		stationService.delete(testStation.getId());

		verify(stationRepository).deleteById(testStation.getId());
	}

	@Test
	void delete_When_StationNotFound_Expect_NotFoundException() {
		UUID nonExistentId = UUID.randomUUID();
		when(stationRepository.existsById(nonExistentId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> stationService.delete(nonExistentId));
		verify(stationRepository, never()).deleteById(any(UUID.class));
	}

}

