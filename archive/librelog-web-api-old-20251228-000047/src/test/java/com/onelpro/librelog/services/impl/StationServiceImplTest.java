package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.StationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class StationServiceImplTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationServiceImpl stationService;

    private StationRequestDTO stationRequestDTO;
    private Station station;
    private UUID stationId;

    @BeforeEach
    void setUp() {
        stationId = UUID.randomUUID();
        stationRequestDTO = StationRequestDTO.builder()
                .callLetters("KPHX")
                .frequency("98.7 FM")
                .market("Phoenix")
                .format("Top 40")
                .ownership("GayPHX Media")
                .inventoryClass("A")
                .active(true)
                .build();

        station = Station.builder()
                .id(stationId)
                .callLetters("KPHX")
                .frequency("98.7 FM")
                .market("Phoenix")
                .format("Top 40")
                .ownership("GayPHX Media")
                .inventoryClass("A")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createStation_When_ValidRequest_Expect_StationCreated() {
        when(stationRepository.existsByCallLetters(anyString())).thenReturn(false);
        when(stationRepository.save(any(Station.class))).thenReturn(station);

        StationResponseDTO result = stationService.createStation(stationRequestDTO);

        assertNotNull(result);
        assertEquals(stationId, result.getId());
        assertEquals("KPHX", result.getCallLetters());
        assertEquals("98.7 FM", result.getFrequency());
        assertEquals("Phoenix", result.getMarket());
        verify(stationRepository, times(1)).existsByCallLetters("KPHX");
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    void createStation_When_CallLettersExist_Expect_IllegalArgumentException() {
        when(stationRepository.existsByCallLetters(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> stationService.createStation(stationRequestDTO));
        verify(stationRepository, times(1)).existsByCallLetters("KPHX");
        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    void getStationById_When_StationExists_Expect_StationReturned() {
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));

        StationResponseDTO result = stationService.getStationById(stationId);

        assertNotNull(result);
        assertEquals(stationId, result.getId());
        assertEquals("KPHX", result.getCallLetters());
        verify(stationRepository, times(1)).findById(stationId);
    }

    @Test
    void getStationById_When_StationNotFound_Expect_EntityNotFoundException() {
        when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stationService.getStationById(stationId));
        verify(stationRepository, times(1)).findById(stationId);
    }

    @Test
    void getStationByCallLetters_When_StationExists_Expect_StationReturned() {
        when(stationRepository.findByCallLetters("KPHX")).thenReturn(Optional.of(station));

        StationResponseDTO result = stationService.getStationByCallLetters("KPHX");

        assertNotNull(result);
        assertEquals("KPHX", result.getCallLetters());
        verify(stationRepository, times(1)).findByCallLetters("KPHX");
    }

    @Test
    void getStationByCallLetters_When_StationNotFound_Expect_EntityNotFoundException() {
        when(stationRepository.findByCallLetters("KPHX")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stationService.getStationByCallLetters("KPHX"));
        verify(stationRepository, times(1)).findByCallLetters("KPHX");
    }

    @Test
    void getAllStations_When_StationsExist_Expect_ListReturned() {
        Station station2 = Station.builder()
                .id(UUID.randomUUID())
                .callLetters("KABC")
                .frequency("101.1 FM")
                .market("Los Angeles")
                .format("Rock")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(stationRepository.findAll()).thenReturn(List.of(station, station2));

        List<StationResponseDTO> result = stationService.getAllStations();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(stationRepository, times(1)).findAll();
    }

    @Test
    void getActiveStations_When_ActiveStationsExist_Expect_ListReturned() {
        when(stationRepository.findByActiveTrue()).thenReturn(List.of(station));

        List<StationResponseDTO> result = stationService.getActiveStations();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("KPHX", result.get(0).getCallLetters());
        verify(stationRepository, times(1)).findByActiveTrue();
    }

    @Test
    void updateStation_When_ValidRequest_Expect_StationUpdated() {
        StationRequestDTO updateRequest = StationRequestDTO.builder()
                .callLetters("KPHX")
                .frequency("98.7 FM")
                .market("Phoenix Metro")
                .format("Contemporary Hit Radio")
                .active(false)
                .build();

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenReturn(station);

        StationResponseDTO result = stationService.updateStation(stationId, updateRequest);

        assertNotNull(result);
        verify(stationRepository, times(1)).findById(stationId);
        verify(stationRepository, never()).existsByCallLetters(anyString()); // Same call letters, no need to check
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    void updateStation_When_CallLettersAlreadyExist_Expect_IllegalArgumentException() {
        StationRequestDTO updateRequest = StationRequestDTO.builder()
                .callLetters("KABC")
                .frequency("101.1 FM")
                .market("Los Angeles")
                .active(true)
                .build();

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
        when(stationRepository.existsByCallLetters("KABC")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> stationService.updateStation(stationId, updateRequest));
        verify(stationRepository, times(1)).findById(stationId);
        verify(stationRepository, times(1)).existsByCallLetters("KABC");
        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    void updateStation_When_StationNotFound_Expect_EntityNotFoundException() {
        when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stationService.updateStation(stationId, stationRequestDTO));
        verify(stationRepository, times(1)).findById(stationId);
        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    void deleteStation_When_StationExists_Expect_StationDeleted() {
        when(stationRepository.existsById(stationId)).thenReturn(true);

        stationService.deleteStation(stationId);

        verify(stationRepository, times(1)).existsById(stationId);
        verify(stationRepository, times(1)).deleteById(stationId);
    }

    @Test
    void deleteStation_When_StationNotFound_Expect_EntityNotFoundException() {
        when(stationRepository.existsById(stationId)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> stationService.deleteStation(stationId));
        verify(stationRepository, times(1)).existsById(stationId);
        verify(stationRepository, never()).deleteById(any(UUID.class));
    }
}

