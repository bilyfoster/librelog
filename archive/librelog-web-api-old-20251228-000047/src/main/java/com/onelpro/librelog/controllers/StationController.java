package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;
import com.onelpro.librelog.services.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for station management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/stations")
@Tag(name = "Stations", description = "Station management API")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @PostMapping
    @Operation(summary = "Create a new station", description = "Creates a new station with the provided information")
    public ResponseEntity<StationResponseDTO> createStation(@Valid @RequestBody StationRequestDTO stationRequestDTO) {
        log.info("Creating station with call letters: {}", stationRequestDTO.getCallLetters());
        StationResponseDTO createdStation = stationService.createStation(stationRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get station by ID", description = "Retrieves a station by its unique identifier")
    public ResponseEntity<StationResponseDTO> getStationById(@PathVariable UUID id) {
        log.debug("Retrieving station with ID: {}", id);
        StationResponseDTO station = stationService.getStationById(id);
        return ResponseEntity.ok(station);
    }

    @GetMapping("/call-letters/{callLetters}")
    @Operation(summary = "Get station by call letters", description = "Retrieves a station by its call letters")
    public ResponseEntity<StationResponseDTO> getStationByCallLetters(@PathVariable String callLetters) {
        log.debug("Retrieving station with call letters: {}", callLetters);
        StationResponseDTO station = stationService.getStationByCallLetters(callLetters);
        return ResponseEntity.ok(station);
    }

    @GetMapping
    @Operation(summary = "Get all stations", description = "Retrieves a list of all stations in the system")
    public ResponseEntity<List<StationResponseDTO>> getAllStations() {
        log.debug("Retrieving all stations");
        List<StationResponseDTO> stations = stationService.getAllStations();
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active stations", description = "Retrieves a list of all active stations")
    public ResponseEntity<List<StationResponseDTO>> getActiveStations() {
        log.debug("Retrieving active stations");
        List<StationResponseDTO> stations = stationService.getActiveStations();
        return ResponseEntity.ok(stations);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update station", description = "Updates an existing station's information")
    public ResponseEntity<StationResponseDTO> updateStation(
            @PathVariable UUID id,
            @Valid @RequestBody StationRequestDTO stationRequestDTO) {
        log.info("Updating station with ID: {}", id);
        StationResponseDTO updatedStation = stationService.updateStation(id, stationRequestDTO);
        return ResponseEntity.ok(updatedStation);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete station", description = "Deletes a station by its unique identifier")
    public ResponseEntity<Void> deleteStation(@PathVariable UUID id) {
        log.info("Deleting station with ID: {}", id);
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}

