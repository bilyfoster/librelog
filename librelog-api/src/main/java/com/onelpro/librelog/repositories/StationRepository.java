package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Station entity operations.
 */
@Repository
public interface StationRepository extends JpaRepository<Station, UUID> {

	Optional<Station> findByCallSign(String callSign);

	boolean existsByCallSign(String callSign);

}

