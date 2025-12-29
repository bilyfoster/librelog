package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for station management operations.
 */
public interface StationService {

    /**
     * Creates a new station.
     *
     * @param stationRequestDTO the station creation request
     * @return the created station response
     * @throws IllegalArgumentException if call letters already exist
     */
    StationResponseDTO createStation(StationRequestDTO stationRequestDTO);

    /**
     * Retrieves a station by ID.
     *
     * @param id the station ID
     * @return the station response
     * @throws jakarta.persistence.EntityNotFoundException if station not found
     */
    StationResponseDTO getStationById(UUID id);

    /**
     * Retrieves a station by call letters.
     *
     * @param callLetters the call letters
     * @return the station response
     * @throws jakarta.persistence.EntityNotFoundException if station not found
     */
    StationResponseDTO getStationByCallLetters(String callLetters);

    /**
     * Retrieves all stations.
     *
     * @return list of all station responses
     */
    List<StationResponseDTO> getAllStations();

    /**
     * Retrieves all active stations.
     *
     * @return list of active station responses
     */
    List<StationResponseDTO> getActiveStations();

    /**
     * Updates an existing station.
     *
     * @param id the station ID
     * @param stationRequestDTO the station update request
     * @return the updated station response
     * @throws jakarta.persistence.EntityNotFoundException if station not found
     * @throws IllegalArgumentException if call letters already exist (when changing call letters)
     */
    StationResponseDTO updateStation(UUID id, StationRequestDTO stationRequestDTO);

    /**
     * Deletes a station by ID.
     *
     * @param id the station ID
     * @throws jakarta.persistence.EntityNotFoundException if station not found
     */
    void deleteStation(UUID id);
}

