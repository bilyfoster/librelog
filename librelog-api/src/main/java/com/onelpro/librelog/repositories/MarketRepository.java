package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Market entity operations.
 */
@Repository
public interface MarketRepository extends JpaRepository<Market, UUID> {

	List<Market> findByOrganizationId(UUID organizationId);

	List<Market> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);

}

