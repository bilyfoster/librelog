package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CustomRole entity operations.
 */
@Repository
public interface CustomRoleRepository extends JpaRepository<CustomRole, UUID> {

	/**
	 * Find a custom role by name.
	 */
	Optional<CustomRole> findByName(String name);

	/**
	 * Check if a custom role with the given name exists.
	 */
	boolean existsByName(String name);

	/**
	 * Find all custom roles created by a specific user.
	 */
	List<CustomRole> findByCreatedByUserId(UUID createdByUserId);

}

