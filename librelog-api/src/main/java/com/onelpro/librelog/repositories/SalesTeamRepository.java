package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.SalesTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SalesTeam entity operations.
 */
@Repository
public interface SalesTeamRepository extends JpaRepository<SalesTeam, UUID> {

	Optional<SalesTeam> findByName(String name);

}

