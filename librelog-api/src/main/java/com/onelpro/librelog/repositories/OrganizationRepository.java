package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Organization entity operations.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

	Optional<Organization> findByName(String name);

	boolean existsByName(String name);

}

