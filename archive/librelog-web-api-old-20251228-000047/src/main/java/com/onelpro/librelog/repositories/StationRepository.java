package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Station entity.
 * Provides data access operations for station management.
 */
@Repository
public interface StationRepository extends JpaRepository<Station, UUID> {

    /**
     * Finds a station by call letters.
     *
     * @param callLetters the call letters to search for
     * @return Optional containing the station if found, empty otherwise
     */
    Optional<Station> findByCallLetters(String callLetters);

    /**
     * Checks if a station exists with the given call letters.
     *
     * @param callLetters the call letters to check
     * @return true if a station exists with the call letters, false otherwise
     */
    boolean existsByCallLetters(String callLetters);

    /**
     * Finds all active stations.
     *
     * @return list of active stations
     */
    java.util.List<Station> findByActiveTrue();
}

