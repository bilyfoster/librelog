package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.SalesRep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SalesRep entity operations.
 */
@Repository
public interface SalesRepRepository extends JpaRepository<SalesRep, UUID> {

	Optional<SalesRep> findByEmail(String email);

	boolean existsByEmail(String email);

	List<SalesRep> findBySalesTeamId(UUID salesTeamId);

	List<SalesRep> findBySalesOfficeId(UUID salesOfficeId);

	List<SalesRep> findBySalesRegionId(UUID salesRegionId);

	List<SalesRep> findByIsActive(Boolean isActive);

}

