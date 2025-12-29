package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Agency entity operations.
 */
@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

	Optional<Agency> findByName(String name);

	List<Agency> findByIsActive(Boolean isActive);

}

