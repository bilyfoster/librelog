package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.LibreTimeIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for LibreTimeIntegrationConfig entity operations.
 */
@Repository
public interface LibreTimeIntegrationConfigRepository extends JpaRepository<LibreTimeIntegrationConfig, UUID> {

	/**
	 * Finds the integration configuration for a specific station.
	 * 
	 * @param stationId The station ID
	 * @return Optional containing the configuration if found
	 */
	Optional<LibreTimeIntegrationConfig> findByStationId(UUID stationId);

	/**
	 * Finds all configurations where sync is enabled.
	 * 
	 * @return List of configurations with sync enabled
	 */
	List<LibreTimeIntegrationConfig> findBySyncEnabledTrue();

}

