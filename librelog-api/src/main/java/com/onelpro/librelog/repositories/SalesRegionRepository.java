package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.SalesRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SalesRegion entity operations.
 */
@Repository
public interface SalesRegionRepository extends JpaRepository<SalesRegion, UUID> {

	Optional<SalesRegion> findByName(String name);

}

