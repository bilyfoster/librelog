package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.StationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for station management operations.
 */
@Slf4j
@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;

    public StationServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    @Transactional
    public StationResponseDTO createStation(StationRequestDTO stationRequestDTO) {
        log.debug("Creating station with call letters: {}", stationRequestDTO.getCallLetters());

        if (stationRepository.existsByCallLetters(stationRequestDTO.getCallLetters())) {
            throw new IllegalArgumentException("Call letters already exist: " + stationRequestDTO.getCallLetters());
        }

        Station station = Station.builder()
                .callLetters(stationRequestDTO.getCallLetters())
                .frequency(stationRequestDTO.getFrequency())
                .market(stationRequestDTO.getMarket())
                .format(stationRequestDTO.getFormat())
                .ownership(stationRequestDTO.getOwnership())
                .contacts(stationRequestDTO.getContacts())
                .rates(stationRequestDTO.getRates())
                .inventoryClass(stationRequestDTO.getInventoryClass())
                .active(stationRequestDTO.getActive())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Station savedStation = stationRepository.save(station);
        log.info("Station created successfully with ID: {}", savedStation.getId());

        return mapToResponseDTO(savedStation);
    }

    @Override
    @Transactional(readOnly = true)
    public StationResponseDTO getStationById(UUID id) {
        log.debug("Retrieving station by ID: {}", id);
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Station not found with ID: " + id));
        return mapToResponseDTO(station);
    }

    @Override
    @Transactional(readOnly = true)
    public StationResponseDTO getStationByCallLetters(String callLetters) {
        log.debug("Retrieving station by call letters: {}", callLetters);
        Station station = stationRepository.findByCallLetters(callLetters)
                .orElseThrow(() -> new EntityNotFoundException("Station not found with call letters: " + callLetters));
        return mapToResponseDTO(station);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponseDTO> getAllStations() {
        log.debug("Retrieving all stations");
        return stationRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponseDTO> getActiveStations() {
        log.debug("Retrieving active stations");
        return stationRepository.findByActiveTrue().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StationResponseDTO updateStation(UUID id, StationRequestDTO stationRequestDTO) {
        log.debug("Updating station with ID: {}", id);
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Station not found with ID: " + id));

        // Check if call letters are being changed and if they already exist
        if (!station.getCallLetters().equals(stationRequestDTO.getCallLetters()) &&
                stationRepository.existsByCallLetters(stationRequestDTO.getCallLetters())) {
            throw new IllegalArgumentException("Call letters already exist: " + stationRequestDTO.getCallLetters());
        }

        station.setCallLetters(stationRequestDTO.getCallLetters());
        station.setFrequency(stationRequestDTO.getFrequency());
        station.setMarket(stationRequestDTO.getMarket());
        station.setFormat(stationRequestDTO.getFormat());
        station.setOwnership(stationRequestDTO.getOwnership());
        station.setContacts(stationRequestDTO.getContacts());
        station.setRates(stationRequestDTO.getRates());
        station.setInventoryClass(stationRequestDTO.getInventoryClass());
        station.setActive(stationRequestDTO.getActive());
        station.setUpdatedAt(Instant.now());

        Station updatedStation = stationRepository.save(station);
        log.info("Station updated successfully with ID: {}", updatedStation.getId());

        return mapToResponseDTO(updatedStation);
    }

    @Override
    @Transactional
    public void deleteStation(UUID id) {
        log.debug("Deleting station with ID: {}", id);
        if (!stationRepository.existsById(id)) {
            throw new EntityNotFoundException("Station not found with ID: " + id);
        }
        stationRepository.deleteById(id);
        log.info("Station deleted successfully with ID: {}", id);
    }

    /**
     * Maps a Station entity to a StationResponseDTO.
     *
     * @param station the station entity
     * @return the station response DTO
     */
    private StationResponseDTO mapToResponseDTO(Station station) {
        return StationResponseDTO.builder()
                .id(station.getId())
                .callLetters(station.getCallLetters())
                .frequency(station.getFrequency())
                .market(station.getMarket())
                .format(station.getFormat())
                .ownership(station.getOwnership())
                .contacts(station.getContacts())
                .rates(station.getRates())
                .inventoryClass(station.getInventoryClass())
                .active(station.getActive())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }
}

